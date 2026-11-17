# Enterprise AI Agent - AgentScope 2.0 Edition (v7.0-agentscope)

> **课程归属**：第 09 课《从手搓到框架：框架的边界》（24P 黄金容量）  
> **工程定位**：高阶智能体应用框架重构工程（企业级多智能体生产力工具）  
> **核心使命**：展示从底层基础设施（Spring AI 2.0）跃迁到高阶应用框架（AgentScope Java 2.0.0 GA）带来的生产力质变、状态复水机理与六边形防套牢架构。

---

## 1. 为什么选择 AgentScope Java 2.0 而非 Spring AI Alibaba？

在第 09 课的技术选型终局决议中，课程研发团队经过严谨的行业调研与代码验证，明确了选型边界：

| 维度 | Spring AI Alibaba (明确放弃) | AgentScope Java 2.0 (终局选型) |
|:---|:---|:---|
| **版本生产成熟度** | ❌ 仍处 **Milestone 预发布阶段**（如 `v2.0.0-M1.1`），非正式 GA，API 频繁变动，无法满足企业严苛生产稳定性要求。 | ✅ **正式发布稳定 GA**（`io.agentscope:agentscope:2.0.0`，最新 2.0.4），经历阿里核心电商业务多轮 RC 淬炼。 |
| **产品战略定位** | 侧重阿里云自研生态基础设施适配（通义千问模型、Nacos 动态配置、DashScope 原生适配）。 | 阿里正统开源智能体应用框架，战略转向通用 **Agent Harness**（宿主容器），具备独立完备的多智能体运行时。 |
| **教学目标匹配度** | 仅是 Spring AI 在阿里生态下的适配层，未能跳出传统 Spring 请求/响应思维。 | 原生支持 `ReActAgent`、五阶段生命周期 Middleware、28 类类型化 `AgentEvent` 流与 `HarnessAgent`，完美承接第 09 课框架机制解构与第 10 课分布式 Multi-Agent 编排。 |
| **跨生态兼容性** | 强绑定 Spring Boot 3 与阿里云 SDK。 | 完美支持 OpenAI 协议兼容端点（SiliconFlow / 本地 Ollama / 深度求索 DeepSeek），支持 JDK 17+ 与 Spring Boot 3.4.1 原生集成。 |

---

## 2. 六边形防套牢架构（Hexagonal Architecture - Ports & Adapters）

为了防止被特定 AI 框架“锁死”，本工程严格贯彻第 09 课 P20~P22 强调的**六边形架构**规范。核心业务语义完全下沉于 `domain` 核心，AI 框架作为外围可拔插的 `adapter`：

```
                    ┌────────────────────────────────────────────────────────┐
                    │               REST & SSE Inbound Adapters              │
                    │      (WorkflowV7Controller / stream-events / fork)     │
                    └──────────────────────────┬─────────────────────────────┘
                                               │
                                       ┌───────▼───────┐
                                       │ ShoppingAgent │
                                       │     Port      │ (Inbound Port)
                                       └───────┬───────┘
                                               │
     ┌─────────────────────────────────────────┼─────────────────────────────────────────┐
     │                                         │                                         │
     │   ┌─────────────────────────────────────▼─────────────────────────────────────┐   │
     │   │                    Domain Core (纯 Java POJO，零框架依赖)                  │   │
     │   │  - ShoppingIntent.java         (导购意图与多品对比选品模型)                    │   │
     │   │  - AgentCheckpoint.java        (统一状态快照：三级边界与时光倒流 Fork)         │   │
     │   │  - ToolOrderRequest.java       (Jakarta Bean Validation @Pattern/@Min 强契约) │   │
     │   │  - ShoppingDomainService.java  (金额风控阈值判定、风险等级评估、选品打分)     │   │
     │   └─────────────────────────────────────┬─────────────────────────────────────┘   │
     │                                         │                                         │
     └─────────────────────────────────────────┼─────────────────────────────────────────┘
                                               │
                       ┌───────────────────────┴───────────────────────┐
                       │                                               │
               ┌───────▼──────────┐                            ┌───────▼──────────┐
               │ CheckpointStore  │ (Outbound Port)            │ EnterpriseTool   │ (Outbound Port)
               │      Port        │                            │      Port        │
               └───────┬──────────┘                            └───────┬──────────┘
                       │                                               │
       ┌───────────────▼───────────────┐               ┌───────────────▼───────────────┐
       │   AgentScopeCheckpointStore   │               │   McpEnterpriseToolAdapter    │
       │ (StateStore / Rehydration)    │               │ (Product 8081 / Knowledge 8082│
       └───────────────────────────────┘               └───────────────────────────────┘
```

### 目录与分包架构

```
src/main/java/com/example/agent/
├── domain/                          # 【领域核心】纯 Java POJO，零框架依赖
│   ├── ShoppingIntent.java          # 导购意图与选品候选打分模型
│   ├── AgentCheckpoint.java         # 统一状态快照模型（区分 Chat History / StateStore / Checkpointer）
│   ├── ToolOrderRequest.java        # 带 @NotNull, @Pattern, @Min 约束的工具强契约
│   └── ShoppingDomainService.java   # 纯领域风控规则（大额订单高风险阈值判定、选品打分）
├── port/                            # 【端口层】六边形出入站端口接口
│   ├── ShoppingAgentPort.java       # 入站端口：executeTask / streamTask / rehydrate / confirmAction
│   ├── CheckpointStorePort.java     # 出站端口：saveSnapshot / saveDelta / loadCheckpoint / fork
│   └── EnterpriseToolPort.java      # 出站端口：queryProducts / checkInventory / createOrder / search
├── adapter/
│   ├── agentscope/                  # 【AgentScope 2.0 专属适配器实现】
│   │   ├── AgentScopeConfig.java    # OpenAI 兼容客户端、Toolkit 与 ReActAgent 装配
│   │   ├── LifecycleMiddleware.java # AgentScope 5 阶段生命周期钩子中间件（风控拦截与 HITL 触发）
│   │   ├── SchemaRepairMiddleware.java # 模式自愈中间件（拦截 ValidationError 封装 Repair Prompt）
│   │   ├── AgentEventStreamBridge.java # 28 类类型化 AgentEvent 转换为多通道 SSE 桥接器
│   │   ├── AgentScopeCheckpointStore.java # 融合 StateStore 的全量快照、差分与时光倒流实现
│   │   └── AgentScopeWorkflowAdapter.java # 实现 ShoppingAgentPort，~110 行消灭 651 行手搓状态机
│   └── tool/                        # 【系统集成适配器】
│       ├── McpEnterpriseToolAdapter.java # 连接 8081/8082 MCP Server 并提供自动 Mock 回退
│       └── ShoppingAgentTools.java       # 声明式 @Tool 工具方法并执行 Bean Validation 校验
├── controller/
│   └── WorkflowV7Controller.java    # REST API 与 28 类多通道 SSE 控制器
└── AgentScopeApplication.java       # Spring Boot 3.4.1 主启动类（端口 8084）
```

---

## 3. 核心四大技术突破点（第 09 课四大深水区落地）

1. **消灭 651 行手搓状态机胶水（P06、P19）**：
   - 手搓 v5.0 `AgentRuntime` 需要 651 行代码手写 while 循环、THINK/ACT/OBSERVE 状态流转、正则解析与熔断计数。
   - `AgentScopeWorkflowAdapter` 仅需约 **110 行**核心编排代码，多轮决策与工具调用完全由官方 `ReActAgent` 内置接管！
2. **28 类类型化 `AgentEvent` 多通道解耦流（P13~P14）**：
   - 彻底超越 Spring AI 2.0 单一文本 Token 流。
   - 原生输出思考流（Thought）、工具执行流（Tool）、回答流（Answer）与人在回路流（HITL），前端按频道自由订阅渲染。
3. **模式修复自愈循环（Schema Repair Loop，P15~P17）**：
   - 结合 Jakarta Bean Validation 注解，当模型输出格式畸变或参数违规时，`SchemaRepairMiddleware` 自动拦截并生成标准化 Repair Prompt 驱动模型自愈修正。
4. **状态 Checkpointer 与时光倒流分支（Time-Travel Forking，P07~P10）**：
   - 严格物理切分 Chat History、Agent State Store 与 Execution Checkpointer 三级边界。
   - 支持跨进程断点复水（Rehydration）与失败分支时光倒流（Forking）。

---

## 4. 快速启动与验证

详细启动指南见 [docs/v7.0-setup.md](file:///c:/Users/shenyw/Downloads/mimoclaw_workspace (6)/ai_training/enterprise-ai-agent-agentscope/docs/v7.0-setup.md)。

```bash
# 1. 编译与单元测试
mvn clean test

# 2. 启动服务（运行于端口 8084）
mvn spring-boot:run
```
