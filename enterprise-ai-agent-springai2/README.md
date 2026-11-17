# Enterprise AI Agent - Spring AI 2.0 现代基础设施对照工程

> **课程体系定位**：本工程严格遵循《课程设计总纲》第 2.2、2.3、2.4 节与第 3.3 节（第 09 课《从手搓到框架：框架的边界》）规范，作为 **全课程三套工程同屏对照体系中的第 2 套工程（Git Tag: `v6.0-springai2`）**。

---

## 1. 工程定位与三套工程同屏对照体系

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       全课程三套工程同屏对照架构体系                               │
└─────────────────────────────────────────────────────────────────────────────┘
  ① enterprise-ai-agent 【手搓原版】
     ├── 技术栈: Spring AI 1.0.0-M4 + Spring Boot 3.4.1 + Java 17
     ├── 端口: 8080 (永久冻结于 commit 0839d6b / v5.0-workflow)
     └── 定位: 原理实验原点，展示手写底层机理与胶水代码

  ② enterprise-ai-agent-springai2 【现代基础设施版（本项目）】
     ├── 技术栈: Spring AI 2.0.2 GA + Spring Boot 4.0.0 + Java 21
     ├── 端口: 8083 (独立新建，模块并排映射)
     └── 定位: 现代基础设施演进，展示 ChatClient.builder()、@Tool、Advisors 与内置 MCP
              【看清：API 变优雅了，但架构编排与状态胶水层依然需要开发者手写】

  ③ enterprise-ai-agent-agentscope 【高级应用框架版】
     ├── 技术栈: AgentScope Java 2.0.0 GA + Spring Boot 3.4.1 + Java 17+
     ├── 端口: 8084 (第 09 课后期引入)
     └── 定位: 企业级多智能体生产力底座，展示 HarnessAgent、28类事件流与持久化
              【看清：框架如何消灭胶水层，提升研发吞吐量】
```

---

## 2. 技术栈升级基线（三层联动现代化升级）

本项目将技术栈整体升级至 Java 现代生态最新标准：

- **JDK 基线**：**Java 21 LTS**（开启虚拟线程 Virtual Threads 并发长等待优化，采用模式匹配与记录模式解构）；
- **Spring Boot 基线**：**Spring Boot 4.0.0**（基于 Spring Framework 7.0 模块化底座与原生 AOT 优化）；
- **AI 基础设施基线**：**Spring AI 2.0.2 GA**（BOM: `org.springframework.ai:spring-ai-bom:2.0.2`，包含 OpenAI Starter、内置 MCP Client 与 Validation）；
- **存储与中间件**：Redis 6+（快照热存）、PostgreSQL 14+ / pgvector（执行快照冷存与向量知识）、MySQL 8.0（画像与商品库）；
- **服务端口**：**8083**（不与原工程 8080 冲突，支持双工程同时启动同屏对照）。

---

## 3. 六边形防套牢架构（Hexagonal Architecture）目录结构

为了彻底打破“业务逻辑被特定 AI 框架注解深度绑架”的架构腐化，本工程严格遵循 **六边形架构（Ports & Adapters，对应第 09 课 P20~P22）**：
- **`domain` 领域核心**：纯 Java POJO，零框架依赖，保持与后续 AgentScope 工程 100% 一致；
- **`port` 六边形端口**：定义清晰的出入站业务契约；
- **`adapter.springai2` 框架适配器**：将 Spring AI 2.0 基础设施能力封装为端口实现；
- **`controller` 外部驱动适配器**：对外提供 REST + SSE 交互端点。

```
enterprise-ai-agent-springai2/
├── pom.xml                                      # Java 21 + Spring Boot 4.0 + Spring AI 2.0.2 GA
├── README.md                                    # 本工程综合说明文档
├── docs/
│   ├── v6.0-setup.md                            # 保姆级部署与验证实操指南
│   ├── v6.0-springai2-decisions.md              # 深度架构决策与 8 大模块对照分析
│   └── schema-checkpoint.sql                    # PostgreSQL Checkpointer 检查点建表脚本
└── src/
    ├── main/
    │   ├── java/com/example/agent/
    │   │   ├── SpringAi2AgentApplication.java   # Spring Boot 主启动类 (端口 8083)
    │   │   │
    │   │   ├── domain/                          # 【纯领域核心】零框架依赖，跨工程通用
    │   │   │   ├── ShoppingIntent.java          # 导购意图与选品打分领域模型
    │   │   │   ├── AgentCheckpoint.java         # 状态快照模型 (划分三级存储边界，包含 CallStack/游标/差分)
    │   │   │   ├── ToolOrderRequest.java        # 强契约工具入参模型 (带 Jakarta Bean Validation 强约束)
    │   │   │   ├── ShoppingDomainService.java   # 纯领域业务规则 (风控硬规则、高额阈值、选品加权排序)
    │   │   │   ├── WorkflowState.java           # 7 状态机枚举 (PLANNING, EXECUTING, WAITING_USER 等)
    │   │   │   ├── WorkflowStep.java            # 执行步骤快照模型
    │   │   │   ├── WorkflowResponse.java        # 统一响应传输对象
    │   │   │   └── WorkflowStreamEvent.java     # SSE 实时事件传输对象
    │   │   │
    │   │   ├── port/                            # 【六边形出入站端口】零特定 AI 框架绑定
    │   │   │   ├── ShoppingAgentPort.java       # 入站端口 (任务执行、SSE 流式、断点复水、人在回路确认)
    │   │   │   ├── CheckpointStorePort.java     # 出站端口 (全量快照、增量差分、复水恢复、分支回滚)
    │   │   │   └── EnterpriseToolPort.java      # 出站端口 (商品查询、价格库存、订单创建、知识检索)
    │   │   │
    │   │   ├── adapter/springai2/               # 【Spring AI 2.0.2 GA 专属适配器】
    │   │   │   ├── SpringAi2ChatConfig.java     # ChatClient.builder() 流式链式构建配置
    │   │   │   ├── SpringAi2ToolAdapter.java    # @Tool 声明式工具、Bean Validation 与内置 MCP
    │   │   │   ├── GuardrailsCallAdvisor.java   # CallAdvisor 拦截流水线 (前置 PII 脱敏/防注入，后置敏感词审查)
    │   │   │   ├── TelemetryStreamAdvisor.java  # StreamAdvisor 流式遥测拦截器 (TTFT、Token 速率打点)
    │   │   │   ├── SchemaRepairCallAdvisor.java # Schema Repair Loop 自愈纠错拦截器
    │   │   │   ├── RedisPostgresCheckpointAdapter.java # Checkpointer 状态快照持久化适配器
    │   │   │   └── SpringAi2WorkflowAdapter.java# ⭐最核心教学对比类：实现 ShoppingAgentPort
    │   │   │
    │   │   ├── controller/
    │   │   │   └── WorkflowV6Controller.java    # REST + SSE 控制器，支持断点复水与时光倒流
    │   │   └── config/
    │   │       ├── CorsConfig.java              # 跨域配置
    │   │       └── RedisConfig.java             # Redis 模板配置
    │   └── resources/
    │       ├── application.yml                  # 基础配置 (端口 8083, SiliconFlow, Redis, PostgreSQL)
    │       └── schema-checkpoint.sql            # 检查点建表脚本
    └── test/java/com/example/agent/
        └── SpringAi2HexagonalTest.java          # 领域核心、风控规则、Bean Validation 与 Checkpoint 单元测试
```

---

## 4. 核心教学对照亮点（手搓原版 v5.0 vs Spring AI 2.0）

1. **单次调用代码断崖式精简（651 行 $\to$ 240 行）**：
   - 原手搓 `AgentRuntime.java` 充斥着手写正则表达式解析 `TOOL_CALL:`、手动反序列化入参、手动拼装 Prompt 模板等初级胶水；
   - Spring AI 2.0 通过 `ChatClient.builder()`、`@Tool` 声明式注解和 `CallAdvisor` 责任链，让底层协议与工具调用闭环完全自动化。
2. **三级存储边界与 Checkpointer 模式（深度深水区 1）**：
   - 彻底理清并物理分离：**Chat History（短期对话文本）**、**Agent State Store（用户画像全局事实）** 与 **Execution Checkpointer（执行栈游标与工作区快照）**；
   - 真正实现 Pod 漂移或重启后的 **断点复水（Rehydration）** 与 **时光倒流（Time-Travel Forking）**。
3. **强契约约束与自愈纠错循环（Schema Repair Loop，深度深水区 3）**：
   - 工具入参采用 Jakarta Bean Validation 注解声明强契约；
   - `SchemaRepairCallAdvisor` 捕获畸变异常并自主向大模型发起 Repair Prompt 纠错，消灭脆弱的降级方案。
4. **终极架构反思（框架的边界）**：
   - **“Spring AI = Servlet API（基础设施）；AgentScope = Spring MVC（应用框架）”**；
   - Spring AI 极大简化了单次模型与工具调用的胶水，但面对多步状态机（7状态）、TaskPlanner、人在回路（WAITING_USER）以及多智能体协同，依然需要开发者在这层手写复杂循环；这正是引入第 2 级阶梯 `AgentScope Java 2.0` 的根本动力！
