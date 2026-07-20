package com.example.mcp.product;

import io.modelcontextprotocol.sdk.McpServer;
import io.modelcontextprotocol.sdk.McpServerFeatures;
import io.modelcontextprotocol.sdk.McpSchema;
import io.modelcontextprotocol.sdk.server.McpSyncServer;
import io.modelcontextprotocol.sdk.server.transport.HttpServletSseServerTransportProvider;
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
        return new ServletRegistrationBean<>(transportProvider, "/mcp/sse");
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
                            "description": "商品ID，如 NB001, PH001, HP001, TB001"
                        }
                    },
                    "required": ["productId"]
                }
                """;

        // ========== Tool 1: queryProductInfo ==========
        McpSchema.Tool productInfoTool = new McpSchema.Tool(
                "queryProductInfo",
                "查询商品的价格和库存信息，返回商品的完整信息（包括名称、价格、库存、销量、状态）",
                productIdSchema
        );

        McpServerFeatures.SyncToolSpecification productInfoSpec = new McpServerFeatures.SyncToolSpecification(
                productInfoTool,
                (exchange, request) -> {
                    String productId = (String) request.arguments().get("productId");
                    Map<String, Object> product = productRepository.getProductInfo(productId);

                    if (product == null) {
                        return new McpSchema.CallToolResult(
                                List.of(new McpSchema.TextContent("未找到商品ID: " + productId + "，请检查商品ID是否正确。")),
                                true
                        );
                    }

                    String result = String.format(
                            "商品ID: %s\n商品名称: %s\n品牌: %s\n类别: %s\n价格: ¥%s\n库存: %s件\n累计销量: %s件\n状态: %s",
                            product.get("id"), product.get("name"), product.get("brand"),
                            product.get("category"), product.get("price"),
                            product.get("stock"), product.get("sales"), product.get("status")
                    );
                    return new McpSchema.CallToolResult(
                            List.of(new McpSchema.TextContent(result)),
                            false
                    );
                }
        );

        // ========== Tool 2: queryPrice ==========
        McpSchema.Tool priceTool = new McpSchema.Tool(
                "queryPrice",
                "查询商品的价格信息，仅返回商品名称和价格",
                productIdSchema
        );

        McpServerFeatures.SyncToolSpecification priceSpec = new McpServerFeatures.SyncToolSpecification(
                priceTool,
                (exchange, request) -> {
                    String productId = (String) request.arguments().get("productId");
                    Map<String, Object> product = productRepository.getProductPrice(productId);

                    if (product == null) {
                        return new McpSchema.CallToolResult(
                                List.of(new McpSchema.TextContent("未找到商品ID: " + productId)),
                                true
                        );
                    }

                    String result = String.format(
                            "商品ID: %s\n商品名称: %s\n价格: ¥%s\n状态: %s",
                            product.get("id"), product.get("name"),
                            product.get("price"), product.get("status")
                    );
                    return new McpSchema.CallToolResult(
                            List.of(new McpSchema.TextContent(result)),
                            false
                    );
                }
        );

        // ========== Tool 3: queryStock ==========
        McpSchema.Tool stockTool = new McpSchema.Tool(
                "queryStock",
                "查询商品的库存信息，仅返回商品名称、库存数量和状态",
                productIdSchema
        );

        McpServerFeatures.SyncToolSpecification stockSpec = new McpServerFeatures.SyncToolSpecification(
                stockTool,
                (exchange, request) -> {
                    String productId = (String) request.arguments().get("productId");
                    Map<String, Object> product = productRepository.getProductStock(productId);

                    if (product == null) {
                        return new McpSchema.CallToolResult(
                                List.of(new McpSchema.TextContent("未找到商品ID: " + productId)),
                                true
                        );
                    }

                    String result = String.format(
                            "商品ID: %s\n商品名称: %s\n库存: %s件\n状态: %s",
                            product.get("id"), product.get("name"),
                            product.get("stock"), product.get("status")
                    );
                    return new McpSchema.CallToolResult(
                            List.of(new McpSchema.TextContent(result)),
                            false
                    );
                }
        );

        return McpServer.sync(transportProvider)
                .serverInfo("product-server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .tools(productInfoSpec, priceSpec, stockSpec)
                .build();
    }
}
