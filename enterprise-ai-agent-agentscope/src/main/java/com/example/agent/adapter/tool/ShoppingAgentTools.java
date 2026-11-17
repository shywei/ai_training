package com.example.agent.adapter.tool;

import com.example.agent.domain.ToolOrderRequest;
import com.example.agent.port.EnterpriseToolPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AgentScope 专属工具注册组件：提供 @Tool 声明式工具方法
 *
 * 对应第 09 课 P15、P18：
 * 1. 使用 AgentScope 官方 `@Tool` 与 `@ToolParam` 注解暴露给 ReActAgent。
 * 2. 入参通过 Jakarta Bean Validation 强约束校验（@NotBlank, @Pattern, @Min, @Size, @DecimalMin），
 *    若校验失败抛出规范的校验异常，供 SchemaRepairMiddleware 捕获自愈修复。
 */
@Component
public class ShoppingAgentTools {

    private static final Logger log = LoggerFactory.getLogger(ShoppingAgentTools.class);

    private final EnterpriseToolPort enterpriseToolPort;
    private final Validator validator;
    private final ObjectMapper objectMapper;

    public ShoppingAgentTools(EnterpriseToolPort enterpriseToolPort, Validator validator, ObjectMapper objectMapper) {
        this.enterpriseToolPort = enterpriseToolPort;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    @Tool(name = "queryProductInfo", description = "查询指定商品的详细信息，包括商品名、品牌、价格、库存和硬件规格")
    public String queryProductInfo(
            @ToolParam(name = "productId", description = "商品唯一编号（例如 NB001 拯救者, NB002 MacBook, NB003 MateBook, PH001 Mate60）", required = true)
            String productId) {
        log.info("[AgentScope-Tool] 执行 queryProductInfo, productId={}", productId);
        try {
            Map<String, Object> info = enterpriseToolPort.queryProductInfo(productId);
            return objectMapper.writeValueAsString(info);
        } catch (Exception e) {
            log.error("[AgentScope-Tool] queryProductInfo 异常: {}", e.getMessage());
            return "{\"error\":\"查询商品失败: " + e.getMessage() + "\"}";
        }
    }

    @Tool(name = "queryPrice", description = "快速查询指定商品的实时单价")
    public String queryPrice(
            @ToolParam(name = "productId", description = "商品编号，如 NB001", required = true)
            String productId) {
        log.info("[AgentScope-Tool] 执行 queryPrice, productId={}", productId);
        BigDecimal price = enterpriseToolPort.queryPrice(productId);
        return "¥" + price.toPlainString();
    }

    @Tool(name = "queryStock", description = "快速查询指定商品的实时库存可用量")
    public String queryStock(
            @ToolParam(name = "productId", description = "商品编号，如 NB001", required = true)
            String productId) {
        log.info("[AgentScope-Tool] 执行 queryStock, productId={}", productId);
        Integer stock = enterpriseToolPort.queryStock(productId);
        return stock + " 件";
    }

    @Tool(name = "createOrder", description = "提交购买订单，生成正式业务订单记录（高风险动作，金额过大将触发人在回路审批挂起）")
    public String createOrder(
            @ToolParam(name = "userId", description = "用户编号，格式为U加数字（如 U001）", required = true)
            String userId,
            @ToolParam(name = "productId", description = "商品编号，如 NB001", required = true)
            String productId,
            @ToolParam(name = "quantity", description = "购买数量，必须为整数且至少为1", required = true)
            Integer quantity,
            @ToolParam(name = "totalAmount", description = "订单总金额，如 8999.00", required = true)
            Double totalAmount,
            @ToolParam(name = "shippingAddress", description = "详细收货地址，长度在6至200字符之间", required = true)
            String shippingAddress,
            @ToolParam(name = "contactPhone", description = "联系人有效11位大陆手机号码", required = true)
            String contactPhone,
            @ToolParam(name = "idempotencyKey", description = "业务幂等键，8-64位字符", required = false)
            String idempotencyKey) {

        log.info("[AgentScope-Tool] 执行 createOrder 尝试, userId={}, productId={}, quantity={}, amount={}",
                userId, productId, quantity, totalAmount);

        String idemKey = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey : "IDEM-" + UUID.randomUUID().toString().substring(0, 16);

        BigDecimal amount = totalAmount != null ? BigDecimal.valueOf(totalAmount) : BigDecimal.ZERO;
        ToolOrderRequest request = new ToolOrderRequest(userId, productId, quantity, amount, shippingAddress, contactPhone, idemKey);

        // 执行 Bean Validation 强约束校验（P15 深度深水区 3）
        Set<ConstraintViolation<ToolOrderRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String errorMsg = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            log.warn("[AgentScope-Tool] 校验未通过，触发 Schema 校验异常: {}", errorMsg);
            throw new IllegalArgumentException("ValidationError: " + errorMsg);
        }

        Map<String, Object> result = enterpriseToolPort.createOrder(request);
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return result.toString();
        }
    }

    @Tool(name = "searchKnowledge", description = "检索企业内部选品评测指南、硬件参数百科与售后服务退换货政策（RAG）")
    public String searchKnowledge(
            @ToolParam(name = "query", description = "用户提出的疑问或检索词条", required = true)
            String query) {
        log.info("[AgentScope-Tool] 执行 searchKnowledge, query={}", query);
        try {
            return enterpriseToolPort.searchKnowledge(query);
        } catch (Exception e) {
            log.error("[AgentScope-Tool] searchKnowledge 异常: {}", e.getMessage());
            return "{\"error\":\"知识库检索失败: " + e.getMessage() + "\"}";
        }
    }
}
