package com.example.agent.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Prompt 管理器：构建 System Prompt 和 User Prompt
 *
 * 第四课核心知识点：
 * - System Prompt vs User Prompt 职责划分
 * - Prompt Template 变量替换
 * - 角色 + 能力 + 约束 三要素
 */
@Component
public class PromptManager {

    @Value("${agent.name}")
    private String agentName;

    @Value("${agent.role}")
    private String agentRole;

    @Value("${agent.constraints}")
    private String agentConstraints;

    /**
     * 构建 System Prompt
     * 包含：角色定义 + 能力边界 + 行为约束
     */
    public String buildSystemPrompt() {
        return String.format("""
                # 角色
                你是%s。%s
                
                # 能力
                - 根据用户需求推荐合适的商品
                - 解答商品相关问题
                - 提供购物建议
                
                # 约束
                %s
                
                # 输出要求
                - 回答简洁明了
                - 推荐商品时说明理由
                - 不确定的信息不要编造
                """, agentName, agentRole, agentConstraints);
    }

    /**
     * 构建带上下文的 User Prompt
     * v1.0: context = null（无记忆）
     * v2.0: context = 对话历史（Session Memory）
     * v3.0: context = 对话历史 + RAG 检索结果
     * v4.0: context = 对话历史 + RAG + Tool 调用结果
     */
    public String buildUserPrompt(String userMessage, String context) {
        if (context == null || context.isBlank()) {
            return userMessage;
        }
        return String.format("""
                ## 参考信息
                %s
                
                ## 用户问题
                %s
                """, context, userMessage);
    }
}
