package com.example.agent.workflow;

import com.example.agent.core.GuardrailsService;
import com.example.agent.core.PromptManager;
import com.example.agent.memory.MemoryManager;
import com.example.agent.tool.McpKnowledgeClient;
import com.example.agent.tool.McpProductClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Agent Runtime — ReAct 循环引擎
 *
 * 替代 ShoppingAgentController 中的一次性调用。
 * 核心循环：THINK → ACT → OBSERVE → repeat until done
 *
 * 这是所有 Agent 框架（LangChain、LangGraph、Spring AI）的底层模式。
 * 手搓一遍，才能理解框架帮了什么。
 *
 * 流程：
 * 1. TaskPlanner 规划步骤（可选，复杂任务才规划）
 * 2. 进入 ReAct 循环：
 *    a. THINK：LLM 思考下一步该做什么
 *    b. ACT：调用工具 / 请求确认 / 直接回答
 *    c. OBSERVE：拿到结果，判断是否完成
 *    d. 未完成 → 回到 THINK
 * 3. 输出最终回复
 *
 * 教学点：Agent 不是一个 LLM 调用，而是一个可控的运行循环。
 */
@Component
public class AgentRuntime {

    private static final Logger log = LoggerFactory.getLogger(AgentRuntime.class);

    /** 最大循环次数，防止 Agent 无限循环 */
    private static final int MAX_ITERATIONS = 10;

    private final ChatModel chatModel;
    private final String reasoningModel;
    private final PromptManager promptManager;
    private final MemoryManager memoryManager;
    private final McpKnowledgeClient mcpKnowledgeClient;
    private final McpProductClient mcpProductClient;
    private final GuardrailsService guardrails;
    private final TaskPlanner taskPlanner;
    private final TraceService traceService;
    private final ErrorRecovery errorRecovery;
    private final ConfirmationService confirmationService;

    public AgentRuntime(ChatModel chatModel,
                        @Value("${agent.model.reasoning}") String reasoningModel,
                        PromptManager promptManager,
                        MemoryManager memoryManager,
                        McpKnowledgeClient mcpKnowledgeClient,
                        McpProductClient mcpProductClient,
                        GuardrailsService guardrails,
                        TaskPlanner taskPlanner,
                        TraceService traceService,
                        ErrorRecovery errorRecovery,
                        ConfirmationService confirmationService) {
        this.chatModel = chatModel;
        this.reasoningModel = reasoningModel;
        this.promptManager = promptManager;
        this.memoryManager = memoryManager;
        this.mcpKnowledgeClient = mcpKnowledgeClient;
        this.mcpProductClient = mcpProductClient;
        this.guardrails = guardrails;
        this.taskPlanner = taskPlanner;
        this.traceService = traceService;
        this.errorRecovery = errorRecovery;
        this.confirmationService = confirmationService;
    }

    /**
     * 执行工作流
     *
     * @param traceId   追踪 ID
     * @param sessionId 会话 ID
     * @param userId    用户 ID
     * @param message   用户消息
     * @return 执行结果
     */
    public WorkflowResult execute(String traceId, String sessionId, String userId, String message) {
        TraceService.TraceRecord trace = traceService.getTrace(traceId);
        if (trace == null) {
            return WorkflowResult.failed("Trace 不存在: " + traceId);
        }

        // 1. 构建 context（画像 + 记忆 + 对话历史）
        String context = memoryManager.buildContext(sessionId, userId, message);

        // 2. RAG 检索（始终注入，让 Agent 有商品知识）
        String ragContext = mcpKnowledgeClient.search(message);
        if (ragContext != null && !ragContext.isBlank()) {
            context = (context != null ? context : "") + "## 参考知识（从商品库检索）\n" + ragContext + "\n";
        }

        // 3. 判断是否需要规划
        List<WorkflowStep> plannedSteps = List.of();
        if (taskPlanner.needsPlanning(message)) {
            traceService.recordStateChange(traceId, WorkflowState.PLANNING, WorkflowState.PLANNING);
            plannedSteps = taskPlanner.plan(message, context);
        }

        // 4. 进入 ReAct 循环
        traceService.recordStateChange(traceId, WorkflowState.PLANNING, WorkflowState.EXECUTING);
        trace.setState(WorkflowState.EXECUTING);

        StringBuilder finalAnswer = new StringBuilder();
        StringBuilder conversationHistory = new StringBuilder();
        int iteration = 0;

        while (iteration < MAX_ITERATIONS) {
            iteration++;
            log.info("[AgentRuntime] === 循环第 {} 轮 ===", iteration);

            // 4a. THINK：LLM 思考下一步
            String thinkPrompt = buildThinkPrompt(message, context, plannedSteps, conversationHistory.toString(), iteration);
            String thinking = callLLM(thinkPrompt, reasoningModel);

            if (thinking == null || thinking.isBlank()) {
                log.warn("[AgentRuntime] THINK 返回空，结束循环");
                break;
            }

            log.info("[AgentRuntime] THINK: {}", thinking.length() > 200 ? thinking.substring(0, 200) + "..." : thinking);

            // 记录思考过程
            WorkflowStep currentStep = getCurrentStep(plannedSteps, iteration);
            if (currentStep != null) {
                currentStep.setLlmThought(thinking);
                traceService.recordThought(traceId, currentStep.getStepNumber(), thinking);
            }

            // 4b. 判断 LLM 是否要调用工具
            String toolCall = extractToolCall(thinking);

            if (toolCall != null) {
                // ACT：执行工具调用
                String toolResult = executeToolCall(traceId, currentStep, toolCall, context);

                if (toolResult == null) {
                    // 工具调用失败且重试耗尽，降级
                    String degradationMsg = "工具调用失败，基于已有信息回答。";
                    conversationHistory.append("系统: ").append(degradationMsg).append("\n");
                    // 让 LLM 直接给出最终答案
                    String finalPrompt = buildFinalAnswerPrompt(message, context, conversationHistory.toString());
                    String answer = callLLM(finalPrompt, reasoningModel);
                    if (answer != null && !answer.isBlank()) {
                        finalAnswer.append(answer);
                    }
                    break;
                }

                // 检查是否需要确认
                if (confirmationService.hasPendingConfirmation(traceId)) {
                    traceService.recordStateChange(traceId, WorkflowState.EXECUTING, WorkflowState.CONFIRMING);
                    trace.setState(WorkflowState.CONFIRMING);
                    String confirmMsg = confirmationService.getPendingConfirmation(traceId).getDescription();
                    return WorkflowResult.confirmationRequired(traceId, confirmMsg,
                            confirmationService.getPendingConfirmation(traceId));
                }

                // OBSERVE：记录工具结果
                conversationHistory.append("工具结果: ").append(toolResult).append("\n");
                if (currentStep != null) {
                    currentStep.complete(toolResult);
                    traceService.recordStepEnd(traceId, currentStep);
                }

                log.info("[AgentRuntime] OBSERVE: {}", toolResult.length() > 200 ? toolResult.substring(0, 200) + "..." : toolResult);

            } else {
                // 没有工具调用 → LLM 准备直接回答
                log.info("[AgentRuntime] LLM 决定直接回答，结束循环");
                finalAnswer.append(thinking);
                break;
            }
        }

        if (iteration >= MAX_ITERATIONS && finalAnswer.length() == 0) {
            // 达到最大循环次数，强制让 LLM 总结
            String finalPrompt = buildFinalAnswerPrompt(message, context, conversationHistory.toString());
            String answer = callLLM(finalPrompt, reasoningModel);
            if (answer != null) {
                finalAnswer.append(answer);
            }
        }

        // 5. 完成
        String answer = finalAnswer.toString().trim();
        if (answer.isEmpty()) {
            answer = "抱歉，处理过程中遇到了问题，请重新描述您的问题。";
        }

        // 脱敏
        answer = guardrails.sanitize(answer);

        traceService.completeTrace(traceId, WorkflowState.COMPLETED,
                "完成，共 " + iteration + " 轮循环");

        return WorkflowResult.success(traceId, answer, trace);
    }

    /**
     * 恢复执行（用户确认后）
     */
    public WorkflowResult resumeAfterConfirm(String traceId, boolean confirmed,
                                              String sessionId, String userId, String originalMessage) {
        ConfirmationService.PendingConfirmation pending = confirmationService.getPendingConfirmation(traceId);
        if (pending == null) {
            return WorkflowResult.failed("没有待确认的操作: " + traceId);
        }

        TraceService.TraceRecord trace = traceService.getTrace(traceId);

        if (!confirmed) {
            // 用户取消
            confirmationService.cancel(traceId);
            traceService.recordStateChange(traceId, WorkflowState.CONFIRMING, WorkflowState.CANCELLED);
            traceService.completeTrace(traceId, WorkflowState.CANCELLED, "用户取消操作");
            return WorkflowResult.success(traceId, "已取消操作。", trace);
        }

        // 用户确认，执行工具
        confirmationService.confirm(traceId);
        traceService.recordStateChange(traceId, WorkflowState.CONFIRMING, WorkflowState.EXECUTING);

        WorkflowStep step = new WorkflowStep(pending.getStepNumber(), pending.getDescription());
        step.setToolName(pending.getToolName());
        step.setToolInput(pending.getToolInput());
        step.start();
        traceService.recordStepStart(traceId, step);

        String toolResult = callToolDirectly(pending.getToolName(), pending.getToolInput());

        if (toolResult != null) {
            step.complete(toolResult);
            traceService.recordStepEnd(traceId, step);

            // 让 LLM 基于工具结果生成最终回复
            String context = memoryManager.buildContext(sessionId, userId, originalMessage);
            String finalPrompt = "用户请求: " + originalMessage + "\n\n" +
                    "工具 \"" + pending.getToolName() + "\" 执行结果:\n" + toolResult + "\n\n" +
                    "请基于以上结果，给用户一个简洁的回复。";

            String answer = callLLM(finalPrompt, reasoningModel);
            if (answer == null || answer.isBlank()) {
                answer = "操作已执行。工具返回: " + toolResult;
            }

            traceService.completeTrace(traceId, WorkflowState.COMPLETED, "确认后执行完成");
            return WorkflowResult.success(traceId, guardrails.sanitize(answer), trace);
        } else {
            step.fail("工具调用失败");
            traceService.recordStepEnd(traceId, step);
            traceService.completeTrace(traceId, WorkflowState.FAILED, "确认后工具调用失败");
            return WorkflowResult.failed("工具调用失败，请稍后重试。");
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 构建 THINK 提示词
     */
    private String buildThinkPrompt(String userMessage, String context,
                                      List<WorkflowStep> plannedSteps,
                                      String conversationHistory, int iteration) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个智能导购 Agent。请分析当前情况，决定下一步行动。\n\n");
        sb.append("## 用户请求\n").append(userMessage).append("\n\n");

        if (context != null && !context.isBlank()) {
            sb.append("## 参考信息\n").append(context).append("\n\n");
        }

        if (!plannedSteps.isEmpty()) {
            sb.append("## 执行计划\n");
            for (WorkflowStep step : plannedSteps) {
                String status = step.getStatus() == WorkflowStep.Status.SUCCESS ? "✅" :
                        step.getStatus() == WorkflowStep.Status.RUNNING ? "🔄" : "⬜";
                sb.append(status).append(" Step ").append(step.getStepNumber())
                        .append(": ").append(step.getAction()).append("\n");
            }
            sb.append("\n");
        }

        if (!conversationHistory.isBlank()) {
            sb.append("## 已执行的操作和结果\n").append(conversationHistory).append("\n\n");
        }

        sb.append("## 你的任务\n");
        sb.append("这是第 ").append(iteration).append(" 轮思考。\n\n");
        sb.append("请决定下一步行动：\n");
        sb.append("1. 如果需要查询信息，输出：TOOL_CALL:工具名(参数)\n");
        sb.append("   可用工具：search_knowledge(query), queryProductInfo(productId), place_order(productId, quantity)\n");
        sb.append("2. 如果已有足够信息，直接输出最终回复（简洁、有用）。\n\n");
        sb.append("注意：每次只调用一个工具。如果需要多个查询，分多轮进行。\n");

        return sb.toString();
    }

    /**
     * 构建最终回答提示词
     */
    private String buildFinalAnswerPrompt(String userMessage, String context, String conversationHistory) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个智能导购 Agent。请基于以下信息，给用户最终回复。\n\n");
        sb.append("## 用户请求\n").append(userMessage).append("\n\n");

        if (context != null && !context.isBlank()) {
            sb.append("## 参考信息\n").append(context).append("\n\n");
        }

        if (!conversationHistory.isBlank()) {
            sb.append("## 已查询的信息\n").append(conversationHistory).append("\n\n");
        }

        sb.append("请给出简洁、有用的最终回复。推荐商品时引用具体配置和价格。\n");

        return sb.toString();
    }

    /**
     * 从 LLM 输出中提取工具调用
     * 格式：TOOL_CALL:工具名(参数)
     */
    private String extractToolCall(String thinking) {
        if (thinking == null) return null;

        // 匹配 TOOL_CALL:xxx(yyy) 格式
        int toolCallIdx = thinking.indexOf("TOOL_CALL:");
        if (toolCallIdx >= 0) {
            String after = thinking.substring(toolCallIdx + 10).trim();
            // 找到第一个 ( 和匹配的 )
            int parenStart = after.indexOf("(");
            if (parenStart > 0) {
                int depth = 0;
                int parenEnd = -1;
                for (int i = parenStart; i < after.length(); i++) {
                    char c = after.charAt(i);
                    if (c == '(') depth++;
                    else if (c == ')') {
                        depth--;
                        if (depth == 0) { parenEnd = i; break; }
                    }
                }
                if (parenEnd > parenStart) {
                    return after.substring(0, parenEnd + 1);
                }
                // 没有匹配的括号，取整行
                int newline = after.indexOf('\n');
                return newline > 0 ? after.substring(0, newline).trim() : after.trim();
            }
        }

        // 也尝试匹配中文格式："调用工具 xxx"
        int callIdx = thinking.indexOf("调用工具");
        if (callIdx >= 0) {
            String after = thinking.substring(callIdx + 4).trim();
            int newline = after.indexOf('\n');
            return newline > 0 ? after.substring(0, newline).trim() : after;
        }

        return null;
    }

    /**
     * 执行工具调用（带重试和熔断）
     */
    private String executeToolCall(String traceId, WorkflowStep step, String toolCall, String context) {
        // 解析工具名和参数
        String toolName = parseToolName(toolCall);
        String toolInput = parseToolInput(toolCall);

        if (step != null) {
            step.setToolName(toolName);
            step.setToolInput(toolInput);
            step.start();
            traceService.recordStepStart(traceId, step);
        }

        traceService.recordToolCall(traceId,
                step != null ? step.getStepNumber() : 0, toolName, toolInput);

        // 检查熔断
        if (errorRecovery.isCircuitBroken(toolName)) {
            log.warn("[AgentRuntime] 工具 {} 已熔断，跳过", toolName);
            if (step != null) {
                step.skip("工具已熔断");
                traceService.recordStepEnd(traceId, step);
            }
            return errorRecovery.getDegradationMessage(toolName);
        }

        // 检查是否需要确认
        if (confirmationService.requiresConfirmation(toolName, toolInput)) {
            String confirmMsg = confirmationService.requestConfirmation(
                    traceId, step != null ? step.getStepNumber() : 0,
                    toolName, toolInput, "执行 " + toolName);
            if (step != null) {
                step.waitForConfirm();
            }
            traceService.recordConfirmRequest(traceId,
                    step != null ? step.getStepNumber() : 0, confirmMsg);
            return null; // 返回 null，上层检查 pendingConfirmation
        }

        // 带重试的工具调用
        while (true) {
            try {
                String result = callToolDirectly(toolName, toolInput);
                if (result != null) {
                    errorRecovery.recordSuccess(toolName);
                    return result;
                }
                // result 为 null 视为失败
                throw new RuntimeException("工具返回 null");
            } catch (Exception e) {
                log.warn("[AgentRuntime] 工具 {} 调用失败: {}", toolName, e.getMessage());

                if (step != null) {
                    step.incrementRetry();
                }
                errorRecovery.recordFailure(toolName, e.getMessage());
                traceService.recordRetry(traceId,
                        step != null ? step.getStepNumber() : 0,
                        step != null ? step.getRetryCount() : 0,
                        e.getMessage());

                if (step != null && errorRecovery.shouldRetry(step)) {
                    errorRecovery.waitBeforeRetry(step);
                    // 重试
                } else {
                    // 重试耗尽
                    if (step != null) {
                        step.fail("重试耗尽: " + e.getMessage());
                        traceService.recordStepEnd(traceId, step);
                    }
                    return null;
                }
            }
        }
    }

    /**
     * 直接调用工具（不带重试）
     *
     * 根据工具名路由到对应的 MCP Client 或本地模拟。
     * 注意：MCP Product Server 的 queryProductInfo 需要通过 MCP 协议调用，
     * 不是直接调 FunctionCallback（入参格式不同）。
     */
    private String callToolDirectly(String toolName, String toolInput) {
        // 知识检索
        if (toolName.contains("search_knowledge") || toolName.contains("知识")) {
            String query = extractQuery(toolInput);
            return mcpKnowledgeClient.search(query);
        }

        // 商品查询 — 通过 MCP Client 直接调用
        if (toolName.contains("queryProductInfo") || toolName.contains("商品") || toolName.contains("product")) {
            String productId = extractProductId(toolInput);
            if (mcpProductClient.isConnected()) {
                try {
                    // 通过 MCP 协议调用
                    io.modelcontextprotocol.spec.McpSchema.CallToolResult result =
                            mcpProductClient.callToolDirectly("queryProductInfo",
                                    java.util.Map.of("productId", productId));
                    if (result != null && result.content() != null) {
                        StringBuilder sb = new StringBuilder();
                        for (Object content : result.content()) {
                            if (content instanceof io.modelcontextprotocol.spec.McpSchema.TextContent tc) {
                                sb.append(tc.text());
                            }
                        }
                        return sb.toString();
                    }
                } catch (Exception e) {
                    log.warn("[AgentRuntime] MCP Product 调用失败: {}", e.getMessage());
                }
            }
            // fallback: 用知识检索
            return mcpKnowledgeClient.search(toolInput);
        }

        // 下单（模拟）
        if (toolName.contains("place_order") || toolName.contains("下单")) {
            return "{\"status\":\"success\",\"message\":\"订单已创建\",\"orderId\":\"ORD-" + System.currentTimeMillis() + "\"}";
        }

        // 默认尝试知识检索
        return mcpKnowledgeClient.search(toolInput);
    }

    /**
     * 从工具输入中提取商品 ID
     * 入参可能是 "NB001"、"productId: NB001"、"{productId: NB001}" 等格式
     */
    private String extractProductId(String toolInput) {
        if (toolInput == null) return "";
        String cleaned = toolInput.replaceAll("[\"{}()]", "").trim();
        if (cleaned.contains("productId:")) {
            cleaned = cleaned.substring(cleaned.indexOf("productId:") + 10).trim();
        }
        // 取第一个单词
        String[] parts = cleaned.split("[,;\\s]+");
        return parts.length > 0 ? parts[0].trim() : cleaned;
    }

    /**
     * 调用 LLM
     */
    private String callLLM(String prompt, String model) {
        try {
            return ChatClient.create(chatModel)
                    .prompt()
                    .user(prompt)
                    .options(OpenAiChatOptions.builder()
                            .withModel(model)
                            .withTemperature(0.3)
                            .build())
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("[AgentRuntime] LLM 调用失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前应该执行的步骤
     */
    private WorkflowStep getCurrentStep(List<WorkflowStep> plannedSteps, int iteration) {
        if (plannedSteps.isEmpty()) {
            return new WorkflowStep(iteration, "第 " + iteration + " 轮操作");
        }
        // 返回第一个未完成的步骤
        for (WorkflowStep step : plannedSteps) {
            if (step.getStatus() == WorkflowStep.Status.PENDING ||
                step.getStatus() == WorkflowStep.Status.RUNNING) {
                return step;
            }
        }
        return plannedSteps.get(plannedSteps.size() - 1);
    }

    /**
     * 从工具调用字符串中解析工具名
     */
    private String parseToolName(String toolCall) {
        if (toolCall == null) return "unknown";

        // TOOL_CALL:queryProductInfo(NB001) → queryProductInfo
        int parenIdx = toolCall.indexOf("(");
        if (parenIdx > 0) {
            String before = toolCall.substring(0, parenIdx).trim();
            // 取最后一个空格后的内容
            int spaceIdx = before.lastIndexOf(' ');
            return spaceIdx >= 0 ? before.substring(spaceIdx + 1) : before;
        }

        // 没有括号，取第一个空格后的第一个词
        String[] parts = toolCall.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : toolCall;
    }

    /**
     * 从工具调用字符串中解析参数
     */
    private String parseToolInput(String toolCall) {
        if (toolCall == null) return "";

        // TOOL_CALL:queryProductInfo(NB001) → NB001
        int parenStart = toolCall.indexOf("(");
        int parenEnd = toolCall.lastIndexOf(")");
        if (parenStart >= 0 && parenEnd > parenStart) {
            return toolCall.substring(parenStart + 1, parenEnd).trim();
        }

        // 没有括号，取工具名后面的部分
        String[] parts = toolCall.trim().split("\\s+", 2);
        return parts.length > 1 ? parts[1] : toolCall;
    }

    /**
     * 从工具输入中提取查询文本
     */
    private String extractQuery(String toolInput) {
        if (toolInput == null) return "";
        // 去掉可能的 JSON 格式
        String cleaned = toolInput.replaceAll("[\"{}]", "").trim();
        // 提取 query 字段
        if (cleaned.contains("query:")) {
            int idx = cleaned.indexOf("query:");
            return cleaned.substring(idx + 6).trim();
        }
        return cleaned;
    }

    /**
     * 工作流执行结果
     */
    public static class WorkflowResult {
        private final boolean success;
        private final boolean needsConfirmation;
        private final String traceId;
        private final String message;
        private final TraceService.TraceRecord trace;
        private final ConfirmationService.PendingConfirmation pendingConfirmation;

        private WorkflowResult(boolean success, boolean needsConfirmation, String traceId,
                               String message, TraceService.TraceRecord trace,
                               ConfirmationService.PendingConfirmation pendingConfirmation) {
            this.success = success;
            this.needsConfirmation = needsConfirmation;
            this.traceId = traceId;
            this.message = message;
            this.trace = trace;
            this.pendingConfirmation = pendingConfirmation;
        }

        public static WorkflowResult success(String traceId, String message, TraceService.TraceRecord trace) {
            return new WorkflowResult(true, false, traceId, message, trace, null);
        }

        public static WorkflowResult failed(String message) {
            return new WorkflowResult(false, false, null, message, null, null);
        }

        public static WorkflowResult confirmationRequired(String traceId, String message,
                                                           ConfirmationService.PendingConfirmation pending) {
            return new WorkflowResult(false, true, traceId, message, null, pending);
        }

        public boolean isSuccess() { return success; }
        public boolean isNeedsConfirmation() { return needsConfirmation; }
        public String getTraceId() { return traceId; }
        public String getMessage() { return message; }
        public TraceService.TraceRecord getTrace() { return trace; }
        public ConfirmationService.PendingConfirmation getPendingConfirmation() { return pendingConfirmation; }
    }
}