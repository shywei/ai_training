package com.example.mcp.product;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * MCP Server 配置类
 * 配置 SSE 传输层，注册 3 个商品查询 Tool
 * 数据来自 MySQL product 表（通过 ProductRepository）
 */
@Configuration
public class McpServerConfig {

    @Bean
    public HttpServletSseServerTransportProvider servletSseServerTransportProvider() {
        return HttpServletSseServerTransportProvider.builder()
                .messageEndpoint("/mcp/message")
                .build();
    }

    @Bean
    public ServletRegistrationBean<?> customServletBean(
            HttpServletSseServerTransportProvider transportProvider) {
        return new ServletRegistrationBean<>(transportProvider, "/*");
    }

    @Bean
    public McpSyncServer mcpSyncServer(HttpServletSseServerTransportProvider transportProvider,
                                        ProductRepository productRepository) {

        String productIdSchema = """
                {
                    "type": "object",
                    "properties": {
                        "productId": {
                            "type": "string",
                            "description": "单个商品ID（格式：大写字母+数字，如NB001）。从用户消息中提取对应商品的ID，只传一个。"
                        }
                    },
                    "required": ["productId"]
                }
                """;

        // ========== Tool 1: queryProductInfo ==========
        McpSchema.Tool productInfoTool = McpSchema.Tool.builder()
                .name("queryProductInfo")
                .description("查询商品的价格和库存信息。传入单个商品ID，返回该商品的完整信息。")
                .inputSchema(jsonSchema(productIdSchema))
                .build();

        McpServerFeatures.SyncToolSpecification productInfoSpec = new McpServerFeatures.SyncToolSpecification(
                productInfoTool,
                (exchange, request) -> {
                    String productId = (String) request.arguments().get("productId");
                    Map<String, Object> product = productRepository.getProductInfo(productId);

                    if (product == null) {
                        return McpSchema.CallToolResult.builder()
                                .addTextContent("未找到商品ID: " + productId + "，请检查商品ID是否正确。")
                                .isError(true)
                                .build();
                    }

                    String result = String.format(
                            "商品ID: %s\n商品名称: %s\n品牌: %s\n类别: %s\n价格: ¥%s\n库存: %s件\n累计销量: %s件\n状态: %s",
                            product.get("id"), product.get("name"), product.get("brand"),
                            product.get("category"), product.get("price"),
                            product.get("stock"), product.get("sales"), product.get("status")
                    );
                    return McpSchema.CallToolResult.builder()
                            .addTextContent(result)
                            .isError(false)
                            .build();
                }
        );

        // ========== Tool 2: queryPrice ==========
        McpSchema.Tool priceTool = McpSchema.Tool.builder()
                .name("queryPrice")
                .description("查询商品的价格信息。传入单个商品ID，返回该商品名称和价格。")
                .inputSchema(jsonSchema(productIdSchema))
                .build();

        McpServerFeatures.SyncToolSpecification priceSpec = new McpServerFeatures.SyncToolSpecification(
                priceTool,
                (exchange, request) -> {
                    String productId = (String) request.arguments().get("productId");
                    Map<String, Object> product = productRepository.getProductPrice(productId);

                    if (product == null) {
                        return McpSchema.CallToolResult.builder()
                                .addTextContent("未找到商品ID: " + productId)
                                .isError(true)
                                .build();
                    }

                    String result = String.format(
                            "商品ID: %s\n商品名称: %s\n价格: ¥%s\n状态: %s",
                            product.get("id"), product.get("name"),
                            product.get("price"), product.get("status")
                    );
                    return McpSchema.CallToolResult.builder()
                            .addTextContent(result)
                            .isError(false)
                            .build();
                }
        );

        // ========== Tool 3: queryStock ==========
        McpSchema.Tool stockTool = McpSchema.Tool.builder()
                .name("queryStock")
                .description("查询商品的库存信息。传入单个商品ID，返回该商品库存和状态。")
                .inputSchema(jsonSchema(productIdSchema))
                .build();

        McpServerFeatures.SyncToolSpecification stockSpec = new McpServerFeatures.SyncToolSpecification(
                stockTool,
                (exchange, request) -> {
                    String productId = (String) request.arguments().get("productId");
                    Map<String, Object> product = productRepository.getProductStock(productId);

                    if (product == null) {
                        return McpSchema.CallToolResult.builder()
                                .addTextContent("未找到商品ID: " + productId)
                                .isError(true)
                                .build();
                    }

                    String result = String.format(
                            "商品ID: %s\n商品名称: %s\n库存: %s件\n状态: %s",
                            product.get("id"), product.get("name"),
                            product.get("stock"), product.get("status")
                    );
                    return McpSchema.CallToolResult.builder()
                            .addTextContent(result)
                            .isError(false)
                            .build();
                }
        );

        return McpServer.sync(transportProvider)
                .serverInfo("product-server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .tools(productInfoSpec, priceSpec, stockSpec)
                .build();
    }

    private McpSchema.JsonSchema jsonSchema(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> map = mapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            String type = (String) map.getOrDefault("type", "object");
            Map<String, Object> properties = (Map<String, Object>) map.getOrDefault("properties", Map.of());
            List<String> required = (List<String>) map.getOrDefault("required", List.of());
            return new McpSchema.JsonSchema(type, properties, required, null, null, null);
        } catch (Exception e) {
            throw new RuntimeException("JSON Schema 解析失败", e);
        }
    }
}
