package com.example.agent.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 执行追踪服务
 *
 * 记录 Agent 每一步的完整执行链路，支持：
 * - 实时查看当前执行状态
 * - 事后回溯执行过程
 * - 性能分析（每步耗时）
 *
 * 教学点：Agent 的执行不是黑盒，Tracing 是调试、审计、优化的基础。
 *
 * 生产环境通常接入 OpenTelemetry / Jaeger / Zipkin，
 * Demo 用内存存储简化实现。
 */
@Component
public class TraceService {

    private static final Logger log = LoggerFactory.getLogger(TraceService.class);

    /** traceId → TraceRecord */
    private final ConcurrentHashMap<String, TraceRecord> traces = new ConcurrentHashMap<>();

    /**
     * 创建新的 Trace
     */
    public TraceRecord createTrace(String userMessage) {
        String traceId = "trace-" + UUID.randomUUID().toString().substring(0, 8);
        TraceRecord record = new TraceRecord(traceId, userMessage);
        traces.put(traceId, record);
        log.info("[Trace] 新建追踪: {} | 用户消息: {}", traceId, userMessage);
        return record;
    }

    /**
     * 获取 Trace
     */
    public TraceRecord getTrace(String traceId) {
        return traces.get(traceId);
    }

    /**
     * 获取最近的 Trace 列表（最多 50 条）
     */
    public List<TraceRecord> getRecentTraces() {
        return traces.values().stream()
                .sorted((a, b) -> b.getStartTime().compareTo(a.getStartTime()))
                .limit(50)
                .toList();
    }

    /**
     * 记录步骤开始
     */
    public void recordStepStart(String traceId, WorkflowStep step) {
        TraceRecord record = traces.get(traceId);
        if (record != null) {
            record.addStep(step);
            log.info("[Trace] {} | Step {} 开始: {}", traceId, step.getStepNumber(), step.getAction());
        }
    }

    /**
     * 记录步骤完成
     */
    public void recordStepEnd(String traceId, WorkflowStep step) {
        TraceRecord record = traces.get(traceId);
        if (record != null) {
            log.info("[Trace] {} | Step {} 完成: {} ({}ms) 状态={}",
                    traceId, step.getStepNumber(), step.getAction(),
                    step.getDurationMs(), step.getStatus());
        }
    }

    /**
     * 记录状态变更
     */
    public void recordStateChange(String traceId, WorkflowState from, WorkflowState to) {
        TraceRecord record = traces.get(traceId);
        if (record != null) {
            record.setState(to);
            record.addEvent("状态变更: " + from + " → " + to);
            log.info("[Trace] {} | 状态变更: {} → {}", traceId, from, to);
        }
    }

    /**
     * 记录 LLM 思考
     */
    public void recordThought(String traceId, int stepNumber, String thought) {
        TraceRecord record = traces.get(traceId);
        if (record != null) {
            record.addEvent("Step " + stepNumber + " 思考: " + thought);
            log.info("[Trace] {} | Step {} 思考: {}", traceId, stepNumber,
                    thought.length() > 100 ? thought.substring(0, 100) + "..." : thought);
        }
    }

    /**
     * 记录工具调用
     */
    public void recordToolCall(String traceId, int stepNumber, String toolName, String input) {
        TraceRecord record = traces.get(traceId);
        if (record != null) {
            record.addEvent("Step " + stepNumber + " 调用工具: " + toolName);
            log.info("[Trace] {} | Step {} 调用工具: {} | 入参: {}",
                    traceId, stepNumber, toolName,
                    input.length() > 200 ? input.substring(0, 200) + "..." : input);
        }
    }

    /**
     * 记录重试
     */
    public void recordRetry(String traceId, int stepNumber, int retryCount, String reason) {
        TraceRecord record = traces.get(traceId);
        if (record != null) {
            record.addEvent("Step " + stepNumber + " 重试第 " + retryCount + " 次: " + reason);
            log.info("[Trace] {} | Step {} 重试第 {} 次: {}", traceId, stepNumber, retryCount, reason);
        }
    }

    /**
     * 记录确认请求
     */
    public void recordConfirmRequest(String traceId, int stepNumber, String confirmMessage) {
        TraceRecord record = traces.get(traceId);
        if (record != null) {
            record.addEvent("Step " + stepNumber + " 等待用户确认: " + confirmMessage);
            log.info("[Trace] {} | Step {} 等待确认: {}", traceId, stepNumber, confirmMessage);
        }
    }

    /**
     * 完成整个 Trace
     */
    public void completeTrace(String traceId, WorkflowState finalState, String summary) {
        TraceRecord record = traces.get(traceId);
        if (record != null) {
            record.setState(finalState);
            record.setSummary(summary);
            record.setEndTime(Instant.now());
            log.info("[Trace] {} | 完成: 状态={} 总耗时={}ms",
                    traceId, finalState, record.getTotalDurationMs());
        }
    }

    /**
     * Trace 记录
     */
    public static class TraceRecord {
        private final String traceId;
        private final String userMessage;
        private WorkflowState state;
        private final List<WorkflowStep> steps = new ArrayList<>();
        private final List<String> events = new ArrayList<>();
        private final Instant startTime;
        private Instant endTime;
        private String summary;

        public TraceRecord(String traceId, String userMessage) {
            this.traceId = traceId;
            this.userMessage = userMessage;
            this.state = WorkflowState.PLANNING;
            this.startTime = Instant.now();
        }

        public void addStep(WorkflowStep step) { steps.add(step); }
        public void addEvent(String event) { events.add(event); }

        public long getTotalDurationMs() {
            Instant end = endTime != null ? endTime : Instant.now();
            return Duration.between(startTime, end).toMillis();
        }

        // Getters & Setters
        public String getTraceId() { return traceId; }
        public String getUserMessage() { return userMessage; }
        public WorkflowState getState() { return state; }
        public void setState(WorkflowState state) { this.state = state; }
        public List<WorkflowStep> getSteps() { return steps; }
        public List<String> getEvents() { return events; }
        public Instant getStartTime() { return startTime; }
        public Instant getEndTime() { return endTime; }
        public void setEndTime(Instant endTime) { this.endTime = endTime; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
    }
}