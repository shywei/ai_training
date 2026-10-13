package com.example.agent.workflow;

/**
 * 工作流状态机
 *
 * 用有限状态机管理 Agent 的执行生命周期，
 * 替代纯 ReAct 的"LLM 想到哪算哪"模式。
 *
 * 状态转换：
 *   PLANNING → EXECUTING → COMPLETED
 *                   ↓  ↑
 *                RETRYING
 *                   ↓
 *                 FAILED
 *
 *   EXECUTING → CONFIRMING → EXECUTING（用户确认后继续）
 *                   ↓
 *                 CANCELLED（用户取消）
 */
public enum WorkflowState {

    /** 任务规划中：LLM 正在分解步骤 */
    PLANNING,

    /** 执行中：Agent 正在执行 ReAct 循环 */
    EXECUTING,

    /** 等待确认：高风险操作需要用户确认 */
    CONFIRMING,

    /** 重试中：工具调用失败，正在重试 */
    RETRYING,

    /** 已完成：所有步骤执行成功 */
    COMPLETED,

    /** 已失败：重试耗尽或不可恢复的错误 */
    FAILED,

    /** 已取消：用户主动取消或拒绝确认 */
    CANCELLED;

    /**
     * 判断是否为终态（不会再转换）
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }

    /**
     * 判断是否可以接受用户确认
     */
    public boolean canConfirm() {
        return this == CONFIRMING;
    }
}