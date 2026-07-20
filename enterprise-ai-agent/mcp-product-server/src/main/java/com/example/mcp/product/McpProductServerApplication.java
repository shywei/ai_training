package com.example.mcp.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MCP Product Server 启动类
 * 模拟企业ERP系统的MCP Server，提供商品查询能力
 */
@SpringBootApplication
public class McpProductServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpProductServerApplication.class, args);
    }
}
