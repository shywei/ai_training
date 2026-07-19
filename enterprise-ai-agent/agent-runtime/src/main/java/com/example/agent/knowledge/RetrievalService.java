package com.example.agent.knowledge;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 检索服务 - 从知识库中检索相关商品
 *
 * v3.0(Knowledge):关键词过滤 + 语义检索,返回商品描述
 * v4.0(Tool):LLM 通过 Tool 调用查价格、库存等实时数据
 *
 * 为什么这里不查 MySQL:
 * - 查价格/库存是"做事",属于 Tool 的范畴
 * - 第六课只解决"懂"(知识检索),第七课解决"做"(Tool 调用)
 * - 保持每课能力边界清晰
 *
 * Demo 的检索策略:
 * - 关键词过滤:简单判断是否需要检索(避免无关对话浪费 token)
 * - 单路向量召回:pgvector cosine similarity
 * - 第七课会用 LLM 路由替代关键词过滤
 */
@Service
public class RetrievalService {

    private final PgVectorStore vectorStore;

    private static final int DEFAULT_TOP_K = 3;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.3;

    /** 商品相关关键词 - 简单过滤,第七课会用 LLM 路由替代 */
    private static final String[] PRODUCT_KEYWORDS = {
            "推荐", "买", "选购", "什么", "哪个", "哪款",
            "笔记本", "电脑", "手机", "耳机", "平板", "平板电脑",
            "游戏本", "轻薄本", "商务本", "降噪", "拍照",
            "价格", "多少钱", "预算", "便宜", "贵", "性价比",
            "办公", "游戏", "开发", "编程", "剪辑", "设计", "学习",
            "联想", "苹果", "华为", "小米", "戴尔", "华硕", "惠普", "索尼", "三星", "OPPO", "vivo",
            "ThinkPad", "MacBook", "iPad", "iPhone", "AirPods", "ROG",
            "配置", "性能", "续航", "屏幕", "轻薄", "散热"
    };

    public RetrievalService(PgVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 检索与用户问题相关的商品
     *
     * v4.0 变化：移除关键词过滤，由 LLM 路由决定是否调用检索。
     * 路由判断为 KNOWLEDGE 意图时才调用此方法。
     */
    public String retrieve(String query) {
        return retrieve(query, DEFAULT_TOP_K);
    }

    public String retrieve(String query, int topK) {
        try {
            SearchRequest searchRequest = SearchRequest.query(query)
                    .withTopK(topK)
                    .withSimilarityThreshold(DEFAULT_SIMILARITY_THRESHOLD);

            List<Document> results = vectorStore.similaritySearch(searchRequest);

            if (results == null || results.isEmpty()) {
                return null;
            }

            return formatResults(results);

        } catch (Exception e) {
            System.err.println("[Knowledge] 检索失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 按类别过滤检索(Metadata 过滤演示)
     */
    public String retrieveByCategory(String query, String category) {
        try {
            SearchRequest searchRequest = SearchRequest.query(query)
                    .withTopK(DEFAULT_TOP_K)
                    .withSimilarityThreshold(DEFAULT_SIMILARITY_THRESHOLD)
                    .withFilterExpression("category == '" + category + "'");

            List<Document> results = vectorStore.similaritySearch(searchRequest);

            if (results == null || results.isEmpty()) {
                return null;
            }

            return formatResults(results);

        } catch (Exception e) {
            System.err.println("[Knowledge] 按类别检索失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 判断用户问题是否和商品相关
     *
     * 简单的关键词匹配,第七课会用 LLM 路由替代。
     * 命中任一关键词就认为需要检索。
     */
    private boolean isProductRelated(String query) {
        if (query == null || query.isBlank()) return false;
        String lower = query.toLowerCase();
        for (String keyword : PRODUCT_KEYWORDS) {
            if (lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 格式化检索结果
     *
     * 只包含描述性信息,不包含价格/库存(v4.0 Tool 负责)
     */
    private String formatResults(List<Document> documents) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            String name = (String) doc.getMetadata().get("name");
            String brand = (String) doc.getMetadata().get("brand");
            String category = (String) doc.getMetadata().get("category");

            sb.append(String.format("[%d] %s(%s)- %s\n", i + 1, name, brand, category));
            sb.append(doc.getContent());
            sb.append("\n\n");
        }

        return sb.toString().trim();
    }
}
