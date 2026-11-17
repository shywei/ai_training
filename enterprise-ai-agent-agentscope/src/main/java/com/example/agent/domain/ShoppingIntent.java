package com.example.agent.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 导购意图与选品打分领域模型（纯 Java POJO，零框架依赖）
 *
 * 对应第 09 课 P20~P22 六边形防套牢架构：
 * 领域核心（Domain Core）严禁受任何 AI 框架（如 Spring AI / AgentScope / LangChain4j）的注解或接口污染。
 * 无论是从自然语言抽取意图，还是后续接入不同的 Agent 运行时，核心选品决策与意图结构完全稳定。
 */
public class ShoppingIntent {

    /** 意图类别：QUERY(咨询), COMPARE(对比), PURCHASE(下单), COMPLAINT(售后) */
    private String intentCategory;

    /** 目标品类：如 "笔记本电脑", "智能手机" */
    private String targetCategory;

    /** 预算上限 (元) */
    private BigDecimal budgetLimit;

    /** 用户偏好的关键词/品牌列表 */
    private List<String> preferredBrands = new ArrayList<>();

    /** 核心使用场景：如 "商务办公", "3A游戏", "剪辑视频" */
    private String primaryUsage;

    /** 是否属于明确的对比选品请求 (>=2 个候选商品) */
    private boolean comparative;

    /** 候选商品 ID 列表 */
    private List<String> candidateProductIds = new ArrayList<>();

    /** 选品打分明细：Key=商品ID, Value=综合得分 (0.0 ~ 100.0) */
    private Map<String, Double> productScores = Collections.emptyMap();

    public ShoppingIntent() {
    }

    public ShoppingIntent(String intentCategory, String targetCategory, BigDecimal budgetLimit,
                          List<String> preferredBrands, String primaryUsage, boolean comparative) {
        this.intentCategory = intentCategory;
        this.targetCategory = targetCategory;
        this.budgetLimit = budgetLimit;
        if (preferredBrands != null) {
            this.preferredBrands = new ArrayList<>(preferredBrands);
        }
        this.primaryUsage = primaryUsage;
        this.comparative = comparative;
    }

    public String getIntentCategory() {
        return intentCategory;
    }

    public void setIntentCategory(String intentCategory) {
        this.intentCategory = intentCategory;
    }

    public String getTargetCategory() {
        return targetCategory;
    }

    public void setTargetCategory(String targetCategory) {
        this.targetCategory = targetCategory;
    }

    public BigDecimal getBudgetLimit() {
        return budgetLimit;
    }

    public void setBudgetLimit(BigDecimal budgetLimit) {
        this.budgetLimit = budgetLimit;
    }

    public List<String> getPreferredBrands() {
        return preferredBrands;
    }

    public void setPreferredBrands(List<String> preferredBrands) {
        this.preferredBrands = preferredBrands != null ? new ArrayList<>(preferredBrands) : new ArrayList<>();
    }

    public String getPrimaryUsage() {
        return primaryUsage;
    }

    public void setPrimaryUsage(String primaryUsage) {
        this.primaryUsage = primaryUsage;
    }

    public boolean isComparative() {
        return comparative;
    }

    public void setComparative(boolean comparative) {
        this.comparative = comparative;
    }

    public List<String> getCandidateProductIds() {
        return candidateProductIds;
    }

    public void setCandidateProductIds(List<String> candidateProductIds) {
        this.candidateProductIds = candidateProductIds != null ? new ArrayList<>(candidateProductIds) : new ArrayList<>();
    }

    public Map<String, Double> getProductScores() {
        return productScores;
    }

    public void setProductScores(Map<String, Double> productScores) {
        this.productScores = productScores != null ? productScores : Collections.emptyMap();
    }

    @Override
    public String toString() {
        return "ShoppingIntent{" +
                "intentCategory='" + intentCategory + '\'' +
                ", targetCategory='" + targetCategory + '\'' +
                ", budgetLimit=" + budgetLimit +
                ", preferredBrands=" + preferredBrands +
                ", primaryUsage='" + primaryUsage + '\'' +
                ", comparative=" + comparative +
                ", candidateProductIds=" + candidateProductIds +
                ", productScores=" + productScores +
                '}';
    }
}
