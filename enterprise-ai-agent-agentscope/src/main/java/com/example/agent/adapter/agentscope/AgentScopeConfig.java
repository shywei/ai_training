package com.example.agent.adapter.agentscope;

import com.example.agent.adapter.tool.ShoppingAgentTools;
import com.example.agent.domain.ShoppingDomainService;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * AgentScope 2.0 核心配置中心：装配模型客户端、工具集与 ReActAgent / HarnessAgent 运行实例
 *
 * 对应第09课 P19（AgentScope Java 2.0 模块映射全景与深层辨析）：
 *
 * ────────── HarnessAgent 架构辨析与三层记忆映射 ──────────
 * 1. 【HarnessAgent 的新定位（从 Agent Framework 到 Agent Harness）】：
 *    - 阿里云开发者社区明确定义：Agent Harness 为智能体提供统一的宿主环境（Host Container）。
 *    - Harness 容器负责四大工程支柱：
 *      ① Workspace  (执行工作空间与持久化沙箱文件系统)
 *      ② Memory     (会话内存上下文与短期记忆窗口)
 *      ③ Sandbox    (Daytona / E2B / Kubernetes 隔离代码执行环境)
 *      ④ StateStore (会话运行状态持久化存储：支持 Redis, MySQL, PG, OSS, COS)
 *
 * 2. 【核心架构边界：State Store 状态持久化 != 完整三层企业记忆系统】：
 *    - 学员最容易产生的认知误区：以为挂载了 AgentScope 的 RedisAgentStateStore 就不需要第05课手搓的记忆系统了。
 *    - 事实边界：
 *      - 【State Store】：仅解决当前进程/会话的断点持久化与崩溃恢复（状态机游标、未完成 Tool 挂起态）。
 *      - 【企业三层记忆系统】：
 *        ① Redis 会话缓冲（Session Memory，多轮滑动窗口与 Token 剪枝）
 *        ② MySQL 用户画像（User Profile，长期消费偏好、预算上限、收货习惯提取）
 *        ③ pgvector 历史语义记忆（RAG Long-Term Memory，跨会话知识与历史订单问答）
 *    - 结论：框架接管状态机持久化（State Store），但业务语义的画像提取与向量记忆仍需与企业数据资产深度打通！
 */
@Configuration
public class AgentScopeConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentScopeConfig.class);

    @Value("${agentscope.model.api-key:sk-fake-for-dev}")
    private String apiKey;

    @Value("${agentscope.model.base-url:https://api.siliconflow.cn}")
    private String baseUrl;

    @Value("${agentscope.model.model-name:Qwen/Qwen2.5-7B-Instruct}")
    private String modelName;

    @Value("${agentscope.model.stream:true}")
    private boolean stream;

    @Value("${agentscope.agent.name:EnterpriseShoppingAgentScope}")
    private String agentName;

    @Value("${agentscope.agent.max-iters:10}")
    private int maxIters;

    @Value("${agentscope.agent.default-session-id:default-session}")
    private String defaultSessionId;

    @Bean
    public ShoppingDomainService shoppingDomainService() {
        return new ShoppingDomainService();
    }

    /**
     * 1. 配置 OpenAI 兼容协议大模型客户端（无缝接入 SiliconFlow / 阿里云百炼 / 本地 Ollama）
     */
    @Bean
    public Model agentScopeModel() {
        log.info("[AgentScope-Config] 初始化 OpenAI 兼容大模型客户端: baseUrl={}, modelName={}", baseUrl, modelName);
        return OpenAIChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .stream(stream)
                .build();
    }

    /**
     * 2. 配置 AgentScope Toolkit：注册通过 @Tool 注解暴露的企业工具
     */
    @Bean
    public Toolkit agentScopeToolkit(ShoppingAgentTools shoppingAgentTools) {
        log.info("[AgentScope-Config] 注册企业导购工具集 (ShoppingAgentTools)...");
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(shoppingAgentTools);
        log.info("[AgentScope-Config] 已成功注册工具数量: {}", toolkit.getToolNames().size());
        return toolkit;
    }

    /**
     * 3. 装配 ReActAgent 主智能体实例（挂载中间件、Toolkit、状态机与 StateStore）
     */
    @Bean
    public ReActAgent shoppingReActAgent(
            Model agentScopeModel,
            Toolkit agentScopeToolkit,
            LifecycleMiddleware lifecycleMiddleware,
            SchemaRepairMiddleware schemaRepairMiddleware,
            AgentScopeCheckpointStore checkpointStore) {

        log.info("[AgentScope-Config] 构建企业级 ReActAgent: agentName={}, maxIters={}", agentName, maxIters);

        String systemPrompt = """
                你是企业专属智能导购助理（基于 AgentScope 2.0.0 GA 架构）。
                你的职责是：
                1. 理解用户购买意向，查询商城在售商品信息 (query_product)。
                2. 实时校验商品库存与仓储可用性 (check_inventory)。
                3. 当涉及对比选购时，调用知识库深入分析硬件规格与评测指南 (search_knowledge)。
                4. 当用户明确要求下单时，调用 (create_order) 提交订单。
                   注意：请严格按照规范格式传入参数，对于大额高风险订单系统将自动挂起进入审批流程。
                """;

        return ReActAgent.builder()
                .name(agentName)
                .sysPrompt(systemPrompt)
                .model(agentScopeModel)
                .toolkit(agentScopeToolkit)
                .maxIters(maxIters)
                .defaultSessionId(defaultSessionId)
                .middlewares(List.of(schemaRepairMiddleware, lifecycleMiddleware)) // 挂载五阶段生命周期与自愈中间件
                .stateStore(checkpointStore.getAgentScopeStateStore())              // 挂载状态存储器
                .build();
    }
}
