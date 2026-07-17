package com.example.agent.memory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用户记忆服务 — 存储和语义检索用户记忆
 *
 * 职责分工：
 * - 本类（UserMemoryService）：LLM 摘要 → pgvector（语义记忆）
 * - UserProfileExtractor：LLM 提取的结构化标签 → MySQL（画像）
 *
 * 两者不重叠：
 * - MySQL 存「提炼后的结论」（用户是谁）
 * - pgvector 存「对话的关键记忆」（用户说过什么、偏好什么）
 *
 * 核心能力：
 * - store()：将记忆内容 Embedding 后存入 pgvector
 * - search()：按用户 + 语义相似度检索相关记忆
 * - storeConversationSummary()：用 LLM 从对话中提取关键信息，存为记忆
 * - storePreference()：用户主动要求记住时存储偏好
 */
@Service
public class UserMemoryService {

    private final UserMemoryMapper memoryMapper;
    private final EmbeddingModel embeddingModel;
    private final ChatModel chatModel;
    private final String summaryModel;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger("LLM_MEMORY");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int DEFAULT_SEARCH_LIMIT = 5;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.3;

    public UserMemoryService(UserMemoryMapper memoryMapper,
                              EmbeddingModel embeddingModel,
                              ChatModel chatModel,
                              @Value("${agent.model.summary}") String summaryModel) {
        this.memoryMapper = memoryMapper;
        this.embeddingModel = embeddingModel;
        this.chatModel = chatModel;
        this.summaryModel = summaryModel;
    }

    /**
     * 存储一条用户记忆
     */
    public void store(String userId, String memoryType, String content, Map<String, Object> metadata) {
        try {
            float[] embedding = embeddingModel.embed(content);

            UserMemory memory = new UserMemory(userId, memoryType, content);
            memory.setEmbedding(embedding);
            if (metadata != null && !metadata.isEmpty()) {
                memory.setMetadataJson(objectMapper.writeValueAsString(metadata));
            }

            memoryMapper.save(memory);
            System.out.println("[Memory] 存储用户记忆: " + userId + " / " + memoryType + " / " +
                    (content.length() > 30 ? content.substring(0, 30) + "..." : content));

        } catch (Exception e) {
            System.err.println("[Memory] 存储记忆失败: " + e.getMessage());
        }
    }

    public void store(String userId, String memoryType, String content) {
        store(userId, memoryType, content, null);
    }

    /**
     * 语义检索用户记忆
     */
    public String search(String userId, String query, int limit) {
        try {
            float[] queryEmbedding = embeddingModel.embed(query);
            List<UserMemory> results = memoryMapper.searchBySimilarity(
                    userId, queryEmbedding, DEFAULT_SIMILARITY_THRESHOLD, limit);

            if (results == null || results.isEmpty()) {
                return null;
            }

            return formatMemories(results);

        } catch (Exception e) {
            System.err.println("[Memory] 记忆检索失败: " + e.getMessage());
            return null;
        }
    }

    public String search(String userId, String query) {
        return search(userId, query, DEFAULT_SEARCH_LIMIT);
    }

    /**
     * 用 LLM 从对话中提取关键信息，存为记忆
     *
     * 和之前的区别：
     * - 之前：截取原文前500字（信息丢失严重）
     * - 现在：LLM 提取关键事实和偏好（结构化摘要）
     */
    public void storeConversationSummary(String userId, String sessionId, List<String> messages) {
        if (messages == null || messages.isEmpty() || userId == null || userId.isBlank()) {
            return;
        }

        try {
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
                System.out.println("[Memory] LLM 摘要为空，跳过存储");
                return;
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("sessionId", sessionId);
            metadata.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            metadata.put("messageCount", messages.size());

            store(userId, "conversation", summary, metadata);

        } catch (Exception e) {
            System.err.println("[Memory] 对话摘要沉淀失败: " + e.getMessage());
        }
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

    /**
     * 存储用户偏好记忆（用户主动要求记住时调用）
     */
    public void storePreference(String userId, String preference) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "profile_extractor");
        metadata.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        store(userId, "preference", preference, metadata);
    }

    /**
     * 格式化记忆列表为 Prompt 可用的文本
     */
    private String formatMemories(List<UserMemory> memories) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < memories.size(); i++) {
            UserMemory mem = memories.get(i);
            sb.append(String.format("[%s] %s", mem.getMemoryType(), mem.getContent()));
            sb.append("\n");
        }
        return sb.toString().trim();
    }
}
