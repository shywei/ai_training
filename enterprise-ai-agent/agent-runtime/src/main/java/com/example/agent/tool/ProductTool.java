package com.example.agent.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 商品查询 Tool — 让 LLM 能查价格、查库存
 *
 * 第七课核心教学点：
 * - Tool 是 LLM 和企业系统之间的桥梁
 * - @Tool 注解定义工具的描述和参数
 * - LLM 根据描述决定什么时候调用、传什么参数
 * - 系统执行实际逻辑，返回结果给 LLM
 *
 * 安全边界：
 * - 只读操作（查询），不做写操作（下单）
 * - 参数验证（商品ID 格式）
 * - 权限控制（后续扩展）
 */
@Component
public class ProductTool {

    private final ProductApiController productApi;

    public ProductTool(ProductApiController productApi) {
        this.productApi = productApi;
    }

    /**
     * 查询商品价格和库存
     *
     * @param productId 商品ID，如 NB001、PH001
     * @return 商品的价格、库存、状态信息
     */
    @Tool(description = "查询商品的价格和库存信息。当用户询问商品价格、库存、是否有货时调用此工具。参数productId格式如：NB001、PH001")
    public String queryProductInfo(
            @ToolParam(description = "商品ID，如 NB001、PH001、HP001") String productId) {
        System.out.println("[Tool] 查询商品信息: " + productId);

        var result = productApi.getInfo(productId.toUpperCase());

        if (result.containsKey("error")) {
            return "商品 " + productId + " 不存在，请检查商品ID";
        }

        String name = (String) result.get("name");
        int price = (int) result.get("price");
        int stock = (int) result.get("stock");
        String status = (String) result.get("status");
        int sales = (int) result.get("sales");

        String response = String.format(
                "%s：价格 %d 元，库存 %d 件，状态 %s，累计销量 %d 件",
                name, price, stock, status, sales);

        System.out.println("[Tool] 查询结果: " + response);
        return response;
    }

    /**
     * 查询商品价格
     */
    @Tool(description = "只查询商品价格。当用户只问价格不问库存时调用此工具")
    public String queryPrice(
            @ToolParam(description = "商品ID") String productId) {
        System.out.println("[Tool] 查询价格: " + productId);

        var result = productApi.getPrice(productId.toUpperCase());

        if (result.containsKey("error")) {
            return "商品 " + productId + " 不存在";
        }

        return result.get("name") + "：价格 " + result.get("price") + " 元";
    }

    /**
     * 查询商品库存
     */
    @Tool(description = "只查询商品库存。当用户只问有没有货、库存多少时调用此工具")
    public String queryStock(
            @ToolParam(description = "商品ID") String productId) {
        System.out.println("[Tool] 查询库存: " + productId);

        var result = productApi.getStock(productId.toUpperCase());

        if (result.containsKey("error")) {
            return "商品 " + productId + " 不存在";
        }

        String status = (String) result.get("status");
        int stock = (int) result.get("stock");

        if ("缺货".equals(status)) {
            return result.get("name") + "：目前缺货";
        }

        return result.get("name") + "：库存 " + stock + " 件，有货";
    }
}
