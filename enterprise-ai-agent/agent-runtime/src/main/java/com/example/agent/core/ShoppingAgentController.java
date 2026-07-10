package com.example.agent.core;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 导购 Agent API
 *
 * v1.0-prompt 验证目标：
 * - [x] Streaming 输出
 * - [x] 模型路由（Fast vs Reasoning）
 * - [x] 结构化输出（JSON Schema）
 * - [x] Self-Correction（输出非法时自动重试）
 * - [x] Guardrails（注入防御 + 敏感信息过滤）
 */
@RestController
@RequestMapping("/api/agent")
public class ShoppingAgentController {

    private final ChatModel chatModel;
    private final ModelRouter modelRouter;
    private final PromptManager promptManager;
    private final StructuredOutputHandler outputHandler;
    private final GuardrailsService guardrails;

    public ShoppingAgentController(ChatModel chatModel,
                                   ModelRouter modelRouter,
                                   PromptManager promptManager,
                                   StructuredOutputHandler outputHandler,
                                   GuardrailsService guardrails) {
        this.chatModel = chatModel;
        this.modelRouter = modelRouter;
        this.promptManager = promptManager;
        this.outputHandler = outputHandler;
        this.guardrails = guardrails;
    }

    /**
     * 流式对话接口
     * GET /api/agent/chat?message=帮我推荐一台笔记本
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam String message) {
        // Guardrails: 检查 Prompt Injection
        if (guardrails.checkInjection(message)) {
            return Flux.just("检测到不安全的输入，请重新提问。");
        }

        // 模型路由：选择用哪个模型
        String model = modelRouter.selectModel(message);

        // 构建 Prompt
        String systemPrompt = promptManager.buildSystemPrompt();
        String userPrompt = promptManager.buildUserPrompt(message, null);

        // 调用 LLM（Streaming）
        String finalModel = model;
        return ChatClient.create(chatModel)
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(OpenAiChatOptions.builder()
                        .withModel(model)
                        .build())
                .stream()
                .content()
                // 在第一个 chunk 前插入模型名标记
                .transform(flux -> {
                    String modelTag = "[MODEL]" + finalModel;
                    return Flux.concat(
                        Flux.just(modelTag),
                        flux.map(chunk -> guardrails.sanitize(chunk))
                    );
                });
    }

    /**
     * 结构化输出接口（带 Self-Correction）
     * POST /api/agent/recommend
     * 返回符合 JSON Schema 的商品推荐
     */
    @PostMapping("/recommend")
    public String recommend(@RequestBody String userNeed) {
        if (guardrails.checkInjection(userNeed)) {
            return "{\"error\":\"检测到不安全的输入\"}";
        }

        String model = modelRouter.selectModel(userNeed);
        String systemPrompt = promptManager.buildSystemPrompt() + """

                ## 输出要求
                直接输出 JSON，不要用代码块包裹，不要添加任何解释文字。
                格式：{"recommendation":"推荐理由","products":[{"name":"商品名","reason":"理由","price_range":"价格"}],"confidence":0.9}
                """;
        String userPrompt = promptManager.buildUserPrompt(userNeed, null);

        String rawOutput = callLLM(systemPrompt, userPrompt, model);
        System.out.println("\n========== 结构化输出演示 ==========");
        System.out.println("【步骤1】LLM 原始输出:");
        System.out.println(rawOutput);

        boolean hasSensitive = guardrails.containsSensitiveInfo(rawOutput);

        // 提取 JSON
        String extracted = outputHandler.extractJson(rawOutput);
        System.out.println("\n【步骤2】提取出的 JSON:");
        System.out.println(extracted);

        // 验证
        var json = outputHandler.validate(rawOutput);
        System.out.println("\n【步骤3】Schema 验证: " + (json != null ? "通过" : "失败"));

        if (json != null) {
            // Bean Mapping: JSON → Java 对象
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

        // Self-Correction
        System.out.println("【步骤4】Self-Correction: 第一次输出非法，重试...");
        String correctionPrompt = outputHandler.buildCorrectionPrompt(rawOutput, "输出不是合法 JSON");
        String corrected = callLLM(systemPrompt, correctionPrompt, model);
        System.out.println("【步骤4】重试输出:");
        System.out.println(corrected);

        var json2 = outputHandler.validate(corrected);
        System.out.println("【步骤4】验证: " + (json2 != null ? "通过" : "失败"));
        if (json2 != null) {
            System.out.println("【结果】Self-Correction 成功，返回结构化 JSON");
            System.out.println("====================================\n");
            return json2.toString();
        }

        System.out.println("【结果】两次都失败，返回错误");
        System.out.println("====================================\n");
        return "{\"error\":\"模型输出无法解析\",\"rawOutput\":\"" + rawOutput.replace("\"", "'").replaceAll("\\s+", " ").substring(0, Math.min(rawOutput.length(), 150)) + "\"}";
    }

    /**
     * 非流式调用 LLM
     */
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
