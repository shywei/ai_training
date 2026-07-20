package com.example.knowledge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 向量检索服务，从 pgvector 中检索相关商品信息。
 */
@Service
public class VectorSearchService {

    private static final Logger log = LoggerFactory.getLogger(VectorSearchService.class);

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;
    private final int topK;
    private final double similarityThreshold;

    public VectorSearchService(
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
     * 将 query Embedding 后在 pgvector 中做余弦相似度搜索。
     *
     * @param query 用户查询文本
     * @return 格式化的商品信息文本
     */
    public String search(String query) {
        try {
            float[] queryVector = embeddingService.embed(query);
            String vectorLiteral = EmbeddingService.toPgVectorLiteral(queryVector);

            String sql = """
                    SELECT content, 1 - (embedding <=> ?::vector) AS similarity
                    FROM vector_store
                    WHERE 1 - (embedding <=> ?::vector) > ?
                    ORDER BY similarity DESC
                    LIMIT ?
                    """;

            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                    sql, vectorLiteral, vectorLiteral, similarityThreshold, topK);

            if (results.isEmpty()) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("以下是与「").append(query).append("」相关的商品信息：\n\n");

            for (int i = 0; i < results.size(); i++) {
                String content = (String) results.get(i).get("content");
                Object similarityObj = results.get(i).get("similarity");
                double similarity = similarityObj instanceof Number
                        ? ((Number) similarityObj).doubleValue() : 0;

                sb.append("【").append(i + 1).append("】");
                sb.append("（相关度 ").append(String.format("%.0f", similarity * 100)).append("%）\n");
                sb.append(content).append("\n\n");
            }

            return sb.toString().trim();
        } catch (Exception e) {
            log.error("向量检索失败: {}", e.getMessage(), e);
            return "检索出错: " + e.getMessage();
        }
    }
}
