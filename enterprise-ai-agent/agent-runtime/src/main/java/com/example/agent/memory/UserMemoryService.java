package com.example.agent.memory;

import com.example.agent.tool.McpKnowledgeClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用户记忆服务 — 通过 MCP 协议存储和检索用户记忆。
 *
 * 第七课 MCP 化后的定位：
 * 所有记忆读写统一通过 McpKnowledgeClient → MCP 协议 → mcp-knowledge-server。
 * 不再直接访问 pgvector。
 *
 * 职责：
 * - storeConversationSummary()：用 LLM 从对话中提取关键信息，通过 MCP 存为记忆
 * - storePreference()：用户偏好通过 MCP 存储
 * - search()：通过 MCP 语义检索用户记忆
 *
 * 调用方：
 * - UserProfileExtractor（后台画像提取 → 存储 preference）
 * - ShoppingAgentController（对话中检测到"记住"指令）
 */
@Service
public class UserMemoryService {

    private final McpKnowledgeClient mcpKnowledgeClient;
    private final ChatModel chatModel;
    private final String summaryModel;
    private static final Logger log = LoggerFactory.getLogger("LLM_MEMORY");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public UserMemoryService(McpKnowledgeClient mcpKnowledgeClient,
                              ChatModel chatModel,
                              @Value("${agent.model.summary}") String summaryModel) {
        this.mcpKnowledgeClient = mcpKnowledgeClient;
        this.chatModel = chatModel;
        this.summaryModel = summaryModel;
    }

    /**
     * 存储一条用户记忆（通过 MCP 协议）
     */
    public void store(String userId, String memoryType, String content) {
        mcpKnowledgeClient.saveMemory(userId, memoryType, content);
    }

    /**
     * 语义检索用户记忆（通过 MCP 协议）
     */
    public String search(String userId, String query) {
        return mcpKnowledgeClient.searchMemory(userId, query);
    }

    /**
     * 用 LLM 从对话中提取关键信息，通过 MCP 存为记忆
     */
    public void storeConversationSummary(String userId, String sessionId, List<String> messages) {
        if (messages == null || messages.isEmpty() || userId == null || userId.isBlank()) {
            return;
        }

        // 拼接对话内容
        StringBuilder conversation = new StringBuilder();
        for (String msg : messages) {
            String[] parts = msg.split("\\|", 2);
            if (parts.length == 2) {
                if (parts[1].startsWith("[对话摘要]")) continue;
                String role = "user".equals(parts[0]) ? "用户" : "助手";
                conversation.append(role).append(": ").append(parts[1]).append("\n");
            }
        }

        if (conversation.length() < 10) return;

        // 用 LLM 提取关键信息
        String summary = extractSummary(conversation.toString());

        if (summary == null || summary.isBlank()) {
            log.info("[Memory] LLM 摘要为空，跳过存储");
            return;
        }

        // 通过 MCP 存储
        store(userId, "conversation", summary);
    }

    /**
     * 存储用户偏好记忆（通过 MCP 协议）
     */
    public void storePreference(String userId, String preference) {
        store(userId, "preference", preference);
    }

    /**
     * 调用 LLM 从对话中提取关键信息
     */
    private String extractSummary(String conversation) {
        String prompt = """
                请从以下对话中提取关键信息，用简洁的自然语言总结。

                要求：
                - 提取用户的偏好、需求、决策
                - 提取关键事实（提到的商品、价格、配置等）
                - 忽略寒暄和无关内容
                - 不超过200字
                - 用中文

                对话内容：
                %s
                """.formatted(conversation);

        log.info("\n╔══════════════════════════════════════════════════╗\n║ MEMORY EXTRACT REQUEST {} ║\n╚══════════════════════════════════════════════════╝\n[Model] {}\n\n[Prompt]\n{}", LocalDateTime.now().format(FMT), summaryModel, prompt);

        long start = System.currentTimeMillis();

        try {
            String result = ChatClient.create(chatModel)
                    .prompt()
                    .user(prompt)
                    .options(OpenAiChatOptions.builder()
                            .withModel(summaryModel)
                            .withTemperature(0.3)
                            .build())
                    .call()
                    .content();

            long duration = System.currentTimeMillis() - start;

            log.info("\n╔══════════════════════════════════════════════════╗\n║ MEMORY EXTRACT RESPONSE {} ║\n╚══════════════════════════════════════════════════╝\n[Model] {}\n[Duration] {}ms\n\n[Summary]\n{}", LocalDateTime.now().format(FMT), summaryModel, duration, result);

            return result;

        } catch (Exception e) {
            log.error("[Memory] LLM 摘要提取失败: {}", e.getMessage());
            return null;
        }
    }
}
