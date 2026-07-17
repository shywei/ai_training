package com.example.agent.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.*;

/**
 * 知识库管理 API
 *
 * 第六课核心教学点：
 * - Embedding 是一个显式动作，不是自动发生的
 * - 索引需要管理：查看状态、手动触发、按需更新
 * - 企业中不能盲目覆盖生产数据
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final PgVectorStore vectorStore;
    private final JdbcTemplate pgvectorJdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KnowledgeController(PgVectorStore vectorStore, JdbcTemplate pgvectorJdbcTemplate) {
        this.vectorStore = vectorStore;
        this.pgvectorJdbcTemplate = pgvectorJdbcTemplate;
    }

    /**
     * 查看当前索引状态
     * GET /api/knowledge/status
     */
    @GetMapping("/status")
    public Map<String, Object> indexStatus() {
        // 检查向量库中是否已有数据
        try {
            SearchRequest request = SearchRequest.query("笔记本")
                    .withTopK(1)
                    .withSimilarityThreshold(0.0);
            List<Document> results = vectorStore.similaritySearch(request);
            int count = results != null ? results.size() : 0;

            // 用一个总是能匹配的查询来估算总数
            return Map.of(
                    "indexed", count > 0,
                    "message", count > 0 ? "知识库已索引，可发起检索" : "知识库为空，请先执行索引"
            );
        } catch (Exception e) {
            return Map.of(
                    "indexed", false,
                    "message", "检查失败: " + e.getMessage()
            );
        }
    }

    /**
     * 执行索引
     * POST /api/knowledge/index?force=false
     *
     * - 默认 force=false：如果已索引，跳过（保护生产数据）
     * - force=true：清空后重新索引（慎用）
     */
    @PostMapping("/index")
    public Map<String, Object> indexProducts(@RequestParam(defaultValue = "false") boolean force) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 检查是否已索引
        try {
            SearchRequest checkRequest = SearchRequest.query("商品")
                    .withTopK(1)
                    .withSimilarityThreshold(0.0);
            List<Document> existing = vectorStore.similaritySearch(checkRequest);

            if (existing != null && !existing.isEmpty() && !force) {
                result.put("status", "skipped");
                result.put("message", "知识库已有数据，跳过索引。如需重新索引，请加参数 ?force=true");
                return result;
            }
        } catch (Exception e) {
            // 检查失败，继续尝试索引
        }

        // 加载商品数据
        List<Document> documents;
        try {
            documents = loadProductDocuments();
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "读取 products.json 失败: " + e.getMessage());
            return result;
        }

        if (documents.isEmpty()) {
            result.put("status", "error");
            result.put("message", "没有找到商品数据");
            return result;
        }

        // force=true 时先清空旧数据（直接 SQL，不用 PgVectorStore 的 delete）
        if (force) {
            try {
                int deleted = pgvectorJdbcTemplate.update("DELETE FROM vector_store");
                System.out.println("[Knowledge] 已清空旧数据 (" + deleted + " 条)");
            } catch (Exception e) {
                System.err.println("[Knowledge] 清空旧数据失败: " + e.getMessage());
            }
        }

        // 执行 Embedding + 存入 pgvector
        long start = System.currentTimeMillis();
        try {
            System.out.println("[Knowledge] 开始索引 " + documents.size() + " 个商品...");

            // 逐条处理，展示 Embedding 过程
            for (int i = 0; i < documents.size(); i++) {
                Document doc = documents.get(i);
                String name = (String) doc.getMetadata().get("name");
                System.out.println("[Knowledge] [" + (i + 1) + "/" + documents.size() + "] Embedding: " + name);
            }

            // 批量写入向量库
            vectorStore.add(documents);

            long duration = System.currentTimeMillis() - start;
            System.out.println("[Knowledge] ✅ 索引完成，共 " + documents.size() + " 个商品，耗时 " + duration + "ms");

            result.put("status", "success");
            result.put("count", documents.size());
            result.put("duration_ms", duration);
            result.put("message", "索引完成。共 " + documents.size() + " 个商品，耗时 " + duration + "ms");

        } catch (Exception e) {
            System.err.println("[Knowledge] ⚠️ 索引失败: " + e.getMessage());
            result.put("status", "error");
            result.put("message", "索引失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 从 products.json 加载商品文档
     */
    private List<Document> loadProductDocuments() throws Exception {
        List<Document> documents = new ArrayList<>();

        try (InputStream is = new ClassPathResource("products.json").getInputStream()) {
            JsonNode products = objectMapper.readTree(is);

            for (JsonNode product : products) {
                String id = product.path("id").asText();
                String name = product.path("name").asText();
                String brand = product.path("brand").asText();
                String category = product.path("category").asText();
                String summary = product.path("summary").asText();

                // 构建文本内容（只含描述，不含价格/库存）
                StringBuilder content = new StringBuilder();
                content.append(name).append("（").append(brand).append("）\n");
                content.append("类别：").append(category).append("\n");

                JsonNode specs = product.path("specs");
                if (!specs.isMissingNode()) {
                    content.append("配置：");
                    specs.fields().forEachRemaining(entry ->
                            content.append(entry.getKey()).append("=").append(entry.getValue().asText()).append(", "));
                    content.append("\n");
                }

                JsonNode highlights = product.path("highlights");
                if (highlights.isArray()) {
                    content.append("亮点：");
                    for (JsonNode h : highlights) content.append(h.asText()).append("、");
                    content.append("\n");
                }

                JsonNode suitable = product.path("suitable_for");
                if (suitable.isArray()) {
                    content.append("适合：");
                    for (JsonNode s : suitable) content.append(s.asText()).append("、");
                    content.append("\n");
                }

                JsonNode notSuitable = product.path("not_suitable_for");
                if (notSuitable.isArray()) {
                    content.append("不适合：");
                    for (JsonNode s : notSuitable) content.append(s.asText()).append("、");
                    content.append("\n");
                }

                content.append("总结：").append(summary);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("doc_id", id);
                metadata.put("name", name);
                metadata.put("brand", brand);
                metadata.put("category", category);

                documents.add(new Document(content.toString(), metadata));
            }
        }

        return documents;
    }
}
