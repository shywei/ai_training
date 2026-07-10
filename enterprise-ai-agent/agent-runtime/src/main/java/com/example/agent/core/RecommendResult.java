package com.example.agent.core;

import java.util.List;

/**
 * 结构化推荐结果 — 演示 Bean Mapping
 * LLM 返回 JSON，Jackson 映射为这个 Java 对象
 */
public class RecommendResult {

    private String recommendation;
    private List<Product> products;
    private double confidence;

    public static class Product {
        private String name;
        private String reason;
        private String price_range;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getPrice_range() { return price_range; }
        public void setPrice_range(String price_range) { this.price_range = price_range; }

        @Override
        public String toString() {
            return String.format("[%s] %s (%s)", name, reason, price_range);
        }
    }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    public List<Product> getProducts() { return products; }
    public void setProducts(List<Product> products) { this.products = products; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
}
