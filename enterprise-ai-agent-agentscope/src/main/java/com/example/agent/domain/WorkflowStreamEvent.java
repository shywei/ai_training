package com.example.agent.domain;

import java.io.Serializable;
import java.time.Instant;

/**
 * 工作流实时流式事件 DTO（用于 SSE 推送）
 */
public class WorkflowStreamEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 事件类型：step_start, thought, tool_start, tool_end, checkpoint, confirm_required, token, completed, error */
    private String type;

    /** 关联的执行 ID */
    private String executionId;

    /** 检查点 ID (若产生) */
    private String checkpointId;

    /** 事件载荷内容（文本、JSON 或对象） */
    private Object payload;

    /** 当前步数 */
    private int stepNumber;

    /** 事件时间戳 */
    private Instant timestamp;

    public WorkflowStreamEvent() {
        this.timestamp = Instant.now();
    }

    public WorkflowStreamEvent(String type, String executionId, Object payload) {
        this.type = type;
        this.executionId = executionId;
        this.payload = payload;
        this.timestamp = Instant.now();
    }

    public static WorkflowStreamEvent of(String type, String executionId, Object payload) {
        return new WorkflowStreamEvent(type, executionId, payload);
    }

    public static WorkflowStreamEvent thought(String executionId, int step, String thought) {
        WorkflowStreamEvent event = new WorkflowStreamEvent("thought", executionId, thought);
        event.setStepNumber(step);
        return event;
    }

    public static WorkflowStreamEvent toolStart(String executionId, int step, String toolName, String input) {
        WorkflowStreamEvent event = new WorkflowStreamEvent("tool_start", executionId, "调用工具: " + toolName);
        event.setStepNumber(step);
        return event;
    }

    public static WorkflowStreamEvent toolEnd(String executionId, int step, String toolName, String output) {
        WorkflowStreamEvent event = new WorkflowStreamEvent("tool_end", executionId, output);
        event.setStepNumber(step);
        return event;
    }

    public static WorkflowStreamEvent checkpoint(String executionId, String checkpointId, WorkflowState state) {
        WorkflowStreamEvent event = new WorkflowStreamEvent("checkpoint", executionId, state.name());
        event.setCheckpointId(checkpointId);
        return event;
    }

    public static WorkflowStreamEvent completed(String executionId, String message) {
        return new WorkflowStreamEvent("completed", executionId, message);
    }

    public static WorkflowStreamEvent error(String executionId, String error) {
        return new WorkflowStreamEvent("error", executionId, error);
    }

    // Getters and Setters

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

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

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(int stepNumber) {
        this.stepNumber = stepNumber;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
