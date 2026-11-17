package com.example.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring AI 2.0.2 GA 现代基础设施对照工程启动主类
 *
 * 运行基线：Java 21 + Spring Boot 4.0.0 + Spring Framework 7.0 + Spring AI 2.0.2 GA
 * 默认运行端口：8083（与原工程 8080 及 MCP 8081/8082 隔离独立并存）
 */
@SpringBootApplication
public class SpringAi2AgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAi2AgentApplication.class, args);
    }
}
