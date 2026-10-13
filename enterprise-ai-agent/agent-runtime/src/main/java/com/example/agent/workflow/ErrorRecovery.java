package com.example.agent.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 容错服务：重试 + 熔断 + 降级
 *
 * 企业环境中，MCP Server 可能超时、限流、宕机。
 * Agent 不能假设工具永远可用。
 *
 * 策略：
 * 1. 重试：最多 3 次，指数退避（1s → 2s → 4s）
 * 2. 熔断：同一工具连续失败 5 次，熔断 60 秒
 * 3. 降级：熔断后跳过工具调用，让 LLM 基于已有 context 直接回答
 *
 * 教学点：生产环境的 Agent 必须有容错能力，不能因为一次工具调用失败就崩溃。
 */
@Component
public class ErrorRecovery {

    private static final Logger log = LoggerFactory.getLogger(ErrorRecovery.class);

    /** 最大重试次数 */
    private static final int MAX_RETRY = 3;

    /** 熔断阈值：连续失败次数 */
    private static final int CIRCUIT_BREAKER_THRESHOLD = 5;

    /** 熔断恢复时间（毫秒）：60 秒 */
    private static final long CIRCUIT_BREAKER_RECOVERY_MS = 60_000;

    /** 工具失败计数器 */
    private final ConcurrentHashMap<String, ToolFailureInfo> failureCounters = new ConcurrentHashMap<>();

    /**
     * 判断是否应该重试
     */
    public boolean shouldRetry(WorkflowStep step) {
        return step.getRetryCount() < MAX_RETRY;
    }

    /**
     * 获取重试延迟（指数退避）
     * 第 1 次：1 秒，第 2 次：2 秒，第 3 次：4 秒
     */
    public long getRetryDelayMs(WorkflowStep step) {
        return (long) Math.pow(2, step.getRetryCount()) * 1000;
    }

    /**
     * 执行重试等待
     */
    public void waitBeforeRetry(WorkflowStep step) {
        long delay = getRetryDelayMs(step);
        log.info("[ErrorRecovery] 重试等待 {}ms (第 {} 次重试)", delay, step.getRetryCount() + 1);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 记录工具调用成功
     */
    public void recordSuccess(String toolName) {
        failureCounters.remove(toolName);
    }

    /**
     * 记录工具调用失败
     */
    public void recordFailure(String toolName, String error) {
        ToolFailureInfo info = failureCounters.computeIfAbsent(toolName, k -> new ToolFailureInfo());
        info.failureCount++;
        info.lastFailureTime = System.currentTimeMillis();
        info.lastError = error;

        log.warn("[ErrorRecovery] 工具 {} 失败 ({}/{}): {}",
                toolName, info.failureCount, CIRCUIT_BREAKER_THRESHOLD, error);

        if (info.failureCount >= CIRCUIT_BREAKER_THRESHOLD) {
            log.warn("[ErrorRecovery] ⚡ 工具 {} 触发熔断！暂停 {} 秒",
                    toolName, CIRCUIT_BREAKER_RECOVERY_MS / 1000);
        }
    }

    /**
     * 判断工具是否处于熔断状态
     */
    public boolean isCircuitBroken(String toolName) {
        ToolFailureInfo info = failureCounters.get(toolName);
        if (info == null || info.failureCount < CIRCUIT_BREAKER_THRESHOLD) {
            return false;
        }

        // 检查是否已过恢复时间
        long elapsed = System.currentTimeMillis() - info.lastFailureTime;
        if (elapsed >= CIRCUIT_BREAKER_RECOVERY_MS) {
            log.info("[ErrorRecovery] 工具 {} 熔断恢复，重新尝试", toolName);
            info.failureCount = 0;
            return false;
        }

        return true;
    }

    /**
     * 获取降级消息
     * 当工具熔断时，返回提示信息让 LLM 知道工具不可用
     */
    public String getDegradationMessage(String toolName) {
        ToolFailureInfo info = failureCounters.get(toolName);
        if (info != null) {
            return "[系统提示: 工具 " + toolName + " 当前不可用（连续失败 " + info.failureCount + " 次），请基于已有信息回答用户。";
        }
        return "[系统提示: 工具 " + toolName + " 当前不可用，请基于已有信息回答用户。";
    }

    /**
     * 重置所有计数器（管理接口用）
     */
    public void resetAll() {
        failureCounters.clear();
        log.info("[ErrorRecovery] 所有熔断计数器已重置");
    }

    /**
     * 获取当前状态
     */
    public java.util.Map<String, Object> getStatus() {
        java.util.Map<String, Object> status = new java.util.LinkedHashMap<>();
        failureCounters.forEach((tool, info) -> {
            java.util.Map<String, Object> toolStatus = new java.util.LinkedHashMap<>();
            toolStatus.put("failureCount", info.failureCount);
            toolStatus.put("lastError", info.lastError);
            toolStatus.put("circuitBroken", isCircuitBroken(tool));
            status.put(tool, toolStatus);
        });
        return status;
    }

    private static class ToolFailureInfo {
        int failureCount;
        long lastFailureTime;
        String lastError;
    }
}