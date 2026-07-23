package com.example.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 知识库索引器，启动时自动加载 products.json 并索引到 pgvector。
 * 如果 vector_store 已有数据则跳过（保护生产数据）。
 */
@Component
public class KnowledgeIndexer {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIndexer.class);

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    public KnowledgeIndexer(JdbcTemplate jdbcTemplate, EmbeddingService embeddingService) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingService = embeddingService;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void autoIndex() {
        index(false);
    }

    /**
     * 索引商品知识库。
     *
     * @param force true 时清空后重新索引，false 时已有数据则跳过
     * @return 索引结果描述
     */
    public String index(boolean force) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM vector_store", Integer.class);
            if (count != null && count > 0 && !force) {
                String msg = "vector_store 已有 " + count + " 条数据，跳过索引";
                log.info(msg);
                return msg;
            }

            if (force && count != null && count > 0) {
                jdbcTemplate.update("DELETE FROM vector_store");
                log.info("force=true，已清空旧数据 ({} 条)", count);
            }

            log.info("开始索引商品知识库...");

            // 读取 products.json
            ClassPathResource resource = new ClassPathResource("products.json");
            try (InputStream is = resource.getInputStream()) {
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                List<Map<String, Object>> products = objectMapper.readValue(
                        json, new TypeReference<>() {});

                for (Map<String, Object> product : products) {
                    String content = buildContent(product);
                    float[] embedding = embeddingService.embed(content);
                    String vectorLiteral = EmbeddingService.toPgVectorLiteral(embedding);

                    String id = (String) product.getOrDefault("id", UUID.randomUUID().toString());
                    String metadata = objectMapper.writeValueAsString(product);

                    jdbcTemplate.update(
                            "INSERT INTO vector_store (id, content, metadata, embedding) VALUES (?, ?, ?::jsonb, ?::vector)",
                            id, content, metadata, vectorLiteral);

                    log.info("已索引: {} - {}", id, product.get("name"));
                }

                log.info("商品知识库索引完成，共 {} 条", products.size());
                return "索引完成，共 " + products.size() + " 条商品";
            }
        } catch (Exception e) {
            log.error("知识库索引失败: {}", e.getMessage(), e);
            return "索引失败: " + e.getMessage();
        }
    }

    /**
     * 构建商品的文本内容，用于 Embedding 和检索结果展示。
     */
    @SuppressWarnings("unchecked")
    private String buildContent(Map<String, Object> product) {
        StringBuilder sb = new StringBuilder();

        sb.append("商品ID：").append(product.get("id")).append("\n");
        sb.append("商品名称：").append(product.get("name")).append("\n");
        sb.append("品牌：").append(product.get("brand")).append("\n");
        sb.append("类别：").append(product.get("category")).append("\n");
        sb.append("价格：").append(product.get("price")).append("元\n");

        // 配置信息
        Object specsObj = product.get("specs");
        if (specsObj instanceof Map) {
            Map<String, Object> specs = (Map<String, Object>) specsObj;
            sb.append("配置：");
            specs.forEach((k, v) -> sb.append(k).append("=").append(v).append(", "));
            // 移除末尾多余的 ", "
            if (!specs.isEmpty()) {
                sb.setLength(sb.length() - 2);
            }
            sb.append("\n");
        }

        // 亮点
        Object highlightsObj = product.get("highlights");
        if (highlightsObj instanceof List) {
            List<String> highlights = (List<String>) highlightsObj;
            sb.append("亮点：").append(String.join("、", highlights)).append("\n");
        }

        // 适用场景
        Object suitableObj = product.get("suitable_for");
        if (suitableObj instanceof List) {
            List<String> suitable = (List<String>) suitableObj;
            sb.append("适用场景：").append(String.join("、", suitable)).append("\n");
        }

        // 不适合
        Object notSuitableObj = product.get("not_suitable_for");
        if (notSuitableObj instanceof List) {
            List<String> notSuitable = (List<String>) notSuitableObj;
            sb.append("不太适合：").append(String.join("、", notSuitable)).append("\n");
        }

        // 摘要
        Object summaryObj = product.get("summary");
        if (summaryObj != null) {
            sb.append("总结：").append(summaryObj).append("\n");
        }

        return sb.toString();
    }
}
