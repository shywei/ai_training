package com.example.agent.adapter.agentscope;

import com.example.agent.domain.*;
import com.example.agent.port.CheckpointStorePort;
import com.example.agent.port.EnterpriseToolPort;
import com.example.agent.port.ShoppingAgentPort;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.Msg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.*;

/**
 * 六边形架构入站适配器（Inbound Adapter）：AgentScope 2.0 智能体编排适配器
 *
 * 对应第 09 课 P06、P19、P22（从 651 行手搓状态机胶水到 ~110 行框架优雅编排）：
 *
 * ────────── 生产力跃迁质变对比 ──────────
 * 1. 【手搓 v5.0 AgentRuntime 的代价】：
 *    - 651 行极其臃肿的 while (iteration < MAX_ITERATIONS) 状态机循环胶水。
 *    - 手写 THINK / ACT / OBSERVE 状态流转、手写正则表达式解析 TOOL_CALL、手写熔断器与重试计数。
 *
 * 2. 【AgentScope 2.0 GA 框架重构后】：
 *    - 核心编排代码浓缩至约 110 行！
 *    - ReActAgent 原生接管多轮工具调用决策、最大迭代次数收敛与内部异常恢复。
 *    - 人在回路审批挂起通过 Middleware 与 CheckpointStore 声明式联动。
 *    - 与领域接口 ShoppingAgentPort 严格对齐，实现与 Spring AI 2.0 的无缝平替！
 */
@Service
public class AgentScopeWorkflowAdapter implements ShoppingAgentPort {

    private static final Logger log = LoggerFactory.getLogger(AgentScopeWorkflowAdapter.class);

    private final ReActAgent shoppingAgent;
    private final CheckpointStorePort checkpointStore;
    private final EnterpriseToolPort enterpriseToolPort;
    private final AgentEventStreamBridge eventBridge;

    public AgentScopeWorkflowAdapter(
            ReActAgent shoppingAgent,
            CheckpointStorePort checkpointStore,
            EnterpriseToolPort enterpriseToolPort,
            AgentEventStreamBridge eventBridge) {
        this.shoppingAgent = shoppingAgent;
        this.checkpointStore = checkpointStore;
        this.enterpriseToolPort = enterpriseToolPort;
        this.eventBridge = eventBridge;
    }

    @Override
    public WorkflowResponse executeTask(String traceId, String sessionId, String userId, String message) {
        String execId = (traceId != null && !traceId.isBlank()) ? traceId : "v7-exec-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("[AgentScopeWorkflowAdapter] 执行导购任务: execId={}, msg={}", execId, message);

        RuntimeContext context = RuntimeContext.builder()
                .sessionId(execId)
                .userId(userId != null ? userId : "U001")
                .build();

        try {
            // 一行代码唤起 AgentScope ReAct 循环（消灭手搓 651 行 while 状态机胶水）
            Msg responseMsg = shoppingAgent.call(message, context).block();
            String answer = responseMsg != null ? responseMsg.getTextContent() : "";

            // 检查是否被中间件挂起触发人在回路审批 (HITL)
            Boolean approvalRequired = context.get("APPROVAL_REQUIRED");
            if (Boolean.TRUE.equals(approvalRequired)) {
                String checkpointId = context.get("LAST_CHECKPOINT_ID");
                AgentCheckpoint cp = checkpointStore.loadCheckpoint(execId, checkpointId);
                log.info("[AgentScopeWorkflowAdapter] 任务被风控中间件挂起，等待用户审批: cp={}", checkpointId);
                return WorkflowResponse.confirming(
                        execId, execId, checkpointId,
                        "大额高风险动作已拦截，等待用户确认",
                        cp != null ? cp.getPendingConfirmation() : Map.of()
                );
            }

            // 保存终态检查点快照
            String finalCpId = "cp-done-" + UUID.randomUUID().toString().substring(0, 8);
            AgentCheckpoint finalCp = new AgentCheckpoint(execId, finalCpId, null, WorkflowState.COMPLETED, 1);
            finalCp.setSessionId(sessionId);
            finalCp.setUserId(userId);
            checkpointStore.saveSnapshot(finalCp);

            WorkflowStep step = new WorkflowStep(1, "AgentScope ReAct Reasoning & Execution");
            step.complete(answer);
            return WorkflowResponse.completed(execId, execId, finalCpId, answer, List.of(step));

        } catch (Exception e) {
            log.error("[AgentScopeWorkflowAdapter] 智能体执行异常: {}", e.getMessage(), e);
            return WorkflowResponse.failed(execId, execId, "智能体执行失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<WorkflowStreamEvent> streamTask(String traceId, String sessionId, String userId, String message) {
        String execId = (traceId != null && !traceId.isBlank()) ? traceId : "v7-stream-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("[AgentScopeWorkflowAdapter] 开启 28 类多通道事件流: execId={}", execId);

        RuntimeContext context = RuntimeContext.builder()
                .sessionId(execId)
                .userId(userId != null ? userId : "U001")
                .build();

        Flux<AgentEvent> eventFlux = shoppingAgent.streamEvents(message, context);
        return eventBridge.bridgeToWorkflowStream(execId, eventFlux);
    }

    @Override
    public WorkflowResponse rehydrateFromCheckpoint(String executionId, String checkpointId) {
        log.info("[AgentScopeWorkflowAdapter:Rehydration] 执行断点复水: execId={}, checkpointId={}", executionId, checkpointId);
        AgentCheckpoint cp = checkpointStore.loadCheckpoint(executionId, checkpointId);
        if (cp == null) {
            return WorkflowResponse.failed(executionId, executionId, "指定快照不存在: " + checkpointId);
        }

        if (cp.getState() == WorkflowState.COMPLETED) {
            return WorkflowResponse.completed(executionId, executionId, checkpointId, "快照已处终态，无需复水", List.of());
        }

        String resumeMessage = "继续执行先前在步骤 " + cp.getStepCursor() + " 挂起的任务，历史调用栈: " + cp.getCallStack();
        return executeTask(executionId, cp.getSessionId(), cp.getUserId(), resumeMessage);
    }

    @Override
    public WorkflowResponse confirmAction(String executionId, String checkpointId, boolean confirmed) {
        log.info("[AgentScopeWorkflowAdapter:HITL] 收到审批确认: execId={}, checkpointId={}, confirmed={}",
                executionId, checkpointId, confirmed);

        AgentCheckpoint cp = checkpointStore.loadCheckpoint(executionId, checkpointId);
        if (cp == null) {
            return WorkflowResponse.failed(executionId, executionId, "未找到关联审批快照: " + checkpointId);
        }

        if (!confirmed) {
            String cancelCpId = "cp-cancel-" + UUID.randomUUID().toString().substring(0, 8);
            AgentCheckpoint cancelCp = new AgentCheckpoint(executionId, cancelCpId, checkpointId, WorkflowState.CANCELLED, cp.getStepCursor());
            cancelCp.getMetadata().put("cancelReason", "用户拒绝执行高风险动作");
            checkpointStore.saveSnapshot(cancelCp);
            WorkflowResponse resp = new WorkflowResponse();
            resp.setExecutionId(executionId);
            resp.setCheckpointId(cancelCpId);
            resp.setStatus("cancelled");
            resp.setState(WorkflowState.CANCELLED);
            resp.setMessage("高风险操作已被用户取消");
            return resp;
        }

        // 用户确认批准：执行挂起的工具操作（如正式成单）
        Map<String, Object> pending = cp.getPendingConfirmation();
        String productId = "NB001";
        Integer quantity = 1;
        BigDecimal totalAmount = new BigDecimal("8999.00");
        String address = "北京市海淀区中关村南大街1号院";
        String phone = "13800138000";

        if (pending != null && pending.containsKey("arguments")) {
            Map<?, ?> args = (Map<?, ?>) pending.get("arguments");
            if (args.containsKey("productId")) productId = args.get("productId").toString();
            if (args.containsKey("quantity")) quantity = ((Number) args.get("quantity")).intValue();
            if (args.containsKey("shippingAddress")) address = args.get("shippingAddress").toString();
            if (args.containsKey("contactPhone")) phone = args.get("contactPhone").toString();
            if (args.containsKey("totalAmount")) totalAmount = new BigDecimal(args.get("totalAmount").toString());
        }

        ToolOrderRequest orderReq = new ToolOrderRequest("U001", productId, quantity, totalAmount, address, phone, "CONFIRM-" + System.currentTimeMillis());
        Map<String, Object> orderResult = enterpriseToolPort.createOrder(orderReq);

        String doneCpId = "cp-approved-" + UUID.randomUUID().toString().substring(0, 8);
        AgentCheckpoint doneCp = new AgentCheckpoint(executionId, doneCpId, checkpointId, WorkflowState.COMPLETED, cp.getStepCursor() + 1);
        doneCp.getMetadata().put("orderResult", orderResult.toString());
        checkpointStore.saveSnapshot(doneCp);

        WorkflowStep step = new WorkflowStep(cp.getStepCursor() + 1, "createOrder (HITL Approved)");
        step.complete(orderResult.toString());
        return WorkflowResponse.completed(executionId, executionId, doneCpId,
                "审批通过，高风险订单已成功提交！订单单号: " + orderResult.get("orderId"), List.of(step));
    }
}
