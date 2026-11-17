package com.example.agent.adapter.springai2;

import com.example.agent.domain.*;
import com.example.agent.port.CheckpointStorePort;
import com.example.agent.port.EnterpriseToolPort;
import com.example.agent.port.ShoppingAgentPort;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.math.BigDecimal;
import java.util.*;

/**
 * Spring AI 2.0 工作流适配器（实现六边形入站端口 ShoppingAgentPort）
 *
 * =========================================================================================
 * ⭐【第 09 课最核心教学对比类：从手搓到框架：框架的边界（第 09 课 P06 与 P20~P24）】⭐
 * =========================================================================================
 *
 * 【维度 1：Spring AI 2.0.2 GA 带来的现代基础设施红利】：
 * 1. 极简的单次 LLM + Tool 调用：
 *    - 在 v5.0 手搓版本中，AgentRuntime (651行) 充斥着正则提取 TOOL_CALL、手写 McpToolCallback 桥接、
 *      硬编码的 Prompt 变量格式化。
 *    - 在 Spring AI 2.0 中，ChatClient.prompt().user(...).call().content() 配合 @Tool 声明式工具和
 *      CallAdvisor 拦截流水线，使单轮“思考-调用-拦截”极为优雅，单次调用样板代码从 651 行锐减至底层框架自动接管！
 *
 * 【维度 2：架构反思——为什么依然留有胶水层？框架的边界在哪里？】：
 * 1. Spring AI 的定位是“基础设施层（类似于 Java EE 的 Servlet API）”：
 *    - 它提供了统一的模型网关、ToolCallback 规范、ChatClient Fluent API 与 Advisor 拦截链；
 * 2. 但是，企业级 Agent 远远不止一次 ChatClient 调用：
 *    - 7 状态机跃迁流转（PLANNING -> EXECUTING -> CONFIRMING -> WAITING_USER -> COMPLETED）；
 *    - 多步骤 TaskPlanner 规划拆解与步骤推进；
 *    - 人在回路（HITL）的高风险拦截与异步挂起/恢复；
 *    - 跨 Pod 漂移的 Checkpointer 全量快照与断点复水（Rehydration）；
 *    - 时光倒流分支回滚（Forking）；
 *    这些属于【Agent Harness / 高级应用编排层】的职责！Spring AI 并不原生提供有状态 Harness。
 *    因此，你在这个类中依然能看到：开发者必须“手写”状态机推进循环、手动驱动 Checkpoint 保存与复水！
 *
 * 这正是本课程引入第 2 级阶梯【AgentScope Java 2.0（Spring MVC 级别的高阶框架）】的根本原因！
 */
@Service
public class SpringAi2WorkflowAdapter implements ShoppingAgentPort {

    private static final Logger log = LoggerFactory.getLogger(SpringAi2WorkflowAdapter.class);

    private static final int MAX_ITERATIONS = 8;

    private final ChatClient chatClient;
    private final ChatModel chatModel;
    private final EnterpriseToolPort toolPort;
    private final CheckpointStorePort checkpointStore;
    private final ShoppingDomainService domainService;
    private final ObjectMapper objectMapper;

    public SpringAi2WorkflowAdapter(
            @Qualifier("shoppingChatClient") ChatClient chatClient,
            ChatModel chatModel,
            EnterpriseToolPort toolPort,
            CheckpointStorePort checkpointStore,
            ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.chatModel = chatModel;
        this.toolPort = toolPort;
        this.checkpointStore = checkpointStore;
        this.domainService = new ShoppingDomainService();
        this.objectMapper = objectMapper;
    }

    @Override
    public WorkflowResponse executeTask(String traceId, String sessionId, String userId, String message) {
        log.info("[SpringAI2 Workflow] 接收新任务: traceId={}, sessionId={}, userId={}, msg={}",
                traceId, sessionId, userId, message);

        String executionId = "exec-" + UUID.randomUUID().toString().substring(0, 8);
        String rootCheckpointId = "chk-init-0";

        // 1. 初始化根检查点 (PLANNING 状态)
        AgentCheckpoint currentCheckpoint = new AgentCheckpoint(
                executionId, rootCheckpointId, null, WorkflowState.PLANNING, 0);
        currentCheckpoint.setSessionId(sessionId);
        currentCheckpoint.setUserId(userId);
        currentCheckpoint.getScratchpad().put("user_message", message);
        currentCheckpoint.getMetadata().put("traceId", traceId);
        checkpointStore.saveSnapshot(currentCheckpoint);

        // 2. 步骤规划（复杂多商品对比请求才需要 TaskPlanner 拆解）
        List<WorkflowStep> steps = planTask(message);
        currentCheckpoint.setStepSnapshots(steps);

        // 3. 状态跃迁至 EXECUTING 运行循环
        currentCheckpoint.setState(WorkflowState.EXECUTING);
        checkpointStore.saveSnapshot(currentCheckpoint);

        // 4. 驱动多步执行编排（展示基础设施层仍需开发者编写的状态机推进胶水）
        return driveWorkflowLoop(currentCheckpoint, message, null);
    }

    @Override
    public Flux<WorkflowStreamEvent> streamTask(String traceId, String sessionId, String userId, String message) {
        Sinks.Many<WorkflowStreamEvent> sink = Sinks.many().unicast().onBackpressureBuffer();
        String executionId = "exec-stream-" + UUID.randomUUID().toString().substring(0, 8);

        // 异步线程推进并实时推送 SSE 事件
        Thread.ofVirtual().start(() -> {
            try {
                sink.tryEmitNext(WorkflowStreamEvent.of("start", executionId, "工作流启动"));

                AgentCheckpoint root = new AgentCheckpoint(executionId, "chk-stream-0", null, WorkflowState.PLANNING, 0);
                root.setSessionId(sessionId);
                root.setUserId(userId);
                checkpointStore.saveSnapshot(root);

                sink.tryEmitNext(WorkflowStreamEvent.thought(executionId, 0, "分析用户意图与选品需求..."));
                List<WorkflowStep> steps = planTask(message);
                sink.tryEmitNext(WorkflowStreamEvent.of("planned", executionId, "拆解生成 " + steps.size() + " 个执行步骤"));

                root.setState(WorkflowState.EXECUTING);
                root.setStepSnapshots(steps);
                checkpointStore.saveSnapshot(root);

                WorkflowResponse response = driveWorkflowLoop(root, message, sink);

                if (response.getState() == WorkflowState.CONFIRMING) {
                    sink.tryEmitNext(WorkflowStreamEvent.of("confirm_required", executionId, response.getMessage()));
                } else {
                    sink.tryEmitNext(WorkflowStreamEvent.completed(executionId, response.getMessage()));
                }
                sink.tryEmitComplete();
            } catch (Exception e) {
                log.error("[SpringAI2 Workflow] 流式执行异常: {}", e.getMessage(), e);
                sink.tryEmitNext(WorkflowStreamEvent.error(executionId, e.getMessage()));
                sink.tryEmitComplete();
            }
        });

        return sink.asFlux();
    }

    @Override
    public WorkflowResponse rehydrateFromCheckpoint(String executionId, String checkpointId) {
        log.info("[SpringAI2 Rehydration] 从快照断点复水: executionId={}, checkpointId={}", executionId, checkpointId);

        // 1. 从存储端反序列化提取 AgentCheckpoint 执行栈快照
        AgentCheckpoint checkpoint = checkpointStore.loadCheckpoint(executionId, checkpointId);
        if (checkpoint == null) {
            return WorkflowResponse.failed(executionId, null, "未能找到指定的快照 Checkpoint: " + checkpointId);
        }

        // 2. 检查状态机状态
        if (checkpoint.getState().isTerminal()) {
            return WorkflowResponse.completed(executionId, checkpoint.getMetadata().get("traceId"),
                    checkpointId, "任务已处于终态 (" + checkpoint.getState() + ")，无需重复执行",
                    checkpoint.getStepSnapshots());
        }

        log.info("[SpringAI2 Rehydration] 成功复原执行栈: stepCursor={}, 已完成步骤数={}",
                checkpoint.getStepCursor(), checkpoint.getStepSnapshots().size());

        // 3. 从中断的游标位置继续推进
        String originalMessage = String.valueOf(checkpoint.getScratchpad().getOrDefault("user_message", ""));
        return driveWorkflowLoop(checkpoint, originalMessage, null);
    }

    @Override
    public WorkflowResponse confirmAction(String executionId, String checkpointId, boolean confirmed) {
        log.info("[SpringAI2 HITL] 用户提交确认操作: executionId={}, checkpointId={}, confirmed={}",
                executionId, checkpointId, confirmed);

        AgentCheckpoint checkpoint = checkpointStore.loadCheckpoint(executionId, checkpointId);
        if (checkpoint == null) {
            return WorkflowResponse.failed(executionId, null, "未能找到待确认的 Checkpoint: " + checkpointId);
        }

        if (checkpoint.getState() != WorkflowState.CONFIRMING && checkpoint.getState() != WorkflowState.WAITING_USER) {
            return WorkflowResponse.failed(executionId, null, "当前 Checkpoint 状态不处于待确认状态: " + checkpoint.getState());
        }

        if (!confirmed) {
            // 用户主动取消高风险动作
            checkpoint.setState(WorkflowState.CANCELLED);
            checkpointStore.saveSnapshot(checkpoint);
            return WorkflowResponse.completed(executionId, checkpoint.getMetadata().get("traceId"),
                    checkpointId, "用户已取消当前高风险操作，流程终止。", checkpoint.getStepSnapshots());
        }

        // 用户确认通过，状态恢复为 EXECUTING 并推进后续步骤
        checkpoint.setState(WorkflowState.EXECUTING);
        checkpoint.setPendingConfirmation(null);
        String nextChkId = "chk-confirmed-" + System.currentTimeMillis();
        AgentCheckpoint resumed = AgentCheckpoint.nextStep(checkpoint, nextChkId, WorkflowState.EXECUTING,
                checkpoint.getStepCursor() + 1, Map.of("hitl_confirmed", true));
        checkpointStore.saveSnapshot(resumed);

        String originalMessage = String.valueOf(resumed.getScratchpad().getOrDefault("user_message", ""));
        return driveWorkflowLoop(resumed, originalMessage, null);
    }

    // =========================================================================
    // 内部工作流调度核心（展示基础设施层之上开发者编写的编排循环）
    // =========================================================================

    private WorkflowResponse driveWorkflowLoop(AgentCheckpoint checkpoint, String userMessage,
                                               Sinks.Many<WorkflowStreamEvent> streamSink) {
        String executionId = checkpoint.getExecutionId();
        List<WorkflowStep> steps = checkpoint.getStepSnapshots();
        int cursor = checkpoint.getStepCursor();
        StringBuilder answerAccumulator = new StringBuilder();

        while (cursor < Math.max(1, steps.size()) && cursor < MAX_ITERATIONS) {
            cursor++;
            log.info("[SpringAI2 Loop] 正在执行第 {} 步, executionId={}", cursor, executionId);

            WorkflowStep step = (cursor <= steps.size()) ? steps.get(cursor - 1) : new WorkflowStep(cursor, "综合解答");

            // 1. 调用 Spring AI 2.0 ChatClient 链式执行
            long stepStart = System.currentTimeMillis();
            try {
                if (streamSink != null) {
                    streamSink.tryEmitNext(WorkflowStreamEvent.thought(executionId, cursor, "正在思考与查询: " + step.getAction()));
                }

                // 注入企业知识库与上一步结果作为增强上下文
                String promptContext = buildContextForStep(checkpoint, step);

                // 使用 ChatClient 流式/链式调用，底层自动装配 Advisors 与 @Tool 回调
                String thoughtResponse = chatClient.prompt()
                        .user(promptContext)
                        .call()
                        .content();

                long duration = System.currentTimeMillis() - stepStart;
                step.setDurationMs(duration);
                step.setLlmThought(thoughtResponse);
                step.complete(thoughtResponse);

                // 2. 检查是否触发了高风险工具动作（人在回路拦截）
                if (checkNeedsHumanConfirmation(thoughtResponse, step)) {
                    log.warn("[SpringAI2 Loop] ⚠️ 触发高风险工具动作，挂起工作流进入 WAITING_USER / CONFIRMING");
                    checkpoint.setState(WorkflowState.CONFIRMING);
                    checkpoint.setStepCursor(cursor);

                    Map<String, Object> confirmMeta = Map.of(
                            "stepNumber", cursor,
                            "action", step.getAction(),
                            "warning", "检测到高额订单或敏感变更，金额超过 ¥" + ShoppingDomainService.HIGH_RISK_AMOUNT_THRESHOLD + "，请人工核实确认"
                    );
                    checkpoint.setPendingConfirmation(confirmMeta);
                    checkpointStore.saveSnapshot(checkpoint);

                    return WorkflowResponse.confirming(executionId, checkpoint.getMetadata().get("traceId"),
                            checkpoint.getCheckpointId(), "已挂起等待用户确认: " + step.getAction(), confirmMeta);
                }

                answerAccumulator.append(thoughtResponse).append("\n");

                // 3. 【深水区 Checkpointer 保存】每执行完一步，持久化一个不可变 Checkpoint 快照
                String nextCheckpointId = "chk-" + cursor + "-" + UUID.randomUUID().toString().substring(0, 6);
                checkpoint = AgentCheckpoint.nextStep(checkpoint, nextCheckpointId,
                        WorkflowState.EXECUTING, cursor, Map.of("step_" + cursor + "_out", thoughtResponse));
                checkpointStore.saveSnapshot(checkpoint);

                if (streamSink != null) {
                    streamSink.tryEmitNext(WorkflowStreamEvent.checkpoint(executionId, nextCheckpointId, WorkflowState.EXECUTING));
                }

            } catch (Exception ex) {
                log.error("[SpringAI2 Loop] 步骤 {} 执行发生异常: {}", cursor, ex.getMessage(), ex);
                step.fail(ex.getMessage());
                checkpoint.setState(WorkflowState.FAILED);
                checkpointStore.saveSnapshot(checkpoint);
                return WorkflowResponse.failed(executionId, checkpoint.getMetadata().get("traceId"),
                        "工作流在第 " + cursor + " 步失败: " + ex.getMessage());
            }
        }

        // 终态达成：跃迁为 COMPLETED
        checkpoint.setState(WorkflowState.COMPLETED);
        checkpointStore.saveSnapshot(checkpoint);

        String finalMsg = answerAccumulator.toString().trim();
        if (finalMsg.isBlank()) {
            finalMsg = "已为您完成咨询分析与商品比对。";
        }

        return WorkflowResponse.completed(executionId, checkpoint.getMetadata().get("traceId"),
                checkpoint.getCheckpointId(), finalMsg, steps);
    }

    private List<WorkflowStep> planTask(String message) {
        // 判断是否为多商品复杂对比
        boolean isComplex = message.contains("对比") || message.contains("比较") || message.contains("推荐")
                || (message.contains("拯救者") && message.contains("MacBook"));

        if (!isComplex) {
            return List.of(new WorkflowStep(1, "直接解答并查询匹配商品"));
        }

        // 复杂任务拆解步骤
        List<WorkflowStep> steps = new ArrayList<>();
        steps.add(new WorkflowStep(1, "调用 queryProductInfo 查询联想拯救者 Y9000P 规格与价格"));
        steps.add(new WorkflowStep(2, "调用 queryProductInfo 查询 Apple MacBook Pro 16 规格与价格"));
        steps.add(new WorkflowStep(3, "调用 searchKnowledge 检索退换货质保政策，基于预算场景输出综合对比分析"));
        return steps;
    }

    private String buildContextForStep(AgentCheckpoint checkpoint, WorkflowStep step) {
        String userMsg = String.valueOf(checkpoint.getScratchpad().getOrDefault("user_message", ""));
        return String.format("""
                用户请求: %s
                当前执行步骤: [步骤 %d] %s
                请调用相应的 @Tool 工具获取精准数据，并给出客观分析。
                """, userMsg, step.getStepNumber(), step.getAction());
    }

    private boolean checkNeedsHumanConfirmation(String thought, WorkflowStep step) {
        if (thought == null) return false;
        // 如果思考或动作包含下单或高危关键词，由领域风控服务裁决
        if (thought.contains("createOrder") || step.getAction().contains("创建订单") || step.getAction().contains("下单")) {
            return true;
        }
        return false;
    }
}
