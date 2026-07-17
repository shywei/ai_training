package com.example.agent.knowledge;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 商品结构化信息 — MySQL 存储
 *
 * 与 document_chunk（pgvector）的区别：
 * - document_chunk：商品描述、亮点、适用场景 → 语义检索
 * - product：价格、库存、销量、状态 → 精确查询、实时更新
 *
 * 企业级设计原则：
 * 频繁变化的业务数据（价格、库存）不适合放向量数据库，
 * 因为向量数据库的价值在于语义检索，不在于实时事务。
 */
@TableName("product")
public class ProductInfo {

    @TableId(type = IdType.INPUT)
    private String id;
    private String name;
    private String brand;
    private String category;
    private Integer price;
    private Integer stock;
    private String status;    // 在售 / 缺货 / 下架
    private Integer sales;

    public ProductInfo() {}

    public ProductInfo(String id, String name, String brand, String category,
                       Integer price, Integer stock, String status, Integer sales) {
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.status = status;
        this.sales = sales;
    }

    /**
     * 格式化为 Prompt 可用的文本
     * 只包含实时业务数据，不包含描述信息（描述在向量库里）
     */
    public String toContextString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("（").append(brand).append("）");
        sb.append(" — ").append(price).append("元");
        if ("缺货".equals(status)) {
            sb.append(" [缺货]");
        } else if ("下架".equals(status)) {
            sb.append(" [已下架]");
        } else {
            sb.append(" [有货，库存").append(stock).append("件]");
        }
        sb.append("，累计销量").append(sales).append("件");
        return sb.toString();
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getSales() { return sales; }
    public void setSales(Integer sales) { this.sales = sales; }
}
