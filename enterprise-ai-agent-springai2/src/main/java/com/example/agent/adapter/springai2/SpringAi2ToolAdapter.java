package com.example.agent.adapter.springai2;

import com.example.agent.domain.ShoppingDomainService;
import com.example.agent.domain.ToolOrderRequest;
import com.example.agent.port.EnterpriseToolPort;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.*;

/**
 * Spring AI 2.0.2 GA 统一工具适配器（实现 EnterpriseToolPort 端口）
 *
 * 对应第 09 课 P05（Spring AI 2.0 官方能力演进 · 统一 Tool 抽象与内置 MCP）与 P15~P18：
 *
 * 【对照第 07 课 / v5.0 手搓工具体系】：
 * 在 1.0-M4 手搓版本中：
 * - 必须手写 McpToolCallback (80 行)，手动把 MCP 架构的 inputSchema 转换为 Spring AI FunctionCallback；
 * - 在 AgentRuntime (651 行) 中手写正则 extractToolCall() 提取 `TOOL_CALL:工具名(参数)`；
 * - 手动管理工具序列化、异常包装与调用分支。
 *
 * 【Spring AI 2.0.2 GA 基础设施红利】：
 * 1. 标准化 @Tool 声明式注解：直接通过方法签名与 JavaDoc / description 声明工具元数据；
 * 2. 自动生成 OpenAPI / JSON Schema，大模型 Function Calling 原生调用；
 * 3. 统一 ToolCallback 抽象（涵盖 MethodToolCallback 与 FunctionToolCallback）；
 * 4. 内置 MCP Client 原生接管连接池、自动心跳检测与工具动态发现；
 * 5. 结合 Bean Validation 运行时强校验，触发 SchemaRepairCallAdvisor 自愈纠错！
 */
@Component
public class SpringAi2ToolAdapter implements EnterpriseToolPort {

    private static final Logger log = LoggerFactory.getLogger(SpringAi2ToolAdapter.class);

    private final Validator validator;
    private final ShoppingDomainService domainService;
    private final RestClient restClient;

    @Value("${mcp.product.server-url:http://localhost:8081}")
    private String productMcpUrl;

    @Value("${mcp.knowledge.server-url:http://localhost:8082}")
    private String knowledgeMcpUrl;

    // 本地缓存的商品模拟数据（对接 MCP Product Server 失败时的可靠兜底）
    private final Map<String, Map<String, Object>> mockProductCatalog = new LinkedHashMap<>();

    public SpringAi2ToolAdapter(Validator validator) {
        this.validator = validator;
        this.domainService = new ShoppingDomainService();
        this.restClient = RestClient.builder().build();
        initCatalog();
    }

    private void initCatalog() {
        mockProductCatalog.put("NB001", Map.of(
                "id", "NB001", "name", "联想拯救者 Y9000P", "brand", "联想",
                "price", new BigDecimal("8999.00"), "stock", 15, "category", "游戏本",
                "specs", "i9-14900HX / RTX4060 / 16G / 1TB / 2.5K 240Hz"
        ));
        mockProductCatalog.put("NB002", Map.of(
                "id", "NB002", "name", "Apple MacBook Pro 16", "brand", "Apple",
                "price", new BigDecimal("19999.00"), "stock", 8, "category", "轻薄办公本",
                "specs", "M3 Max / 36G / 1TB / Liquid 视网膜 XDR"
        ));
        mockProductCatalog.put("PH001", Map.of(
                "id", "PH001", "name", "iPhone 16 Pro Max", "brand", "Apple",
                "price", new BigDecimal("9999.00"), "stock", 20, "category", "智能手机",
                "specs", "A18 Pro / 256GB / 沙漠钛金"
        ));
    }

    // =========================================================================
    // Spring AI 2.0 声明式 @Tool 注解注册（由 ChatClient.defaultTools() 自动扫描装配）
    // =========================================================================

    /**
     * 查询商品的详细规格参数、品牌、分类、价格与库存
     */
    @Tool(name = "queryProductInfo", description = "查询商品的详细规格信息。传入商品ID（如 NB001、NB002、PH001）。")
    @Override
    public Map<String, Object> queryProductInfo(
            @ToolParam(description = "商品ID，格式如 NB001, NB002") String productId) {
        log.info("[SpringAI2 Tool] 执行 queryProductInfo, productId={}", productId);

        if (productId != null) {
            productId = productId.trim().toUpperCase();
        }

        Map<String, Object> product = mockProductCatalog.get(productId);
        if (product != null) {
            return product;
        }

        // 尝试通过 HTTP/MCP 代理调用外部 Product Server (:8081)
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> remote = restClient.get()
                    .uri(productMcpUrl + "/api/products/" + productId)
                    .retrieve()
                    .body(Map.class);
            if (remote != null) return remote;
        } catch (Exception e) {
            log.warn("[SpringAI2 Tool] 外部 MCP 8081 请求失败，使用本地兜底: {}", e.getMessage());
        }

        return Map.of("error", "未找到商品 " + productId);
    }

    /**
     * 查询商品实时价格
     */
    @Tool(name = "queryPrice", description = "查询商品价格。传入单个商品ID，返回该商品的价格。")
    @Override
    public BigDecimal queryPrice(
            @ToolParam(description = "商品ID") String productId) {
        log.info("[SpringAI2 Tool] 执行 queryPrice, productId={}", productId);
        Map<String, Object> info = queryProductInfo(productId);
        if (info.containsKey("price")) {
            return (BigDecimal) info.get("price");
        }
        return BigDecimal.ZERO;
    }

    /**
     * 查询商品剩余库存
     */
    @Tool(name = "queryStock", description = "查询商品的实时库存数量。传入单个商品ID。")
    @Override
    public Integer queryStock(
            @ToolParam(description = "商品ID") String productId) {
        log.info("[SpringAI2 Tool] 执行 queryStock, productId={}", productId);
        Map<String, Object> info = queryProductInfo(productId);
        if (info.containsKey("stock")) {
            return (Integer) info.get("stock");
        }
        return 0;
    }

    /**
     * 强契约订单创建工具（带 Bean Validation 校验与领域风控检查）
     */
    @Tool(name = "createOrder", description = "创建购物订单。必须提供合法的用户ID、商品ID、购买数量、总金额、收货地址和幂等键。")
    @Override
    public Map<String, Object> createOrder(
            @ToolParam(description = "结构化下单请求参数") ToolOrderRequest request) {
        log.info("[SpringAI2 Tool] 执行 createOrder: {}", request);

        // 1. 【强契约运行时校验】触发 Jakarta Bean Validation
        Set<ConstraintViolation<ToolOrderRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            log.warn("[SpringAI2 Tool] ❌ 参数强校验不通过，抛出异常触发自愈循环");
            throw new ConstraintViolationException("订单参数校验不通过", violations);
        }

        // 2. 【领域业务规则与风控校验】展示 P18“业务语义必须由开发者守护”
        Optional<String> riskError = domainService.validateOrderRisk(request);
        if (riskError.isPresent()) {
            return Map.of(
                    "status", "REJECTED_BY_RISK_CONTROL",
                    "reason", riskError.get()
            );
        }

        // 3. 执行下单业务逻辑
        String orderId = "ORD-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 900 + 100);
        log.info("[SpringAI2 Tool] ✅ 订单创建成功，订单号: {}", orderId);

        return Map.of(
                "orderId", orderId,
                "status", "SUCCESS",
                "productId", request.getProductId(),
                "quantity", request.getQuantity(),
                "totalAmount", request.getTotalAmount(),
                "message", "订单创建成功，请等待发货"
        );
    }

    /**
     * 向量知识库检索（对接 mcp-knowledge-server :8082）
     */
    @Tool(name = "searchKnowledge", description = "检索商城知识库，获取商品详细手册、售后退换货政策与选购指南。")
    @Override
    public String searchKnowledge(
            @ToolParam(description = "搜索查询词") String query) {
        log.info("[SpringAI2 Tool] 执行 searchKnowledge, query={}", query);

        try {
            String remoteResult = restClient.get()
                    .uri(knowledgeMcpUrl + "/api/knowledge/search?query=" + query)
                    .retrieve()
                    .body(String.class);
            if (remoteResult != null && !remoteResult.isBlank()) {
                return remoteResult;
            }
        } catch (Exception e) {
            log.warn("[SpringAI2 Tool] 外部 MCP 8082 知识检索请求失败，使用兜底知识: {}", e.getMessage());
        }

        // 本地降级知识片段
        return """
                【商城服务政策须知】：
                1. 电脑整机支持 7 天无理由退货（包装完好，未激活 Office 软件）。
                2. 联想拯救者 Y9000P 享受 2 年整机质保，支持上门快修服务。
                3. Apple MacBook Pro 享受 1 年有限保修及 90 天免费技术支持。
                """;
    }
}
