package com.example.agent.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;

/**
 * 统一状态快照领域模型（Agent Checkpoint）
 *
 * 对应第 09 课 P07~P10（深度深水区 1：有状态 Agent 的序列化与断点复水）：
 * 彻底理清并区分三级存储边界：
 * 1. 短期对话历史（Chat History）：用户与 Agent 的原始问答文本列表，用于 LLM 上下文渲染；
 * 2. 状态存储（Agent State Store）：会话级别的画像、偏好等全局事实数据；
 * 3. 执行快照（Execution Checkpointer）：任务执行栈状态机（Call Stack、Scratchpad、游标、增量差分），
 *    用于支持跨进程崩溃恢复（Rehydration）与分支回滚（Forking / Time-Travel）。
 */
public class AgentCheckpoint implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务执行全局唯一 ID（对应 LangGraph thread_id / 本工程 executionId） */
    private String executionId;

    /** 当前检查点版本 ID（单调递增或 UUID，对应 checkpoint_id） */
    private String checkpointId;

    /** 父检查点 ID（构建因果链 DAG 与时光倒流分支树） */
    private String parentCheckpointId;

    /** 关联的用户会话 ID */
    private String sessionId;

    /** 用户 ID */
    private String userId;

    /** 核心状态机状态 (WorkflowState) */
    private WorkflowState state;

    /** 当前执行步骤游标 (从 0 开始的 Step 索引) */
    private int stepCursor;

    /** 执行调用栈（保存多级调用或子目标任务路径） */
    private List<String> callStack = new ArrayList<>();

    /** 思考工作区暂存变量（私有 Scratchpad，存放中间计算结果、工具输出聚合） */
    private Map<String, Object> scratchpad = new HashMap<>();

    /** 增量状态差分（State Delta，记录相对于上一 Checkpoint 的局部变更） */
    private Map<String, Object> stateDelta = new HashMap<>();

    /** 关联的已完成步骤快照列表 */
    private List<WorkflowStep> stepSnapshots = new ArrayList<>();

    /** 待确认的人在回路（HITL）操作元数据（处于 WAITING_USER / CONFIRMING 时有效） */
    private Map<String, Object> pendingConfirmation;

    /** 快照生成时间戳 */
    private Instant createdAt;

    /** 额外元数据（如版本号、节点标签、Trigger 来源） */
    private Map<String, String> metadata = new HashMap<>();

    public AgentCheckpoint() {
        this.createdAt = Instant.now();
    }

    public AgentCheckpoint(String executionId, String checkpointId, String parentCheckpointId,
                           WorkflowState state, int stepCursor) {
        this.executionId = executionId;
        this.checkpointId = checkpointId;
        this.parentCheckpointId = parentCheckpointId;
        this.state = state;
        this.stepCursor = stepCursor;
        this.createdAt = Instant.now();
    }

    // 工厂方法：从上一个 Checkpoint 衍生出下一个步骤 Checkpoint（全量 + 差分）
    public static AgentCheckpoint nextStep(AgentCheckpoint parent, String nextCheckpointId,
                                           WorkflowState newState, int nextStepCursor,
                                           Map<String, Object> delta) {
        AgentCheckpoint next = new AgentCheckpoint();
        next.setExecutionId(parent.getExecutionId());
        next.setCheckpointId(nextCheckpointId);
        next.setParentCheckpointId(parent.getCheckpointId());
        next.setSessionId(parent.getSessionId());
        next.setUserId(parent.getUserId());
        next.setState(newState);
        next.setStepCursor(nextStepCursor);

        // 继承并深拷贝调用栈与工作区
        next.setCallStack(new ArrayList<>(parent.getCallStack()));
        Map<String, Object> mergedScratchpad = new HashMap<>(parent.getScratchpad());
        if (delta != null && !delta.isEmpty()) {
            mergedScratchpad.putAll(delta);
            next.setStateDelta(new HashMap<>(delta));
        }
        next.setScratchpad(mergedScratchpad);
        next.setStepSnapshots(new ArrayList<>(parent.getStepSnapshots()));
        next.setMetadata(new HashMap<>(parent.getMetadata()));
        return next;
    }

    // Getters and Setters

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getCheckpointId() {
        return checkpointId;
    }

    public void setCheckpointId(String checkpointId) {
        this.checkpointId = checkpointId;
    }

    public String getParentCheckpointId() {
        return parentCheckpointId;
    }

    public void setParentCheckpointId(String parentCheckpointId) {
        this.parentCheckpointId = parentCheckpointId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public WorkflowState getState() {
        return state;
    }

    public void setState(WorkflowState state) {
        this.state = state;
    }

    public int getStepCursor() {
        return stepCursor;
    }

    public void setStepCursor(int stepCursor) {
        this.stepCursor = stepCursor;
    }

    public List<String> getCallStack() {
        return callStack;
    }

    public void setCallStack(List<String> callStack) {
        this.callStack = callStack != null ? callStack : new ArrayList<>();
    }

    public Map<String, Object> getScratchpad() {
        return scratchpad;
    }

    public void setScratchpad(Map<String, Object> scratchpad) {
        this.scratchpad = scratchpad != null ? scratchpad : new HashMap<>();
    }

    public Map<String, Object> getStateDelta() {
        return stateDelta;
    }

    public void setStateDelta(Map<String, Object> stateDelta) {
        this.stateDelta = stateDelta != null ? stateDelta : new HashMap<>();
    }

    public List<WorkflowStep> getStepSnapshots() {
        return stepSnapshots;
    }

    public void setStepSnapshots(List<WorkflowStep> stepSnapshots) {
        this.stepSnapshots = stepSnapshots != null ? stepSnapshots : new ArrayList<>();
    }

    public Map<String, Object> getPendingConfirmation() {
        return pendingConfirmation;
    }

    public void setPendingConfirmation(Map<String, Object> pendingConfirmation) {
        this.pendingConfirmation = pendingConfirmation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }
}
