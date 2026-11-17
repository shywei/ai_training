package com.example.agent.port;

import com.example.agent.domain.WorkflowResponse;
import com.example.agent.domain.WorkflowStreamEvent;
import reactor.core.publisher.Flux;

/**
 * 六边形入站端口：导购智能体驱动端口（Inbound Driving Port）
 *
 * 对应第 09 课 P20~P22：
 * 控制器或外部触发源仅与此接口交互，完全感知不到底层是 Spring AI 2.0 还是 AgentScope。
 * 支持同步调用、反应式流式输出、基于 Checkpoint 的崩溃复水以及人在回路确认。
 */
public interface ShoppingAgentPort {

    /**
     * 执行导购任务
     *
     * @param traceId   全局追踪链路 ID
     * @param sessionId 会话 ID
     * @param userId    用户 ID
     * @param message   用户问题或意图
     * @return 结构化工作流响应
     */
    WorkflowResponse executeTask(String traceId, String sessionId, String userId, String message);

    /**
     * 流式执行导购任务（SSE 输出）
     *
     * @param traceId   全局追踪链路 ID
     * @param sessionId 会话 ID
     * @param userId    用户 ID
     * @param message   用户问题或意图
     * @return 实时事件响应流
     */
    Flux<WorkflowStreamEvent> streamTask(String traceId, String sessionId, String userId, String message);

    /**
     * 从指定检查点断点复水恢复执行（Rehydration）
     *
     * 解决 Pod 漂移、应用重启或中断长任务的无损恢复问题（对应 P07~P10）
     *
     * @param executionId  执行 ID
     * @param checkpointId 检查点版本 ID
     * @return 恢复后的执行结果
     */
    WorkflowResponse rehydrateFromCheckpoint(String executionId, String checkpointId);

    /**
     * 人在回路（HITL）交互确认恢复
     *
     * @param executionId  执行 ID
     * @param checkpointId 挂起时的检查点 ID
     * @param confirmed    用户是否同意（true=确认继续执行高风险动作，false=取消）
     * @return 确认后的推进结果
     */
    WorkflowResponse confirmAction(String executionId, String checkpointId, boolean confirmed);
}
