package com.example.agent.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 模型路由：根据任务复杂度选择不同的模型
 *
 * 第四课核心知识点：
 * - Fast Model: 日常对话、简单任务，便宜快
 * - Reasoning Model: 复杂推理、架构设计，贵但强
 * - 动态路由: 代码层判断用哪个模型
 */
@Component
public class ModelRouter {

    @Value("${agent.model.fast}")
    private String fastModel;

    @Value("${agent.model.reasoning}")
    private String reasoningModel;

    /**
     * 根据消息复杂度选择模型
     * 简单规则：消息短 + 无代码关键词 → 快模型
     * 复杂规则：消息长 + 含代码/架构关键词 → 推理模型
     */
    public String selectModel(String userMessage) {
        if (needsReasoning(userMessage)) {
            return reasoningModel;
        }
        return fastModel;
    }

    private boolean needsReasoning(String message) {
        if (message == null) return false;
        // 长消息通常更复杂
        if (message.length() > 500) return true;
        // 包含技术关键词
        String lower = message.toLowerCase();
        String[] keywords = {"架构", "重构", "设计", "分析", "对比", "优化", "debug", "报错", "异常"};
        for (String kw : keywords) {
            if (lower.contains(kw)) return true;
        }
        return false;
    }

    public String getFastModel() { return fastModel; }
    public String getReasoningModel() { return reasoningModel; }
}
