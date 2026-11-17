package com.example.agent.domain;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 强契约工具入参模型：创建订单请求（Tool Contract）
 *
 * 对应第 09 课 P15（深度深水区 3：结构化 I/O 与 Tool Schema 强约束）：
 * 1. 采用 Jakarta Bean Validation 原生注解建立严格的契约边界；
 * 2. 避免大模型任意幻觉、参数缺漏或格式畸变注入企业业务底层；
 * 3. 当参数未通过校验时，抛出规范的校验异常，由 SchemaRepairCallAdvisor 捕获并进入自愈纠错循环。
 */
public class ToolOrderRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "用户ID不能为空")
    @Pattern(regexp = "^U[0-9]{3,8}$", message = "用户ID格式必须为大写字母U开头加3至8位数字，如 U001")
    private String userId;

    @NotBlank(message = "商品ID不能为空")
    @Pattern(regexp = "^(NB|PH|ACC)[0-9]{3}$", message = "商品ID必须为合法编码格式，如 NB001, PH001")
    private String productId;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量至少为 1 件")
    @Max(value = 99, message = "单笔订单购买数量不能超过 99 件")
    private Integer quantity;

    @NotNull(message = "订单总金额不能为空")
    @DecimalMin(value = "0.01", message = "订单金额必须大于 0 元")
    private BigDecimal totalAmount;

    @NotBlank(message = "收货地址不能为空")
    @Size(min = 6, max = 200, message = "收货地址长度必须在 6 到 200 个字符之间")
    private String shippingAddress;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系人手机号码格式不合法")
    private String contactPhone;

    /** 幂等键：防止网络重试或 Agent 反复调用导致重复下单 */
    @NotBlank(message = "业务幂等键不能为空")
    @Size(min = 8, max = 64, message = "幂等键长度必须在 8 到 64 位字符之间")
    private String idempotencyKey;

    /** 备注说明（可选） */
    @Size(max = 200, message = "订单备注不可超过 200 个字")
    private String remark;

    public ToolOrderRequest() {
    }

    public ToolOrderRequest(String userId, String productId, Integer quantity,
                            BigDecimal totalAmount, String shippingAddress,
                            String contactPhone, String idempotencyKey) {
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.shippingAddress = shippingAddress;
        this.contactPhone = contactPhone;
        this.idempotencyKey = idempotencyKey;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "ToolOrderRequest{" +
                "userId='" + userId + '\'' +
                ", productId='" + productId + '\'' +
                ", quantity=" + quantity +
                ", totalAmount=" + totalAmount +
                ", shippingAddress='" + shippingAddress + '\'' +
                ", contactPhone='" + contactPhone + '\'' +
                ", idempotencyKey='" + idempotencyKey + '\'' +
                ", remark='" + remark + '\'' +
                '}';
    }
}
