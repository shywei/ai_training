package com.example.agent.tool;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 模拟企业商品服务 API
 *
 * 这个 Controller 模拟企业 ERP/CRM 系统的商品接口。
 * 生产环境中，这些接口由真实的 ERP 系统提供。
 * Demo 中用 Controller 模拟，方便演示和调试。
 *
 * 第七课核心教学点：
 * - Tool 的本质是给 LLM 提供"做事"的能力
 * - 这些接口本身不复杂，复杂的是"怎么安全地让 LLM 调用"
 */
@RestController
@RequestMapping("/api/product")
public class ProductApiController {

    /**
     * 查询商品价格
     * GET /api/product/{id}/price
     */
    @GetMapping("/{id}/price")
    public Map<String, Object> getPrice(@PathVariable String id) {
        ProductData product = ProductData.getById(id);
        if (product == null) {
            return Map.of("error", "商品不存在", "productId", id);
        }
        return Map.of(
                "productId", id,
                "name", product.name,
                "price", product.price,
                "status", product.status
        );
    }

    /**
     * 查询商品库存
     * GET /api/product/{id}/stock
     */
    @GetMapping("/{id}/stock")
    public Map<String, Object> getStock(@PathVariable String id) {
        ProductData product = ProductData.getById(id);
        if (product == null) {
            return Map.of("error", "商品不存在", "productId", id);
        }
        return Map.of(
                "productId", id,
                "name", product.name,
                "stock", product.stock,
                "status", product.status
        );
    }

    /**
     * 查询商品完整信息（价格+库存+状态）
     * GET /api/product/{id}/info
     */
    @GetMapping("/{id}/info")
    public Map<String, Object> getInfo(@PathVariable String id) {
        ProductData product = ProductData.getById(id);
        if (product == null) {
            return Map.of("error", "商品不存在", "productId", id);
        }
        return Map.of(
                "productId", id,
                "name", product.name,
                "price", product.price,
                "stock", product.stock,
                "status", product.status,
                "sales", product.sales
        );
    }

    /**
     * 模拟商品数据（对应 schema.sql 中的 product 表）
     * 生产中从数据库查询，Demo 用内存数据模拟
     */
    static class ProductData {
        String id, name, status;
        int price, stock, sales;

        ProductData(String id, String name, int price, int stock, String status, int sales) {
            this.id = id; this.name = name; this.price = price;
            this.stock = stock; this.status = status; this.sales = sales;
        }

        static ProductData getById(String id) {
            return switch (id) {
                case "NB001" -> new ProductData(id, "ThinkPad X1 Carbon Gen12", 9999, 58, "在售", 342);
                case "NB002" -> new ProductData(id, "MacBook Pro 14 M3 Pro", 14999, 23, "在售", 567);
                case "NB003" -> new ProductData(id, "联想拯救者 Y9000P 2025", 8999, 12, "在售", 891);
                case "NB004" -> new ProductData(id, "华为 MateBook X Pro 2025", 11999, 0, "缺货", 234);
                case "NB005" -> new ProductData(id, "戴尔 XPS 14 9440", 12999, 31, "在售", 156);
                case "NB006" -> new ProductData(id, "小米 Book Pro 16 2025", 5999, 200, "在售", 1203);
                case "NB007" -> new ProductData(id, "华硕 ROG 幻16 Air", 12999, 8, "在售", 78);
                case "NB008" -> new ProductData(id, "ThinkPad T14s Gen5", 7999, 67, "在售", 445);
                case "NB009" -> new ProductData(id, "MacBook Air 15 M3", 9499, 45, "在售", 678);
                case "NB010" -> new ProductData(id, "机械革命 蛟龙16 Pro", 6499, 150, "在售", 2340);
                case "NB011" -> new ProductData(id, "戴尔 灵越14 Plus 7440", 6999, 89, "在售", 567);
                case "NB012" -> new ProductData(id, "惠普 暗影精灵10", 7999, 35, "在售", 789);
                case "PH001" -> new ProductData(id, "iPhone 16 Pro Max", 9999, 120, "在售", 2345);
                case "PH002" -> new ProductData(id, "华为 Mate 70 Pro", 6499, 80, "在售", 1876);
                case "PH003" -> new ProductData(id, "小米 15 Pro", 4999, 300, "在售", 5678);
                case "PH004" -> new ProductData(id, "OPPO Find X8 Ultra", 5999, 45, "在售", 890);
                case "PH005" -> new ProductData(id, "三星 Galaxy S25 Ultra", 9999, 60, "在售", 1234);
                case "PH006" -> new ProductData(id, "vivo X200 Pro", 4499, 200, "在售", 3456);
                case "HP001" -> new ProductData(id, "AirPods Pro 2", 1899, 500, "在售", 8901);
                case "HP002" -> new ProductData(id, "索尼 WH-1000XM5", 2299, 80, "在售", 2345);
                case "HP003" -> new ProductData(id, "华为 FreeBuds Pro 3", 1199, 300, "在售", 4567);
                case "HP004" -> new ProductData(id, "小米 Buds 5 Pro", 699, 1000, "在售", 12340);
                case "TB001" -> new ProductData(id, "iPad Pro 13 M4", 10999, 35, "在售", 567);
                case "TB002" -> new ProductData(id, "华为 MatePad Pro 13.2", 5699, 60, "在售", 1234);
                case "TB003" -> new ProductData(id, "小米 Pad 7 Pro", 2999, 200, "在售", 3456);
                case "TB004" -> new ProductData(id, "iPad Air 11 M2", 4799, 100, "在售", 2345);
                default -> null;
            };
        }
    }
}
