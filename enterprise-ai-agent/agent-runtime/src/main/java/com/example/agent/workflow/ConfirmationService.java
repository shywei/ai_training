package com.example.agent.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 人在回路服务（Human-in-the-loop）
 *
 * 高风险操作（下单、退款、修改数据）暂停执行，等待用户确认。
 *
 * 教学点：AI 不应该对所有操作都有执行权。
 * Human-in-the-loop 是企业安全的底线。
 *
 * 工作流：
 * 1. Agent 准备调用高风险 Tool
 * 2. ConfirmationService 拦截，暂停工作流
 * 3. 返回确认请求给前端
 * 4. 用户确认 → 工作流继续
 * 5. 用户取消 → 工作流中止
 */
@Component
public class ConfirmationService {

    private static final Logger log = LoggerFactory.getLogger(ConfirmationService.class);

    /**
     * 需要确认的高风险操作关键词
     * 包含这些词的 Tool 调用需要用户确认
     */
    private static final Set<String> HIGH_RISK_KEYWORDS = Set.of(
            "order", "place_order", "create_order",   // 下单
            "refund", "cancel_order",                  // 退款/取消
            "delete", "remove",                        // 删除
            "update_price", "modify_inventory",        // 修改数据
            "支付", "下单", "购买", "退款", "取消订单"     // 中文
    );

    /** traceId → 等待确认的上下文 */
    private final ConcurrentHashMap<String, PendingConfirmation> pendingConfirmations = new ConcurrentHashMap<>();

    /**
     * 判断工具调用是否需要用户确认
     */
    public boolean requiresConfirmation(String toolName, String toolInput) {
        if (toolName == null) return false;

        String lowerToolName = toolName.toLowerCase();
        for (String keyword : HIGH_RISK_KEYWORDS) {
            if (lowerToolName.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        // 也检查入参中是否包含高风险关键词
        if (toolInput != null) {
            String lowerInput = toolInput.toLowerCase();
            for (String keyword : HIGH_RISK_KEYWORDS) {
                if (lowerInput.contains(keyword.toLowerCase())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 创建确认请求，暂停工作流
     *
     * @return 确认提示消息
     */
    public String requestConfirmation(String traceId, int stepNumber,
                                       String toolName, String toolInput,
                                       String description) {
        PendingConfirmation pending = new PendingConfirmation(
                traceId, stepNumber, toolName, toolInput, description);
        pendingConfirmations.put(traceId, pending);

        log.info("[Confirmation] 工作流暂停: traceId={}, tool={}, desc={}",
                traceId, toolName, description);

        return String.format(
                "⚠️ **操作确认**\n\n" +
                "即将执行高风险操作：**%s**\n\n" +
                "操作详情：%s\n" +
                "工具：%s\n\n" +
                "请确认是否继续？",
                description, toolInput != null ? toolInput : "无", toolName);
    }

    /**
     * 用户确认，返回待执行的工具信息
     */
    public PendingConfirmation confirm(String traceId) {
        PendingConfirmation pending = pendingConfirmations.remove(traceId);
        if (pending != null) {
            log.info("[Confirmation] 用户确认: traceId={}, tool={}", traceId, pending.toolName);
        }
        return pending;
    }

    /**
     * 用户取消
     */
    public PendingConfirmation cancel(String traceId) {
        PendingConfirmation pending = pendingConfirmations.remove(traceId);
        if (pending != null) {
            log.info("[Confirmation] 用户取消: traceId={}, tool={}", traceId, pending.toolName);
        }
        return pending;
    }

    /**
     * 检查是否有待确认的操作
     */
    public boolean hasPendingConfirmation(String traceId) {
        return pendingConfirmations.containsKey(traceId);
    }

    /**
     * 获取待确认信息
     */
    public PendingConfirmation getPendingConfirmation(String traceId) {
        return pendingConfirmations.get(traceId);
    }

    /**
     * 待确认的上下文
     */
    public static class PendingConfirmation {
        private final String traceId;
        private final int stepNumber;
        private final String toolName;
        private final String toolInput;
        private final String description;
        private final long createdAt;

        public PendingConfirmation(String traceId, int stepNumber,
                                    String toolName, String toolInput, String description) {
            this.traceId = traceId;
            this.stepNumber = stepNumber;
            this.toolName = toolName;
            this.toolInput = toolInput;
            this.description = description;
            this.createdAt = System.currentTimeMillis();
        }

        public String getTraceId() { return traceId; }
        public int getStepNumber() { return stepNumber; }
        public String getToolName() { return toolName; }
        public String getToolInput() { return toolInput; }
        public String getDescription() { return description; }
        public long getCreatedAt() { return createdAt; }
    }
}