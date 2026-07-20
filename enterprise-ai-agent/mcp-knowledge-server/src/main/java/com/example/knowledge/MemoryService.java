package com.example.knowledge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 用户记忆服务 — 存储和语义检索用户记忆。
 *
 * 操作 pgvector 的 user_memory 表：
 * - search()：按用户 + 语义相似度检索相关记忆
 * - save()：将记忆 Embedding 后存入 pgvector
 */
@Service
public class MemoryService {

    private static final Logger log = LoggerFactory.getLogger(MemoryService.class);

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;
    private final int topK;
    private final double similarityThreshold;

    public MemoryService(
            JdbcTemplate jdbcTemplate,
            EmbeddingService embeddingService,
            @Value("${retrieval.top-k:3}") int topK,
            @Value("${retrieval.similarity-threshold:0.3}") double similarityThreshold) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingService = embeddingService;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
    }

    /**
     * 语义检索用户记忆。
     *
     * @param userId 用户 ID
     * @param query  查询文本
     * @return 格式化的记忆文本，未找到返回 null
     */
    public String search(String userId, String query) {
        try {
            float[] queryVector = embeddingService.embed(query);
            String vectorLiteral = EmbeddingService.toPgVectorLiteral(queryVector);

            String sql = """
                    SELECT memory_type, content, 1 - (embedding <=> ?::vector) AS similarity
                    FROM user_memory
                    WHERE user_id = ?
                      AND 1 - (embedding <=> ?::vector) > ?
                    ORDER BY similarity DESC
                    LIMIT ?
                    """;

            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                    sql, vectorLiteral, userId, vectorLiteral, similarityThreshold, topK);

            if (results.isEmpty()) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> row : results) {
                String type = (String) row.get("memory_type");
                String content = (String) row.get("content");
                sb.append("[").append(type).append("] ").append(content).append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("记忆检索失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 存储一条用户记忆。
     *
     * @param userId     用户 ID
     * @param memoryType 记忆类型：preference / conversation / event
     * @param content    记忆内容
     * @param metadata   元数据（可为 null）
     */
    public void save(String userId, String memoryType, String content, String metadata) {
        try {
            float[] embedding = embeddingService.embed(content);
            String vectorLiteral = EmbeddingService.toPgVectorLiteral(embedding);

            jdbcTemplate.update(
                    "INSERT INTO user_memory (user_id, memory_type, content, metadata, embedding) VALUES (?, ?, ?, ?::jsonb, ?::vector)",
                    userId, memoryType, content, metadata, vectorLiteral);

            log.info("已存储用户记忆: {} / {} / {}", userId, memoryType,
                    content.length() > 40 ? content.substring(0, 40) + "..." : content);
        } catch (Exception e) {
            log.error("记忆存储失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 存储一条用户记忆（无元数据）。
     */
    public void save(String userId, String memoryType, String content) {
        save(userId, memoryType, content, null);
    }
}
