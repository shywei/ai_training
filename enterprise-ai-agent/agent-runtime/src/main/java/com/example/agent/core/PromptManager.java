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

                # 行为规则
                - 用户问购物相关问题时，基于商品库知识推荐，引用配置和亮点说明理由
                - 用户问非购物问题时，正常聊天，不要主动推荐商品
                - 不要每次对话都推荐商品，只在用户明确表达购物需求时推荐

                # 约束
                %s

                # 知识使用规则
                - 如果「参考信息」中有商品知识，请优先基于这些知识回答
                - 推荐商品时，引用具体的配置、价格、亮点来说明理由
                - 如果参考信息中没有相关内容，可以基于常识回答，但不要编造商品信息
                - 不确定的信息说「我不确定」

                # 记忆使用规则
                - 如果「参考信息」中有用户记忆（偏好、历史需求），请结合记忆个性化推荐
                - 当用户表达偏好、需求、约束时（如「我喜欢轻薄本」「预算8000」「记住xxx」），
                  主动调用 save_memory 工具保存，memory_type 用 preference
                - 不需要每次都保存记忆，只保存有价值的信息

                # 输出要求
                - 推荐商品时用编号引用，说明推荐理由
                - 回答简洁，不要过度展开
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
