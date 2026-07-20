package com.example.agent.core;

import com.example.agent.memory.MemoryManager;
import com.example.agent.memory.UserProfileExtractor;
import com.example.agent.tool.LlmRouter;
import com.example.agent.tool.McpKnowledgeClient;
import com.example.agent.tool.McpProductClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.openai.OpenAiChatOptions;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 导购 Agent API
 *
 * v1.0-prompt: Streaming、模型路由、结构化输出、Self-Correction、Guardrails
 * v2.0-memory: 新增 Session Memory、对话历史注入、context 填充
 * v3.0-knowledge: 新增 RAG 知识检索
 * v4.0-tool: 新增 LLM 路由、Tool 调用、数据分层闭环
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
    private final LlmRouter llmRouter;
    private final McpProductClient mcpProductClient;
    private final McpKnowledgeClient mcpKnowledgeClient;

    public ShoppingAgentController(ChatModel chatModel,
                                   ModelRouter modelRouter,
                                   PromptManager promptManager,
                                   StructuredOutputHandler outputHandler,
                                   GuardrailsService guardrails,
                                   MemoryManager memoryManager,
                                   UserProfileExtractor profileExtractor,
                                   LlmInteractionLogger llmLogger,
                                   LlmRouter llmRouter,
                                   McpProductClient mcpProductClient,
                                   McpKnowledgeClient mcpKnowledgeClient) {
        this.chatModel = chatModel;
        this.modelRouter = modelRouter;
        this.promptManager = promptManager;
        this.outputHandler = outputHandler;
        this.guardrails = guardrails;
        this.memoryManager = memoryManager;
        this.profileExtractor = profileExtractor;
        this.llmLogger = llmLogger;
        this.llmRouter = llmRouter;
        this.mcpProductClient = mcpProductClient;
        this.mcpKnowledgeClient = mcpKnowledgeClient;
    }

    /**
     * 流式对话接口
     * v4.0: LLM 路由 → 知识检索 / Tool 调用 / 普通对话
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam String message,
                             @RequestParam(defaultValue = "default") String sessionId,
                             @RequestParam(defaultValue = "") String userId) {
        if (guardrails.checkInjection(message)) {
            return Flux.just("检测到不安全的输入,请重新提问。");
        }

        // v4.0: LLM 路由判断意图
        LlmRouter.Route route = llmRouter.route(message);
        System.out.println("[Router] 用户意图: " + route + " (消息: " + message + ")");

        String model = modelRouter.selectModel(message);

        // 构建 context（画像 + 记忆 + 对话历史）
        String context = memoryManager.buildContext(sessionId, userId, message);

        // 根据路由决策，决定是否添加 RAG 知识
        // v4.0: RAG 检索通过 MCP 协议调用 Knowledge MCP Server
        if (route == LlmRouter.Route.KNOWLEDGE) {
            String ragContext = mcpKnowledgeClient.search(message);
            if (ragContext != null && !ragContext.isBlank()) {
                context = (context != null ? context : "") + "## 参考知识（从商品库检索）\n" + ragContext + "\n";
            }
        }

        llmLogger.logMemoryInjection(sessionId, context);

        String systemPrompt = promptManager.buildSystemPrompt();
        String userPrompt = promptManager.buildUserPrompt(message, context);
        llmLogger.logRequest(model, systemPrompt, userPrompt);

        String finalModel = model;
        String finalSessionId = sessionId;
        String finalMessage = message;
        String finalUserId = userId;

        // 构建 ChatClient，TOOL 路由时注册 FunctionCallback
        var clientBuilder = ChatClient.create(chatModel).prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(OpenAiChatOptions.builder()
                        .withModel(model)
                        .build());

        // 如果是 TOOL 意图，注册 MCP Product Tool
        // 所有商品查询 Tool 统一通过 MCP 协议从 MCP Server 获取
        if (route == LlmRouter.Route.TOOL) {
            List<FunctionCallback> mcpCallbacks = mcpProductClient.getFunctionCallbacks();
            if (!mcpCallbacks.isEmpty()) {
                clientBuilder = clientBuilder.functions(mcpCallbacks.toArray(new FunctionCallback[0]));
                System.out.println("[Tool] 已注册 " + mcpCallbacks.size() + " 个 Product MCP Tool: " + mcpProductClient.getToolNames());
            } else {
                System.out.println("[Tool] Product MCP Server 未连接，无可用 Tool");
            }
        }

        // save_memory 跨路由可用（用户在任何对话中都可能表达偏好）
        List<FunctionCallback> knowledgeCallbacks = mcpKnowledgeClient.getFunctionCallbacks();
        if (!knowledgeCallbacks.isEmpty()) {
            clientBuilder = clientBuilder.functions(knowledgeCallbacks.toArray(new FunctionCallback[0]));
            System.out.println("[Tool] 已注册 " + knowledgeCallbacks.size() + " 个 Knowledge MCP Tool: " + mcpKnowledgeClient.getToolNames());
        }

        String finalRoute = route.name();

        return clientBuilder
                .stream()
                .content()
                .transform(flux -> {
                    String modelTag = "[MODEL]" + finalModel;
                    String routeTag = "[ROUTE]" + finalRoute;
                    StringBuilder fullResp = new StringBuilder();
                    long[] start = {System.currentTimeMillis()};
                    return Flux.concat(
                        Flux.just(modelTag),
                        Flux.just(routeTag),
                        flux.map(chunk -> {
                            fullResp.append(chunk);
                            return guardrails.sanitize(chunk);
                        }).doOnComplete(() -> {
                            llmLogger.logResponse(finalModel, fullResp.toString(), System.currentTimeMillis() - start[0]);
                            memoryManager.saveTurn(finalSessionId, finalUserId, finalMessage, fullResp.toString());
                            if (!finalUserId.isBlank() && containsRememberKeyword(finalMessage)) {
                                System.out.println("[Memory] 检测到 记住 指令，触发画像抽取");
                                profileExtractor.extract(finalUserId, finalSessionId);
                            }
                        })
                    );
                });
    }

    /**
     * 索引商品知识库（通过 MCP 协议调用 Knowledge Server）
     * POST /api/agent/knowledge/index?force=false
     */
    @PostMapping("/knowledge/index")
    public Map<String, Object> indexKnowledge(@RequestParam(defaultValue = "false") boolean force) {
        String result = mcpKnowledgeClient.indexKnowledge(force);
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        if (result != null && result.startsWith("索引完成")) {
            response.put("status", "success");
            response.put("message", result);
        } else if (result != null && result.contains("跳过索引")) {
            response.put("status", "skipped");
            response.put("message", result);
        } else {
            response.put("status", "error");
            response.put("message", result != null ? result : "索引失败");
        }
        return response;
    }

    /**
     * 保存对话到 Memory
     */
    @PostMapping("/memory/save")
    public String saveToMemory(@RequestParam String sessionId,
                               @RequestParam String message,
                               @RequestParam String reply,
                               @RequestParam(defaultValue = "") String userId) {
        memoryManager.saveTurn(sessionId, userId, message, reply);
        long count = memoryManager.getSessionMemory().getMessageCount(sessionId);

        if (!userId.isBlank() && containsRememberKeyword(message)) {
            System.out.println("[Memory] 检测到 记住 指令，触发画像抽取");
            profileExtractor.extract(userId, sessionId);
        }

        return "{\"status\":\"ok\",\"sessionId\":\"" + sessionId + "\",\"messageCount\":" + count + "}";
    }

    private boolean containsRememberKeyword(String message) {
        if (message == null) return false;
        String[] keywords = {"记住", "记一下", "记着", "请记", "帮我记"};
        for (String kw : keywords) {
            if (message.contains(kw)) return true;
        }
        return false;
    }

    /**
     * 清空 Session
     */
    @PostMapping("/memory/clear")
    public String clearMemory(@RequestParam String sessionId,
                              @RequestParam(defaultValue = "") String userId) {
        if (!userId.isBlank()) {
            long count = memoryManager.getSessionMemory().getMessageCount(sessionId);
            if (count > 0) {
                System.out.println("[Memory] 会话结束,触发画像抽取(" + count + "条消息)");
                profileExtractor.extract(userId, sessionId);
            }
        }
        memoryManager.clearSession(sessionId, userId);
        return "{\"status\":\"ok\",\"sessionId\":\"" + sessionId + "\",\"message\":\"Session 已清空\"}";
    }

    /**
     * 结构化输出接口
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

        boolean hasSensitive = guardrails.containsSensitiveInfo(rawOutput);
        String extracted = outputHandler.extractJson(rawOutput);
        var json = outputHandler.validate(rawOutput);

        if (json != null) {
            String response = extracted;
            if (hasSensitive) {
                response = response.replace("\"recommendation\"", "\"_warning\":\"输出含敏感信息已脱敏\",\"recommendation\"");
            }
            return response;
        }

        String correctionPrompt = outputHandler.buildCorrectionPrompt(rawOutput, "输出不是合法 JSON");
        String corrected = callLLM(systemPrompt, correctionPrompt, model);
        var json2 = outputHandler.validate(corrected);
        if (json2 != null) {
            return json2.toString();
        }

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
