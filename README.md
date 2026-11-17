# 企业级 AI 原生软件工程与生产级 Agent 架构实战全景体系

> **面向中高级工程师、架构师与技术管理者的高密度企业级 AI 课程与实战代码库**  
> 体系覆盖从**单兵 AI 辅助编程能力跃迁**，到**企业级存量复杂工程（Brownfield）治理**，再到**从 0 到 1 手搓工业级导购 Agent 系统**与**多 Agent 平台化运营**的全生命周期。

---

## 课程体系大纲

课程体系采用**双轨学习路径设计**：
- **普通学员标准通识主线（01 $\rightarrow$ 02 $\rightarrow$ 03 $\rightarrow$ 04 ~ 08）**：01、02、03 构成必修基石，学完即可顺畅进入 04~08 生产级 Agent 实战开发。
- **项目 Leader 与高级开发深入学习支线（01B 与 02B，可跳过 / 选修）**：深入底层工程原理、算力显存测算与团队工程研发规范。

| 课程编号 | 课程名称 | 核心定位与一句话心智 | 学习路径属性 | 交付规格 | 对应实战环境 / Git Tag |
|:---:|:---|:---|:---:|:---:|:---:|
| **01** | **AI认知入门** | 借古鉴今重塑技术世界观，跨越传统编码鸿沟，破冰祛魅与 .jar 包类比，顺畅拥抱 AI 原生编程时代。 | **标准通识必修** | 22P 课件 + 22篇手册 | 纯理论认知破冰与历史演进 (Tag: `v1.0`) |
| **01B** | **AI认知进阶** | 扒开模型黑盒看齿轮运转，深入底层工程原理与算力机理（Scaling Laws、BPE Tokenizer、RoPE、KV Cache、4阶段训练、显存黄金公式、vLLM/SGLang 显存调度），支撑企业技术选型与算力测算。 | **项目Leader/高工选修**<br>*(普通学员可跳过)* | 22P 课件 + 22篇手册 | 纯理论底层机理与微观计算图 (Tag: `v1.0`) |
| **02** | **AI辅助编程入门** | 掌握 AI 辅助编程工程方法（Prompt、Rules、Hooks、Skills、MCP、OpenCode 实践、上下文管理），单兵研发提效。 | **标准通识必修** | 22P 课件 + 22篇手册 | OpenCode 终端环境与基础配置 (Tag: `v1.0`) |
| **02B** | **AI辅助编程进阶** | 团队工程规范与存量工程治理：规格驱动开发 SDD、EARS 规范、Multi-Agent 并行重构、Git Worktree 隔离研发工作流与防作弊 Hook 护栏。 | **项目Leader/高工选修**<br>*(普通学员可跳过)* | 21P 课件 + 21篇手册 | OpenCode 独立靶场 (`demo-opencode-enterprise`) |
| **03** | **AI Agent架构认知入门** | Agent 不是模型，而是系统；建立 Agent 应用基本架构认知（感知、规划、记忆、工具、状态机、企业治理），为后续实战课程打牢基石。 | **标准通识必修** | 22P 课件 + 22篇手册 | 纯理论与架构全景认知 (Tag: `v1.0`) |
| **04** | **LLM交互工程** | API 决定能力边界，Streaming 流式传输与规则路由决定企业接入质量。 | **实战主线必修** | 17P 课件 + 17篇手册 | `enterprise-ai-agent` (Tag: `v1.1` / `v1.1.1`) |
| **05** | **Memory记忆系统** | Memory 不是简单的聊天记录，而是企业为 AI 建立的分层记忆生命周期系统。 | **实战主线必修** | 19P 课件 + 19篇手册 | `enterprise-ai-agent` (Tag: `v1.2`) |
| **06** | **Knowledge企业知识系统与RAG** | Knowledge 不是散落的文档，而是经过 Pipeline 治理后能被 AI 高保真召回的企业资产。 | **实战主线必修** | 23P 课件 + 23篇手册 | `enterprise-ai-agent` (Tag: `v1.3`) |
| **07** | **ToolCalling连接企业系统** | Tool 不是单纯的 HTTP 接口，而是 AI 与企业核心资产之间严格受控的安全边界。 | **实战主线必修** | 34P 课件 + 34篇手册 | `enterprise-ai-agent` (Tag: `v1.4`) |
| **08** | **Workflow构建可靠的企业Agent** | Workflow 不是固定流程，而是通过状态机与容错兜底让 Agent 在复杂生产中可控运行的机制。 | **实战主线必修** | 26P 课件 + 26篇手册 | `enterprise-ai-agent` (Tag: `v1.5`) |
| **09** | **从手搓到框架：框架的边界** | 手搓理解底层原理，框架提升工程效率；省的不是代码行数，是粘合胶水；守住领域边界，防框架套牢。 | **进阶主线必修** | 24P 课件 + 24篇手册 | `enterprise-ai-agent-springai2` (Tag: `v6.0-springai2`)<br>`enterprise-ai-agent-agentscope` (Tag: `v7.0-agentscope`) |
| **10** *(规划)* | **Multi-Agent 协作网络** | 单 Agent 有复杂度天花板；多 Agent 不是数量堆叠，而是拓扑规划与职责委派。框架没有最好，只有最适合。 | 进阶规划 | 22P 课件 + 22篇手册 | AgentScope vs Embabel vs LangGraph |
| **11** *(规划)* | **企业级 Agent 工程交付：从业务需求到生产验收** | 把业务方的模糊许愿翻译成确定性的状态机契约、红蓝对抗评测集与灰度熔断上线门禁，打通研发到生产的最后一公里。 | 进阶规划 | 24P 课件 + 24篇手册 | 导购 Agent 全链路工程交付实战 |
| **12** *(规划)* | **企业 AI 平台：从单个 Agent 到平台化运营** | 企业最终长期运营与治理的不是零散的 Agent，而是高可用的 AI 原生研发平台。 | 进阶规划 | 22P 课件 + 22篇手册 | 平台全景架构与评测合规体系 |

---

## 目录结构索引

```
ai_training/
├── 01.AI认知入门/                        # 第 01 课: 历史演进、认知重塑、jar包类比与开发者破冰 (22P)
│   ├── index.html                        # 离线全屏 Web 演讲播放器 (支持演讲备注 N / 扩展学习 E)
│   ├── 扩展学习指南.md                    # 课后 3W 深度扩展阅读资料与架构自检题
│   ├── page_1.html ~ page_22.html
│   └── 演讲手册/page_1.md ~ page_22.md
├── 01B.AI认知进阶/                       # 第 01B 课: 底层工程原理、KV Cache、4阶段训练与算力估算 (22P)
│   ├── index.html
│   ├── 扩展学习指南.md
│   ├── page_1.html ~ page_22.html
│   └── 演讲手册/page_1.md ~ page_22.md
├── 02.AI辅助编程入门/                     # 第 02 课: 单兵提效、IDE+CLI 环境与 OpenCode 基础 (22P)
│   ├── index.html
│   ├── 扩展学习指南.md
│   ├── Demo演示步骤.md                    # 基于 OpenCode 的单兵基础演练指南
│   ├── page_1.html ~ page_22.html
│   └── 演讲手册/page_1.md ~ page_22.md
├── 02B.AI辅助编程进阶/                    # 第 02B 课: SDD 规格驱动、存量重构、防篡改 Hook (21P)
│   ├── index.html
│   ├── 扩展学习指南.md
│   ├── Demo演示步骤.md                    # 基于 OpenCode 的企业靶场演练指南
│   ├── page_1.html ~ page_21.html
│   └── 演讲手册/page_1.md ~ page_21.md
├── 03.AI Agent架构认知入门/               # 第 03 课: Agent 架构全景与系统认知 (22P)
│   ├── index.html
│   ├── 扩展学习指南.md
│   ├── page_1.html ~ page_22.html
│   └── 演讲手册/page_1.md ~ page_22.md
├── 04.LLM交互工程/                       # 第 04 课: 协议交互、Streaming、路由与安全护栏 (17P)
│   ├── index.html
│   ├── 扩展学习指南.md
│   ├── Demo演示步骤.md
│   ├── page_1.html ~ page_17.html
│   └── 演讲手册/page_1.md ~ page_17.md
├── 05.Memory记忆系统/                    # 第 05 课: 三层记忆架构、滑动窗口与生命周期沉淀 (19P)
│   ├── index.html
│   ├── 扩展学习指南.md
│   ├── Demo演示步骤.md
│   ├── page_1.html ~ page_19.html
│   └── 演讲手册/page_1.md ~ page_19.md
├── 06.Knowledge企业知识系统与RAG/         # 第 06 课: Chunking、pgvector、HNSW 与检索管线 (23P)
│   ├── index.html
│   ├── 扩展学习指南.md
│   ├── Demo演示步骤.md
│   ├── page_1.html ~ page_23.html
│   └── 演讲手册/page_1.md ~ page_23.md
├── 07.ToolCalling连接企业系统/            # 第 07 课: Function Calling、双 MCP Server 与企业治理 (34P)
│   ├── index.html
│   ├── 扩展学习指南.md
│   ├── Demo演示步骤.md
│   ├── page_1.html ~ page_34.html
│   └── 演讲手册/page_1.md ~ page_34.md
├── 08.Workflow构建可靠的企业Agent/        # 第 08 课: 7状态机、ReAct 引擎、三级容错与可观测 (26P)
│   ├── index.html
│   ├── 扩展学习指南.md
│   ├── Demo演示步骤.md
│   ├── page_1.html ~ page_26.html
│   └── 演讲手册/page_1.md ~ page_26.md
├── 09.从手搓到框架/                       # 第 09 课: 两级阶梯对比、Checkpointer 复水、Middleware 与六边形防套牢 (24P)
│   ├── index.html
│   ├── 扩展学习指南.md
│   ├── Demo演示步骤.md
│   ├── page_1.html ~ page_24.html
│   └── 演讲手册/page_1.md ~ page_24.md
├── dist_ppts/                            # 全套课程导出的 16:9 满铺 2K 超清 .pptx 演示文稿
│   └── 01.AI认知入门.pptx ~ 09.从手搓到框架.pptx
├── enterprise-ai-agent/                  # 第 04-08 课企业级生产演练代码库 (手搓原版 v5.0，端口 8080，永久冻结基准)
│   ├── agent-runtime/                    # 核心运行时微服务 (Spring Boot 3.4.1, 端口 8080)
│   │   └── src/main/java/com/example/agent/
│   │       ├── auth/                     # 用户鉴权与上下文绑定
│   │       ├── core/                     # 04课: 提示词管理 (PromptManager)、流式吐字、结构化输出、敏感词护栏 (GuardrailsService)
│   │       ├── memory/                   # 05课: Redis 短期会话缓冲与 MySQL 长期画像沉淀 (MemoryManager)
│   │       ├── tool/                     # 07课: MCP 客户端接入与工具回调调度 (McpToolCallback, McpProductClient, McpKnowledgeClient)
│   │       └── workflow/                 # 08课: ReAct 循环引擎 (AgentRuntime)、7状态机 (WorkflowState)、任务规划 (TaskPlanner)、三级容错与追踪
│   ├── mcp-product-server/               # 07课: 商品能力微服务 (Spring Boot 3.4.1, 端口 8081, McpServerConfig)
│   ├── mcp-knowledge-server/             # 07课: 知识库检索微服务 (Spring Boot 3.4.1, 端口 8082, VectorSearchService)
│   ├── web/                              # 前端管理与可视化调试界面 (Vue 2.7 + Element UI, 端口 3000)
│   ├── docker/                           # 基础设施容器编排 (MySQL 8.0, Redis 7.0, PostgreSQL 16 + pgvector)
│   └── docs/                             # 历次演进设计决策与数据库 Schema
├── enterprise-ai-agent-springai2/        # 第 09 课第一级阶梯对照工程: Spring AI 2.0.2 GA 现代基础设施版 (端口 8083)
│   ├── src/main/java/com/example/agent/  # 六边形架构: domain + port + adapter.springai2 (ChatClient / @Tool / CallAdvisor / Checkpointer)
│   └── docs/                             # v6.0-setup.md 与 v6.0-springai2-decisions.md
└── enterprise-ai-agent-agentscope/       # 第 09 课第二级阶梯对照工程: AgentScope Java 2.0.0 GA 高阶应用框架版 (端口 8084)
    ├── src/main/java/com/example/agent/  # 六边形架构: 100% 共享 domain/port + adapter.agentscope (ReActAgent / 五阶段 Middleware / 28类 AgentEvent)
    └── docs/                             # v7.0-setup.md 与 v7.0-agentscope-decisions.md
```
