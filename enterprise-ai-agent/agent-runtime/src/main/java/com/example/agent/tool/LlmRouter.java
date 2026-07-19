package com.example.agent.tool;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * LLM 路由器 — 用快模型判断用户意图
 *
 * 替代第六课的关键词过滤，升级为语义理解的意图分类。
 *
 * 第七课核心教学点：
 * - 关键词过滤的局限：无法理解语义（"帮我看看那个笔记本"不含"推荐"但需要检索）
 * - LLM 路由：用快模型先判断意图，再决定调什么
 * - 成本：每次路由多一次 LLM 调用，但用最便宜的快模型（~100 token）
 *
 * 路由决策：
 * - KNOWLEDGE：需要检索商品知识（"推荐笔记本"、"有什么手机"）
 * - TOOL：需要查价格/库存/下单（"多少钱"、"有货吗"、"帮我下单"）
 * - CHAT：普通聊天（"你好"、"今天天气怎么样"）
 */
@Component
public class LlmRouter {

    private final ChatModel chatModel;
    private final String fastModel;

    public LlmRouter(ChatModel chatModel,
                     @Value("${agent.model.fast}") String fastModel) {
        this.chatModel = chatModel;
        this.fastModel = fastModel;
    }

    /**
     * 路由决策
     */
    public enum Route {
        KNOWLEDGE,  // 需要检索知识库
        TOOL,       // 需要调用工具（查价格/库存/下单）
        CHAT        // 普通对话
    }

    /**
     * 判断用户意图
     *
     * @param userMessage 用户消息
     * @return 路由决策
     */
    public Route route(String userMessage) {
        String prompt = """
                判断用户消息的意图，只输出一个词：
                - KNOWLEDGE：需要查询商品信息、推荐商品、了解商品配置和功能
                - TOOL：需要查询价格、库存、是否有货、下单、购买
                - CHAT：普通聊天、问候、闲聊、与购物无关的问题
                
                用户消息：%s
                
                只输出 KNOWLEDGE、TOOL 或 CHAT，不要输出其他内容。
                """.formatted(userMessage);

        try {
            String result = ChatClient.create(chatModel)
                    .prompt()
                    .user(prompt)
                    .options(OpenAiChatOptions.builder()
                            .withModel(fastModel)
                            .withTemperature(0.0)
                            .build())
                    .call()
                    .content();

            if (result == null) return Route.CHAT;

            result = result.trim().toUpperCase();

            if (result.contains("TOOL")) return Route.TOOL;
            if (result.contains("KNOWLEDGE")) return Route.KNOWLEDGE;
            return Route.CHAT;

        } catch (Exception e) {
            System.err.println("[Router] 路由判断失败，默认 CHAT: " + e.getMessage());
            return Route.CHAT;
        }
    }
}
