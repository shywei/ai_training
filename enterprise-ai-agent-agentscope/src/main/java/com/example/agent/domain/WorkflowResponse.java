package com.example.agent.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 工作流执行统一响应传输对象（兼容原工程响应规范，并扩展 Checkpoint 元数据）
 */
public class WorkflowResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String executionId;
    private String traceId;
    private String checkpointId;
    private String status; // completed, confirming, waiting_user, failed, cancelled
    private String message;
    private WorkflowState state;
    private String mode; // simple, workflow
    private int stepCount;
    private List<WorkflowStep> steps = Collections.emptyList();
    private Map<String, Object> pendingConfirmation;

    public WorkflowResponse() {
    }

    public static WorkflowResponse completed(String executionId, String traceId, String checkpointId,
                                            String message, List<WorkflowStep> steps) {
        WorkflowResponse resp = new WorkflowResponse();
        resp.setExecutionId(executionId);
        resp.setTraceId(traceId);
        resp.setCheckpointId(checkpointId);
        resp.setStatus("completed");
        resp.setMessage(message);
        resp.setState(WorkflowState.COMPLETED);
        resp.setSteps(steps != null ? steps : Collections.emptyList());
        resp.setStepCount(resp.getSteps().size());
        resp.setMode(resp.getStepCount() <= 1 ? "simple" : "workflow");
        return resp;
    }

    public static WorkflowResponse confirming(String executionId, String traceId, String checkpointId,
                                              String message, Map<String, Object> pendingConfirmation) {
        WorkflowResponse resp = new WorkflowResponse();
        resp.setExecutionId(executionId);
        resp.setTraceId(traceId);
        resp.setCheckpointId(checkpointId);
        resp.setStatus("confirming");
        resp.setMessage(message);
        resp.setState(WorkflowState.CONFIRMING);
        resp.setPendingConfirmation(pendingConfirmation);
        resp.setMode("workflow");
        return resp;
    }

    public static WorkflowResponse failed(String executionId, String traceId, String message) {
        WorkflowResponse resp = new WorkflowResponse();
        resp.setExecutionId(executionId);
        resp.setTraceId(traceId);
        resp.setStatus("failed");
        resp.setMessage(message);
        resp.setState(WorkflowState.FAILED);
        return resp;
    }

    // Getters and Setters

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getCheckpointId() {
        return checkpointId;
    }

    public void setCheckpointId(String checkpointId) {
        this.checkpointId = checkpointId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public WorkflowState getState() {
        return state;
    }

    public void setState(WorkflowState state) {
        this.state = state;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getStepCount() {
        return stepCount;
    }

    public void setStepCount(int stepCount) {
        this.stepCount = stepCount;
    }

    public List<WorkflowStep> getSteps() {
        return steps;
    }

    public void setSteps(List<WorkflowStep> steps) {
        this.steps = steps != null ? steps : Collections.emptyList();
    }

    public Map<String, Object> getPendingConfirmation() {
        return pendingConfirmation;
    }

    public void setPendingConfirmation(Map<String, Object> pendingConfirmation) {
        this.pendingConfirmation = pendingConfirmation;
    }
}
