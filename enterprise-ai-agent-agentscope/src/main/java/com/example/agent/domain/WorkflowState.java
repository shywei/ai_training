package com.example.agent.domain;

/**
 * 工作流状态机枚举（7 个核心状态 + 挂起/取消状态）
 *
 * 对应第 08 课手搓状态机与第 09 课框架边界：
 * PLANNING      — 任务规划中（拆解多步）
 * EXECUTING     — 步骤执行中（ReAct 循环）
 * CONFIRMING    — 人在回路（高风险工具拦截待确认）
 * WAITING_USER  — 异步挂起等待外部用户输入或审批
 * RETRYING      — 异常重试中（容错机制触发）
 * COMPLETED     — 任务正常完成
 * FAILED        — 任务失败（重试耗尽或不可恢复异常）
 * CANCELLED     — 用户主动取消或超时撤回
 */
public enum WorkflowState {
    PLANNING,
    EXECUTING,
    CONFIRMING,
    WAITING_USER,
    RETRYING,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }

    public boolean isSuspended() {
        return this == CONFIRMING || this == WAITING_USER;
    }
}
