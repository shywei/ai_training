package com.example.agent.domain;

import java.io.Serializable;

/**
 * 工作流执行步骤模型（纯 POJO）
 */
public class WorkflowStep implements Serializable {

    private static final long serialVersionUID = 1L;

    private int stepNumber;
    private String action;
    private String toolName;
    private String toolInput;
    private String toolOutput;
    private String llmThought;
    private String status; // pending, running, completed, failed, skipped
    private int retryCount;
    private long durationMs;

    public WorkflowStep() {
        this.status = "pending";
    }

    public WorkflowStep(int stepNumber, String action) {
        this.stepNumber = stepNumber;
        this.action = action;
        this.status = "pending";
    }

    public void complete(String output) {
        this.toolOutput = output;
        this.status = "completed";
    }

    public void fail(String error) {
        this.toolOutput = error;
        this.status = "failed";
    }

    // Getters and Setters

    public int getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(int stepNumber) {
        this.stepNumber = stepNumber;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getToolInput() {
        return toolInput;
    }

    public void setToolInput(String toolInput) {
        this.toolInput = toolInput;
    }

    public String getToolOutput() {
        return toolOutput;
    }

    public void setToolOutput(String toolOutput) {
        this.toolOutput = toolOutput;
    }

    public String getLlmThought() {
        return llmThought;
    }

    public void setLlmThought(String llmThought) {
        this.llmThought = llmThought;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    @Override
    public String toString() {
        return "WorkflowStep{" +
                "stepNumber=" + stepNumber +
                ", action='" + action + '\'' +
                ", toolName='" + toolName + '\'' +
                ", status='" + status + '\'' +
                ", durationMs=" + durationMs +
                '}';
    }
}
