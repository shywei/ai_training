package com.example.agent.adapter.springai2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Spring AI 2.0.2 GA ChatClient 流式链式构建配置
 *
 * - 引入标准化 Fluent API：ChatClient.builder(chatModel)
 * - 链式挂载 defaultSystem()、defaultAdvisors()、defaultTools() 与官方内置 MCP ToolCallbackProvider
 * - 底层内置 ToolCallingAdvisor 自动接管 @Tool / MCP 工具调用闭环
 */
@Configuration
public class SpringAi2ChatConfig {

    private static final Logger log = LoggerFactory.getLogger(SpringAi2ChatConfig.class);

    @Value("${agent.name:导购助手}")
    private String agentName;

    @Value("${agent.role:你是一个专业的电商导购助手，帮助用户推荐合适的商品。}")
    private String agentRole;

    @Value("${agent.constraints:推荐商品必须基于商品库知识，不编造商品信息。不确定的信息说“我不确定”。}")
    private String agentConstraints;

    @Bean
    public ChatClient shoppingChatClient(ChatModel chatModel,
                                         SpringAi2ToolAdapter toolAdapter,
                                         GuardrailsCallAdvisor guardrailsAdvisor,
                                         SchemaRepairCallAdvisor repairAdvisor,
                                         TelemetryStreamAdvisor telemetryAdvisor,
                                         ObjectProvider<SyncMcpToolCallbackProvider> mcpProvider) {
        log.info("[SpringAI2] 初始化 ChatClient.builder 流式构建器，注入 Advisors 与 Tools...");

        String systemPrompt = String.format("""
                # 角色设定
                你是 %s。%s

                # 行为准则
                - 严格基于企业商品库知识与工具返回的事实回答，绝不虚构商品型号或价格。
                - 当用户询问商品配置、库存或比价时，优先调用 queryProductInfo、queryPrice、queryStock 工具。
                - 当用户明确要求下单且满足条件时，调用 createOrder 工具。
                - 如果不确定某项规格或参数，坦诚回答“我不确定”，严禁生成幻觉。

                # 安全与业务约束
                %s
                """, agentName, agentRole, agentConstraints);

        ChatClient.Builder builder = ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                // 挂载 Spring AI 2.0 双向拦截链：安全守卫 + 自愈纠错 + 流式遥测
                .defaultAdvisors(List.of(guardrailsAdvisor, repairAdvisor, telemetryAdvisor))
                // 挂载声明式 @Tool 本地工具适配器
                .defaultTools(toolAdapter);

        // 若启用了 spring.ai.mcp.client.enabled=true，则自动挂载官方内置 MCP Client 工具集
        SyncMcpToolCallbackProvider mcp = mcpProvider.getIfAvailable();
        if (mcp != null) {
            log.info("[SpringAI2] 检测到官方内置 MCP Client，自动挂载 SyncMcpToolCallbackProvider");
            builder.defaultToolCallbacks(mcp);
        }

        return builder.build();
    }
}
