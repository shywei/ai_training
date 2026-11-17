package com.example.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * AgentScope Java 2.0.0 GA 企业导购智能体重构工程主启动类
 *
 * 对应第09课《从手搓到框架：框架的边界》
 * 运行端口：8084
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class AgentScopeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentScopeApplication.class, args);
    }
}
