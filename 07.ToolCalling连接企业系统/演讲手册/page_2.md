## Slide Objective
展示第七课的完整知识结构，8 章内容（P3–P34），给学员全局路线图。

## Suggested Presentation
第七课分八个章节，完整覆盖从协议机制到企业治理与工程验证。

前四章是第七课的核心能力层：
- **01 为什么需要 Tool Calling（P3–P4）**：回顾第六课、剖析 LLM 天生局限、引入 Function Calling 核心思想。
- **02 Function Calling（P5–P8）**：6 步调用流程、OpenAI API 标准 `"tools"` / `"tool_calls"` 协议格式与 `strict: true` 结构化参数约束、Spring AI 两种实现方式、ReAct 模式边界。
- **03 MCP 协议（P9–P13）**：为什么需要标准协议、MCP 四大能力、三层架构与 Transport 传输层演进（`stdio` / `HTTP+SSE` / `Streamable HTTP`）、双 MCP Server 实现、行业生态与安全防护。
- **04 Tool vs Resource 与协议全景（P14–P17）**：Tool 与 Resource 选型决策树、Function Calling 完整调用链路、2026 Agent 互操作协议全景（MCP / A2A / AG-UI）、从能力到应用过渡。

第五、六章是有了 Tool/MCP 之后，第六课知识体系的架构升级：
- **05 LLM 路由与大规模工具治理（P18–P19）**：三种路由方案对比、企业大规模工具治理（Tool RAG 动态工具检索）、`LlmRouter` 代码实现与 context 按需构建。
- **06 记忆与数据架构演进（P20–P22）**：记忆系统设计（系统自动沉淀 vs LLM 检索使用）、数据分层全面 MCP 化、context 四课演进与 Prompt 三段规则。

第七、八章是企业生产治理与实战验证：
- **07 企业治理与互操作协议（P16, P23–P29）**：企业治理五件套（限流、幂等、超时、补偿、审计）逐项拆解、对照本课 `v1.4` 真实代码现状与十二项生产差距。
- **08 架构验证与实战总结（P30–P34）**：`v1.4` 项目架构与代码阅读路线图、8 个可在 Demo 亲手验证的实验点、Agent 构建进度与能力地图、8 道课后练习及第八课预告。

## Key Emphasis
8 章结构层层递进：核心能力（01–04）→ 架构升级（05–06）→ 企业治理（07）→ 验证与总结（08）。学员看到目录就知道今天学什么、每部分对应哪些页码。

## Optional Extension
**学员问：八章都要学吗？我们只想给内部系统加一两个工具。**
答：按"工具数量 × 消费方数量"砍范围，别按章节完整性砍。
Demo 做法：本课 `v1.4` Demo 是两个 MCP Server（`mcp-product-server:8081`、`mcp-knowledge-server:8082`）加一个 `agent-runtime`——双 Server 是为了演示"一次开发、多处复用"和独立部署这两个收益点，工具只有一个消费方时这不是必选项。
生产做法：一两个工具、单消费方 → Function Calling 直连 + 治理三件（超时/幂等/审计）就能上；工具多起来、或有多 Agent/多端消费 → 再上 MCP 标准化，平台期由 MCP Gateway 统一注册/鉴权/审计（第十一课支柱一）。取舍：MCP 摊薄的是集成与治理成本，不是调用性能——只有一个消费方时它摊不薄。

## Take Away
学课程按"能力（01–04）→ 应用（05–06）→ 治理（07）→ 验证（08）"的依赖顺序走，做项目时反过来按消费方数量倒着定范围——协议是摊薄集成成本的，单消费方直连更轻量，但企业治理五件套无论直连还是 MCP 都不能省。

## Why This Slide Matters
跳过这页，学员会把各章当并列知识点乱序学，或在只有一个工具、一个消费方时就搭 MCP 全家桶——架构先行于问题，后面每个组件都在增加运维面。

## Transition
先看第一章（page_3）：回顾第六课 Agent 懂了什么，以及为什么还需要 Tool Calling。
