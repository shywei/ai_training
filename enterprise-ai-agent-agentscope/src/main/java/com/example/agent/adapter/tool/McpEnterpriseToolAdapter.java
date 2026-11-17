package com.example.agent.adapter.tool;

import com.example.agent.domain.ToolOrderRequest;
import com.example.agent.port.EnterpriseToolPort;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

/**
 * 六边形架构出站适配器（Outbound Adapter）：MCP 企业系统与本地回退适配器
 *
 * 连接外部 Product MCP Server (8081) 与 Knowledge MCP Server (8082)。
 * 完美实现与 Spring AI 2.0 统一的 EnterpriseToolPort 端口契约。
 * 若外部服务未启动或连接中断，自动平滑回退至内置 Mock 数据源，
 * 保证 Demo 无论是否启动 Docker 都能 100% 独立稳定运行。
 */
@Component
public class McpEnterpriseToolAdapter implements EnterpriseToolPort {

    private static final Logger log = LoggerFactory.getLogger(McpEnterpriseToolAdapter.class);

    private final String productServerUrl;
    private final String knowledgeServerUrl;
    private final ObjectMapper objectMapper;

    private McpSyncClient productClient;
    private McpSyncClient knowledgeClient;
    private boolean productConnected = false;
    private boolean knowledgeConnected = false;

    // 内置本地商品库（Mock 降级数据）
    private final Map<String, Map<String, Object>> mockProducts = new LinkedHashMap<>();

    public McpEnterpriseToolAdapter(
            @Value("${mcp.product.server-url:http://localhost:8081}") String productServerUrl,
            @Value("${mcp.knowledge.server-url:http://localhost:8082}") String knowledgeServerUrl,
            ObjectMapper objectMapper) {
        this.productServerUrl = productServerUrl;
        this.knowledgeServerUrl = knowledgeServerUrl;
        this.objectMapper = objectMapper;
        initMockData();
    }

    private void initMockData() {
        mockProducts.put("NB001", Map.of(
                "productId", "NB001",
                "name", "联想拯救者 Y9000P 2024专业电竞本",
                "brand", "联想",
                "price", new BigDecimal("8999.00"),
                "stock", 18,
                "specs", "i9-14900HX / RTX4060 / 16G / 1TB SSD",
                "category", "笔记本电脑"
        ));
        mockProducts.put("NB002", Map.of(
                "productId", "NB002",
                "name", "苹果 MacBook Pro 14 M3 Pro",
                "brand", "Apple",
                "price", new BigDecimal("13999.00"),
                "stock", 7,
                "specs", "M3 Pro芯片 / 18G / 512G SSD / Liquid Retina XDR",
                "category", "笔记本电脑"
        ));
        mockProducts.put("NB003", Map.of(
                "productId", "NB003",
                "name", "华为 MateBook X Pro 2024微绒轻薄本",
                "brand", "华为",
                "price", new BigDecimal("7999.00"),
                "stock", 25,
                "specs", "Ultra7 / 980g极致轻薄 / 3.1K OLED / 16G / 1TB",
                "category", "笔记本电脑"
        ));
        mockProducts.put("PH001", Map.of(
                "productId", "PH001",
                "name", "华为 Mate 60 Pro",
                "brand", "华为",
                "price", new BigDecimal("6999.00"),
                "stock", 32,
                "specs", "12GB+512GB 雅川青 卫星通话",
                "category", "智能手机"
        ));
        mockProducts.put("ACC001", Map.of(
                "productId", "ACC001",
                "name", "索尼 WH-1000XM5 无线降噪耳机",
                "brand", "索尼",
                "price", new BigDecimal("2199.00"),
                "stock", 45,
                "specs", "双芯降噪驱动 / 30h续航 / 舒适耳罩",
                "category", "数码配件"
        ));
    }

    @PostConstruct
    public void init() {
        connectProductServer();
        connectKnowledgeServer();
    }

    private void connectProductServer() {
        try {
            log.info("[MCP-Adapter] 尝试连接 Product MCP Server: {}", productServerUrl);
            HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(productServerUrl).build();
            productClient = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(5)).build();
            productClient.initialize();
            productConnected = true;
            log.info("[MCP-Adapter] ✅ 成功连接 Product MCP Server");
        } catch (Exception e) {
            log.warn("[MCP-Adapter] ⚠️ 连接 Product MCP Server 失败 ({})，平滑回退至内置商品库", e.getMessage());
            productConnected = false;
        }
    }

    private void connectKnowledgeServer() {
        try {
            log.info("[MCP-Adapter] 尝试连接 Knowledge MCP Server: {}", knowledgeServerUrl);
            HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(knowledgeServerUrl).build();
            knowledgeClient = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(5)).build();
            knowledgeClient.initialize();
            knowledgeConnected = true;
            log.info("[MCP-Adapter] ✅ 成功连接 Knowledge MCP Server");
        } catch (Exception e) {
            log.warn("[MCP-Adapter] ⚠️ 连接 Knowledge MCP Server 失败 ({})，平滑回退至内置知识库", e.getMessage());
            knowledgeConnected = false;
        }
    }

    @Override
    public Map<String, Object> queryProductInfo(String productId) {
        if (productConnected && productClient != null) {
            try {
                McpSchema.CallToolResult result = productClient.callTool(
                        new McpSchema.CallToolRequest("query_product", Map.of("productId", productId))
                );
                String text = extractText(result);
                if (text != null && !text.isBlank()) {
                    return objectMapper.readValue(text, new TypeReference<Map<String, Object>>() {});
                }
            } catch (Exception e) {
                log.warn("[MCP-Adapter] 远程 MCP 调用失败，降级本地数据: {}", e.getMessage());
            }
        }
        return mockProducts.getOrDefault(productId, Map.of("error", "未找到商品 " + productId));
    }

    @Override
    public BigDecimal queryPrice(String productId) {
        Map<String, Object> info = queryProductInfo(productId);
        if (info.containsKey("price")) {
            return new BigDecimal(info.get("price").toString());
        }
        return new BigDecimal("999.00");
    }

    @Override
    public Integer queryStock(String productId) {
        Map<String, Object> info = queryProductInfo(productId);
        if (info.containsKey("stock")) {
            return ((Number) info.get("stock")).intValue();
        }
        return 10;
    }

    @Override
    public Map<String, Object> createOrder(ToolOrderRequest request) {
        if (productConnected && productClient != null) {
            try {
                Map<String, Object> args = new HashMap<>();
                args.put("userId", request.getUserId());
                args.put("productId", request.getProductId());
                args.put("quantity", request.getQuantity());
                args.put("totalAmount", request.getTotalAmount());
                args.put("shippingAddress", request.getShippingAddress());
                args.put("contactPhone", request.getContactPhone());
                args.put("idempotencyKey", request.getIdempotencyKey());

                McpSchema.CallToolResult result = productClient.callTool(
                        new McpSchema.CallToolRequest("create_order", args)
                );
                String text = extractText(result);
                if (text != null && !text.isBlank()) {
                    return objectMapper.readValue(text, new TypeReference<Map<String, Object>>() {});
                }
            } catch (Exception e) {
                log.warn("[MCP-Adapter] 远程下单调用失败，使用本地模拟成单: {}", e.getMessage());
            }
        }

        String orderId = "ORD-" + System.currentTimeMillis() + "-" + (1000 + new Random().nextInt(9000));
        return Map.of(
                "orderId", orderId,
                "productId", request.getProductId(),
                "quantity", request.getQuantity(),
                "totalAmount", request.getTotalAmount(),
                "status", "CREATED",
                "message", "订单创建成功，配送地址：" + request.getShippingAddress()
        );
    }

    @Override
    public String searchKnowledge(String query) {
        if (knowledgeConnected && knowledgeClient != null) {
            try {
                McpSchema.CallToolResult result = knowledgeClient.callTool(
                        new McpSchema.CallToolRequest("search_knowledge", Map.of("query", query))
                );
                String text = extractText(result);
                if (text != null && !text.isBlank()) {
                    return text;
                }
            } catch (Exception e) {
                log.warn("[MCP-Adapter] 知识库检索失败，降级本地数据: {}", e.getMessage());
            }
        }

        if (query.contains("拯救者") || query.contains("游戏") || query.contains("对比")) {
            return "【知识库切片】拯救者Y9000P搭载满功耗RTX4060显卡与霜刃Pro散热系统，适合3A高帧率游戏与高负载视频渲染；MacBook Pro M3 Pro侧重能效比与色彩管理。";
        }
        if (query.contains("售后") || query.contains("退换") || query.contains("保修")) {
            return "【售后政策】商城支持未拆封7天无理由退货；质量问题15天免费换新，核心硬件享全国联保2年服务。";
        }
        return "【导购指南】商城全场满299元包邮，提供12期免息分期。";
    }

    private String extractText(McpSchema.CallToolResult result) {
        if (result != null && result.content() != null) {
            for (Object obj : result.content()) {
                if (obj instanceof McpSchema.TextContent textContent) {
                    return textContent.text();
                }
            }
        }
        return null;
    }

    @PreDestroy
    public void close() {
        if (productClient != null) {
            try { productClient.closeGracefully(); } catch (Exception ignored) {}
        }
        if (knowledgeClient != null) {
            try { knowledgeClient.closeGracefully(); } catch (Exception ignored) {}
        }
    }
}
