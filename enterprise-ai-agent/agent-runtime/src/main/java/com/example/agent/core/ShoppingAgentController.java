package com.example.agent.core;

import com.example.agent.memory.MemoryManager;
import com.example.agent.memory.UserProfileExtractor;
import com.example.agent.tool.LlmRouter;
import com.example.agent.tool.McpKnowledgeClient;
import com.example.agent.tool.McpProductClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/**
 * 导购 Agent API
 *
 * v1.0-prompt: Streaming、模型路由、结构化输出、Self-Correction、Guardrails
 * v2.0-memory: 新增 Session Memory、对话历史注入、context 填充
 * v3.0-knowledge: 新增 RAG 知识检索
 * v4.0-tool: 新增 LLM 路由、Tool 调用、数据分层闭环
 *
 * 注意：/chat 端点已删除。
 * 前端统一调用 /api/agent/workflow 或 /api/agent/workflow/stream。
 */
@RestController
@RequestMapping("/api/agent")
public class ShoppingAgentController {

    private final ChatModel chatModel;
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

    /**
     * 构建 RAG 搜索 query，包含最近对话上下文
     *
     * 解决“多少钱”这种短 query 缺少产品上下文的问题。
     * 把最近的用户消息拼入 query，让向量检索能找到相关商品。
     */
    private String buildSearchQuery(String sessionId, String currentMessage) {
        List<String> messages = memoryManager.getSessionMemory().getMessages(sessionId);
        if (messages == null || messages.isEmpty()) {
            return currentMessage;
        }

        // 取最近 4 条用户消息，提取关键信息拼入 query
        StringBuilder context = new StringBuilder();
        int userCount = 0;
        for (int i = messages.size() - 1; i >= 0 && userCount < 4; i--) {
            String msg = messages.get(i);
            if (msg.startsWith("user|")) {
                String content = msg.substring(5).replace("\\n", " ");
                context.insert(0, content + " ");
                userCount++;
            }
        }
        context.append(currentMessage);

        String query = context.toString().trim();
        // 限制 query 长度，避免过长
        if (query.length() > 200) {
            query = query.substring(query.length() - 200);
        }
        return query;
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

        String model = llmRouter.route(userNeed).model();
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
