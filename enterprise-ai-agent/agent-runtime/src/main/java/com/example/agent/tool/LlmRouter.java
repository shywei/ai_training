package com.example.agent.tool;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * LLM 路由器 — 用快模型判断用户意图，并决定使用哪个模型。
 *
 * 整合了原 ModelRouter（模型选择）和 LlmRouter（意图路由）的功能，
 * 一次 LLM 调用同时返回路由决策和推荐模型。
 *
 * 路由决策：
 * - KNOWLEDGE：需要检索商品知识
 * - TOOL：需要查价格/库存
 * - CHAT：普通聊天
 *
 * 模型选择：
 * - KNOWLEDGE：reasoning 模型（理解用户需求、结合知识推荐）
 * - TOOL：reasoning 模型（理解参数、多步推理）
 * - CHAT：chat 模型（快、便宜）
 */
@Component
public class LlmRouter {

    private final ChatModel chatModel;
    private final String routingModel;
    private final String chatModelName;
    private final String reasoningModelName;
    private static final Logger routeLog = LoggerFactory.getLogger("LLM_ROUTE");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public LlmRouter(ChatModel chatModel,
                     @Value("${agent.model.routing}") String routingModel,
                     @Value("${agent.model.chat}") String chatModelName,
                     @Value("${agent.model.reasoning}") String reasoningModelName) {
        this.chatModel = chatModel;
        this.routingModel = routingModel;
        this.chatModelName = chatModelName;
        this.reasoningModelName = reasoningModelName;
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
     * 路由结果：包含意图和推荐模型
     */
    public record RouteResult(Route route, String model) {}

    /**
     * 判断用户意图并选择模型
     */
    public RouteResult route(String userMessage) {
        String prompt = """
                判断用户消息的意图，只输出一个词：
                - KNOWLEDGE：需要查询商品信息、推荐商品、了解商品配置和功能
                - TOOL：需要查询价格、库存、是否有货、下单、购买
                - CHAT：普通聊天、问候、闲聊、与购物无关的问题
                
                用户消息：%s
                
                只输出 KNOWLEDGE、TOOL 或 CHAT，不要输出其他内容。
                """.formatted(userMessage);

        try {
            routeLog.info("\n╔══════════════════════════════════════════════════╗\n║ ROUTE REQUEST  {} ║\n╚══════════════════════════════════════════════════╝\n[Model] {}\n[Prompt]\n{}", LocalDateTime.now().format(FMT), routingModel, prompt);

            long start = System.currentTimeMillis();

            String result = ChatClient.create(chatModel)
                    .prompt()
                    .user(prompt)
                    .options(OpenAiChatOptions.builder()
                            .withModel(routingModel)
                            .withTemperature(0.0)
                            .build())
                    .call()
                    .content();

            long duration = System.currentTimeMillis() - start;

            Route route;
            if (result == null) {
                route = Route.CHAT;
            } else {
                result = result.trim().toUpperCase();
                if (result.contains("TOOL")) route = Route.TOOL;
                else if (result.contains("KNOWLEDGE")) route = Route.KNOWLEDGE;
                else route = Route.CHAT;
            }

            // 根据路由选择模型：TOOL 需要推理能力，其他用 chat 模型
            String model = selectModel(route);

            routeLog.info("\n╔══════════════════════════════════════════════════╗\n║ ROUTE RESPONSE {} ║\n╚══════════════════════════════════════════════════╝\n[Model] {}\n[Duration] {}ms\n[Result] {}\n[Route] {}\n[Selected Model] {}", LocalDateTime.now().format(FMT), routingModel, duration, result, route, model);

            return new RouteResult(route, model);

        } catch (Exception e) {
            System.err.println("[Router] 路由判断失败，默认 CHAT: " + e.getMessage());
            return new RouteResult(Route.CHAT, chatModelName);
        }
    }

    /**
     * 根据路由选择模型
     *
     * TOOL 路由需要 reasoning 模型（理解参数、多步推理）
     * 其他路由用 chat 模型（快、便宜）
     */
    private String selectModel(Route route) {
        return switch (route) {
            case KNOWLEDGE, TOOL -> reasoningModelName;
            default -> chatModelName;
        };
    }
}
