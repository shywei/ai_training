package com.example.agent.core;

import com.example.agent.memory.MemoryManager;
import com.example.agent.memory.UserProfileExtractor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 导购 Agent API
 *
 * v1.0-prompt: Streaming、模型路由、结构化输出、Self-Correction、Guardrails
 * v2.0-memory: 新增 Session Memory、对话历史注入、context 填充
 */
@RestController
@RequestMapping("/api/agent")
public class ShoppingAgentController {

    private final ChatModel chatModel;
    private final ModelRouter modelRouter;
    private final PromptManager promptManager;
    private final StructuredOutputHandler outputHandler;
    private final GuardrailsService guardrails;
    private final MemoryManager memoryManager;
    private final UserProfileExtractor profileExtractor;
    private final LlmInteractionLogger llmLogger;

    public ShoppingAgentController(ChatModel chatModel,
                                   ModelRouter modelRouter,
                                   PromptManager promptManager,
                                   StructuredOutputHandler outputHandler,
                                   GuardrailsService guardrails,
                                   MemoryManager memoryManager,
                                   UserProfileExtractor profileExtractor,
                                   LlmInteractionLogger llmLogger) {
        this.chatModel = chatModel;
        this.modelRouter = modelRouter;
        this.promptManager = promptManager;
        this.outputHandler = outputHandler;
        this.guardrails = guardrails;
        this.memoryManager = memoryManager;
        this.profileExtractor = profileExtractor;
        this.llmLogger = llmLogger;
    }

    /**
     * 流式对话接口(v2.0: 支持 Session Memory)
     * GET /api/agent/chat?message=xxx&sessionId=xxx
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam String message,
                             @RequestParam(defaultValue = "default") String sessionId,
                             @RequestParam(defaultValue = "") String userId) {
        if (guardrails.checkInjection(message)) {
            return Flux.just("检测到不安全的输入,请重新提问。");
        }

        String model = modelRouter.selectModel(message);

        // v3.0: 从 Memory + Knowledge 构建 context(对话历史 + 用户画像 + RAG)
        String context = memoryManager.buildContext(sessionId, userId, message);
        llmLogger.logMemoryInjection(sessionId, context);

        String systemPrompt = promptManager.buildSystemPrompt();
        String userPrompt = promptManager.buildUserPrompt(message, context);
        llmLogger.logRequest(model, systemPrompt, userPrompt);

        String finalModel = model;
        String finalSessionId = sessionId;
        String finalMessage = message;
        String finalUserId = userId;

        return ChatClient.create(chatModel)
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(OpenAiChatOptions.builder()
                        .withModel(model)
                        .build())
                .stream()
                .content()
                .transform(flux -> {
                    String modelTag = "[MODEL]" + finalModel;
                    StringBuilder fullResp = new StringBuilder();
                    long[] start = {System.currentTimeMillis()};
                    return Flux.concat(
                        Flux.just(modelTag),
                        flux.map(chunk -> {
                            fullResp.append(chunk);
                            return guardrails.sanitize(chunk);
                        }).doOnComplete(() -> {
                            llmLogger.logResponse(finalModel, fullResp.toString(), System.currentTimeMillis() - start[0]);
                            // 流结束后存入 Redis，达到阈值自动沉淀到 pgvector
                            memoryManager.saveTurn(finalSessionId, finalUserId, finalMessage, fullResp.toString());
                            // 检测“记住”关键词，触发画像抽取
                            if (!finalUserId.isBlank() && containsRememberKeyword(finalMessage)) {
                                System.out.println("[Memory] 检测到 记住 指令，触发画像抽取");
                                profileExtractor.extract(finalUserId, finalSessionId);
                            }
                        })
                    );
                });
    }

    /**
     * v2.0: 保存对话到 Memory
     * POST /api/agent/memory/save
     */
    @PostMapping("/memory/save")
    public String saveToMemory(@RequestParam String sessionId,
                               @RequestParam String message,
                               @RequestParam String reply,
                               @RequestParam(defaultValue = "") String userId) {
        memoryManager.saveTurn(sessionId, userId, message, reply);
        long count = memoryManager.getSessionMemory().getMessageCount(sessionId);

        // 触发画像抽取:用户主动说"记住"时
        if (!userId.isBlank() && containsRememberKeyword(message)) {
            System.out.println("[Memory] 检测到 记住 指令，触发画像抽取");
            profileExtractor.extract(userId, sessionId);
        }

        return "{\"status\":\"ok\",\"sessionId\":\"" + sessionId + "\",\"messageCount\":" + count + "}";
    }

    /**
     * 检测用户是否说了"记住"相关关键词
     */
    private boolean containsRememberKeyword(String message) {
        if (message == null) return false;
        String[] keywords = {"记住", "记一下", "记着", "请记", "帮我记"};
        for (String kw : keywords) {
            if (message.contains(kw)) return true;
        }
        return false;
    }

    /**
     * v2.0: 清空 Session
     * POST /api/agent/memory/clear?sessionId=xxx
     */
    @PostMapping("/memory/clear")
    public String clearMemory(@RequestParam String sessionId,
                              @RequestParam(defaultValue = "") String userId) {
        // 会话结束时触发画像抽取
        if (!userId.isBlank()) {
            long count = memoryManager.getSessionMemory().getMessageCount(sessionId);
            if (count > 0) {
                System.out.println("[Memory] 会话结束,触发画像抽取(" + count + "条消息)");
                profileExtractor.extract(userId, sessionId);
            }
        }
        // v3.0: clearSession 传入 userId，沉淀对话记忆到 pgvector
        memoryManager.clearSession(sessionId, userId);
        return "{\"status\":\"ok\",\"sessionId\":\"" + sessionId + "\",\"message\":\"Session 已清空\"}";
    }

    /**
     * 结构化输出接口(保持不变)
     */
    @PostMapping("/recommend")
    public String recommend(@RequestBody String userNeed) {
        if (guardrails.checkInjection(userNeed)) {
            return "{\"error\":\"检测到不安全的输入\"}";
        }

        String model = modelRouter.selectModel(userNeed);
        String systemPrompt = promptManager.buildSystemPrompt() + """

                ## 输出要求
                直接输出 JSON,不要用代码块包裹,不要添加任何解释文字。
                格式:{"recommendation":"推荐理由","products":[{"name":"商品名","reason":"理由","price_range":"价格"}],"confidence":0.9}
                """;
        String userPrompt = promptManager.buildUserPrompt(userNeed, null);
        llmLogger.logRequest(model, systemPrompt, userPrompt);

        long startTime = System.currentTimeMillis();
        String rawOutput = callLLM(systemPrompt, userPrompt, model);
        llmLogger.logResponse(model, rawOutput, System.currentTimeMillis() - startTime);
        System.out.println("\n========== 结构化输出演示 ==========");
        System.out.println("【步骤1】LLM 原始输出:");
        System.out.println(rawOutput);

        boolean hasSensitive = guardrails.containsSensitiveInfo(rawOutput);
        String extracted = outputHandler.extractJson(rawOutput);
        System.out.println("\n【步骤2】提取出的 JSON:");
        System.out.println(extracted);

        var json = outputHandler.validate(rawOutput);
        System.out.println("\n【步骤3】Schema 验证: " + (json != null ? "通过" : "失败"));

        if (json != null) {
            try {
                RecommendResult result = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(extracted, RecommendResult.class);
                System.out.println("\n【步骤4】Bean Mapping: JSON → RecommendResult 对象");
                System.out.println("  推荐理由: " + result.getRecommendation());
                System.out.println("  置信度: " + result.getConfidence());
                for (RecommendResult.Product p : result.getProducts()) {
                    System.out.println("  商品: " + p);
                }
                System.out.println("\n【步骤5】Java 代码可以对这些对象做任何事:");
                System.out.println("  · 存入数据库: productRepository.save(result.getProducts())");
                System.out.println("  · 传给订单系统: orderService.createOrder(result.getProducts())");
                System.out.println("  · 渲染商品卡片: return productCardView(result)");
                System.out.println("\n【结果】返回结构化 JSON 给前端");
                System.out.println("====================================\n");
            } catch (Exception e) {
                System.out.println("  Bean Mapping 失败: " + e.getMessage());
            }
            String response = extracted;
            if (hasSensitive) {
                response = response.replace("\"recommendation\"", "\"_warning\":\"输出含敏感信息已脱敏\",\"recommendation\"");
            }
            return response;
        }

        System.out.println("【步骤4】Self-Correction: 第一次输出非法,重试...");
        String correctionPrompt = outputHandler.buildCorrectionPrompt(rawOutput, "输出不是合法 JSON");
        String corrected = callLLM(systemPrompt, correctionPrompt, model);
        System.out.println("【步骤4】重试输出:");
        System.out.println(corrected);

        var json2 = outputHandler.validate(corrected);
        System.out.println("【步骤4】验证: " + (json2 != null ? "通过" : "失败"));
        if (json2 != null) {
            System.out.println("【结果】Self-Correction 成功,返回结构化 JSON");
            System.out.println("====================================\n");
            return json2.toString();
        }

        System.out.println("【结果】两次都失败,返回错误");
        System.out.println("====================================\n");
        return "{\"error\":\"模型输出无法解析\",\"rawOutput\":\"" + rawOutput.replace("\"", "'").replaceAll("\\s+", " ").substring(0, Math.min(rawOutput.length(), 150)) + "\"}";
    }

    private String callLLM(String systemPrompt, String userPrompt, String model) {
        return ChatClient.create(chatModel)
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(OpenAiChatOptions.builder()
                        .withModel(model)
                        .withTemperature(0.2)
                        .build())
                .call()
                .content();
    }
}
