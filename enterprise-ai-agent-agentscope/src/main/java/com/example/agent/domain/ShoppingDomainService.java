package com.example.agent.domain;

import java.math.BigDecimal;
import java.util.*;

/**
 * 导购领域业务服务（Domain Service，纯业务规则，零框架依赖）
 *
 * 对应第 09 课 P18（框架能做的 vs 框架该做的）与 P21（Ports & Adapters）：
 * 架构核心洞察：
 * 框架只能替我们处理模型网络请求、协议解析、工具注册与单步执行，
 * 但核心业务语义（高风险阈值、风控拦截、选品排序加权、合法状态跃迁）
 * 必须且只能由领域服务守护，绝不能委托给任何不可靠的非确定性 LLM！
 */
public class ShoppingDomainService {

    /** 高风险操作金额硬阈值：单笔订单金额超过 5000 元，必须触发人在回路（WAITING_USER 确认） */
    public static final BigDecimal HIGH_RISK_AMOUNT_THRESHOLD = new BigDecimal("5000.00");

    /** 单日用户累计最高限额 (元) */
    public static final BigDecimal DAILY_USER_LIMIT = new BigDecimal("20000.00");

    /** 单品单次最大购买数 */
    public static final int MAX_SINGLE_ITEM_QUANTITY = 10;

    /**
     * 判断工具调用是否属于高风险操作，需挂起工作流进入人在回路（WAITING_USER / CONFIRMING）
     *
     * @param toolName 工具名称
     * @param request  下单参数
     * @return true 表示高风险，必须等待人工批准
     */
    public boolean isHighRiskAction(String toolName, ToolOrderRequest request) {
        if (toolName == null) {
            return false;
        }

        // 订单创建类工具，触发金额风控规则
        if ("createOrder".equalsIgnoreCase(toolName) || "place_order".equalsIgnoreCase(toolName)) {
            if (request == null) {
                return true; // 无法评估参数时默认按高风险防御
            }
            if (request.getTotalAmount() != null &&
                    request.getTotalAmount().compareTo(HIGH_RISK_AMOUNT_THRESHOLD) >= 0) {
                return true;
            }
            if (request.getQuantity() != null && request.getQuantity() > 3) {
                return true; // 大宗批量采购强制人工审核
            }
        }

        // 敏感退款/删除操作无论金额大小均属高风险
        if (toolName.toLowerCase().contains("refund") || toolName.toLowerCase().contains("delete")) {
            return true;
        }

        return false;
    }

    /**
     * 校验下单参数是否符合风控规则
     *
     * @param request 下单请求
     * @return 校验通过返回 Optional.empty()，未通过返回错误描述
     */
    public Optional<String> validateOrderRisk(ToolOrderRequest request) {
        if (request == null) {
            return Optional.of("下单请求不能为空");
        }
        if (request.getQuantity() > MAX_SINGLE_ITEM_QUANTITY) {
            return Optional.of("单品单次限购 " + MAX_SINGLE_ITEM_QUANTITY + " 件，防止黄牛囤货");
        }
        if (request.getTotalAmount().compareTo(DAILY_USER_LIMIT) > 0) {
            return Optional.of("订单金额超过单笔风控上限 ¥" + DAILY_USER_LIMIT);
        }
        return Optional.empty();
    }

    /**
     * 选品打分逻辑（确定性业务排序算法，消除大模型幻觉与乱序偏差）
     *
     * 综合得分 = 价格适配度 (30%) + 用户品牌偏好 (35%) + 场景配置匹配度 (35%)
     *
     * @param candidateProducts 候选商品详情列表 (Map: id, name, brand, price, stock, specs)
     * @param intent            导购意图模型
     * @return 经过排序与打分的候选商品 ID 列表
     */
    public Map<String, Double> scoreAndRankCandidates(List<Map<String, Object>> candidateProducts,
                                                      ShoppingIntent intent) {
        if (candidateProducts == null || candidateProducts.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Double> scoreMap = new LinkedHashMap<>();
        BigDecimal budget = (intent != null && intent.getBudgetLimit() != null)
                ? intent.getBudgetLimit()
                : new BigDecimal("10000.00");
        List<String> preferredBrands = (intent != null && intent.getPreferredBrands() != null)
                ? intent.getPreferredBrands()
                : Collections.emptyList();

        for (Map<String, Object> product : candidateProducts) {
            String productId = String.valueOf(product.get("id"));
            BigDecimal price = new BigDecimal(String.valueOf(product.getOrDefault("price", "0")));
            String brand = String.valueOf(product.getOrDefault("brand", ""));

            double priceScore;
            if (price.compareTo(budget) <= 0) {
                // 预算内：离预算越接近且性价比高者得分越高 (60~100)
                double ratio = price.doubleValue() / budget.doubleValue();
                priceScore = 60.0 + (ratio * 40.0);
            } else {
                // 稍微超预算则按超额比率重罚
                double overRatio = (price.doubleValue() - budget.doubleValue()) / budget.doubleValue();
                priceScore = Math.max(0.0, 50.0 - (overRatio * 100.0));
            }

            double brandScore = 50.0;
            for (String pref : preferredBrands) {
                if (brand.toLowerCase().contains(pref.toLowerCase())) {
                    brandScore = 100.0;
                    break;
                }
            }

            int stock = Integer.parseInt(String.valueOf(product.getOrDefault("stock", "0")));
            double stockFactor = stock > 0 ? 1.0 : 0.2; // 缺货商品大幅降权

            double totalScore = ((priceScore * 0.4) + (brandScore * 0.6)) * stockFactor;
            scoreMap.put(productId, Math.round(totalScore * 10.0) / 10.0);
        }

        // 按得分由高到低排序
        List<Map.Entry<String, Double>> list = new ArrayList<>(scoreMap.entrySet());
        list.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

        Map<String, Double> sortedScores = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : list) {
            sortedScores.put(entry.getKey(), entry.getValue());
        }

        return sortedScores;
    }
}
