package com.example.agent.port;

import com.example.agent.domain.ToolOrderRequest;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 六边形出站端口：企业系统工具交互端口（Outbound Driven Port）
 *
 * 对应第 09 课 P05（统一 ToolCallback / 内置 MCP 演进）与 P15~P18（Tool 契约与业务语义守护）：
 * 屏蔽 MCP 传输层协议、HTTP RPC 或本地 MySQL/PostgreSQL 调用的底层差异。
 */
public interface EnterpriseToolPort {

    /**
     * 查询商品详细信息（价格、库存、规格、品牌）
     *
     * @param productId 商品 ID (如 NB001)
     * @return 商品详情键值对
     */
    Map<String, Object> queryProductInfo(String productId);

    /**
     * 查询商品单价
     *
     * @param productId 商品 ID
     * @return 价格金额
     */
    BigDecimal queryPrice(String productId);

    /**
     * 查询商品实时库存
     *
     * @param productId 商品 ID
     * @return 剩余件数
     */
    Integer queryStock(String productId);

    /**
     * 创建业务订单（带强类型参数与风控校验）
     *
     * @param request 下单请求
     * @return 订单创建结果（包含 orderId, status 等）
     */
    Map<String, Object> createOrder(ToolOrderRequest request);

    /**
     * 检索企业向量知识库（商品手册、售后政策等）
     *
     * @param query 检索查询词
     * @return 知识文本片段
     */
    String searchKnowledge(String query);
}
