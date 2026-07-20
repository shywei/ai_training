package com.example.mcp.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * 商品数据访问 — 从 MySQL product 表查询。
 *
 * 替代原来的内存硬编码 ProductData，实现真正的数据库查询。
 * 对应 schema.sql 中的 product 表。
 */
@Repository
public class ProductRepository {

    private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public ProductRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询商品完整信息（价格 + 库存 + 状态 + 销量）
     */
    public Map<String, Object> getProductInfo(String productId) {
        try {
            var results = jdbcTemplate.queryForList(
                    "SELECT id, name, brand, category, price, stock, status, sales FROM product WHERE id = ?",
                    productId.toUpperCase());
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            log.error("查询商品失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 查询商品价格
     */
    public Map<String, Object> getProductPrice(String productId) {
        try {
            var results = jdbcTemplate.queryForList(
                    "SELECT id, name, price, status FROM product WHERE id = ?",
                    productId.toUpperCase());
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            log.error("查询商品价格失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 查询商品库存
     */
    public Map<String, Object> getProductStock(String productId) {
        try {
            var results = jdbcTemplate.queryForList(
                    "SELECT id, name, stock, status FROM product WHERE id = ?",
                    productId.toUpperCase());
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            log.error("查询商品库存失败: {}", e.getMessage());
            return null;
        }
    }
}
