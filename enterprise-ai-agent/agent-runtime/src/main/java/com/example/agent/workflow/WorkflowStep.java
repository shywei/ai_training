package com.example.agent.workflow;

import java.time.Duration;
import java.time.Instant;

/**
 * 工作流步骤模型
 *
 * 记录每个执行步骤的状态、耗时、结果，
 * 用于 Tracing 和调试。
 */
public class WorkflowStep {

    public enum Status { PENDING, RUNNING, SUCCESS, FAILED, SKIPPED, WAITING_CONFIRM }

    private final int stepNumber;
    private final String action;         // LLM 规划的动作描述
    private String toolName;             // 实际调用的工具名
    private String toolInput;            // 工具入参
    private String toolOutput;           // 工具返回
    private String llmThought;           // LLM 的思考过程
    private Status status;
    private int retryCount;
    private String errorMessage;
    private Instant startTime;
    private Instant endTime;

    public WorkflowStep(int stepNumber, String action) {
        this.stepNumber = stepNumber;
        this.action = action;
        this.status = Status.PENDING;
    }

    public void start() {
        this.startTime = Instant.now();
        this.status = Status.RUNNING;
    }

    public void complete(String output) {
        this.endTime = Instant.now();
        this.toolOutput = output;
        this.status = Status.SUCCESS;
    }

    public void fail(String error) {
        this.endTime = Instant.now();
        this.errorMessage = error;
        this.status = Status.FAILED;
    }

    public void skip(String reason) {
        this.endTime = Instant.now();
        this.errorMessage = reason;
        this.status = Status.SKIPPED;
    }

    public void waitForConfirm() {
        this.status = Status.WAITING_CONFIRM;
    }

    public long getDurationMs() {
        if (startTime == null) return 0;
        Instant end = endTime != null ? endTime : Instant.now();
        return Duration.between(startTime, end).toMillis();
    }

    // Getters & Setters
    public int getStepNumber() { return stepNumber; }
    public String getAction() { return action; }
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public String getToolInput() { return toolInput; }
    public void setToolInput(String toolInput) { this.toolInput = toolInput; }
    public String getToolOutput() { return toolOutput; }
    public void setToolOutput(String toolOutput) { this.toolOutput = toolOutput; }
    public String getLlmThought() { return llmThought; }
    public void setLlmThought(String llmThought) { this.llmThought = llmThought; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public int getRetryCount() { return retryCount; }
    public void incrementRetry() { this.retryCount++; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
}