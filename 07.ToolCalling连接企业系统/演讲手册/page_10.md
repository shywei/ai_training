## Slide Objective
讲解 MCP 的核心角色模型与能力矩阵分类（Server Features vs Client Features），让学员掌握 MCP 协议的规范架构。Demo 覆盖了 Server Features 中的 Tools 和 Resources，Prompts 作为模板化参考，Client Features（Elicitation / Roots / Sampling）做架构演进剖析，并**重点说明 2026-07-28 最新规范中 Roots、Sampling、Logging 的弃用状态及替代方案**。

## Suggested Presentation
各位学员，MCP 协议将通信两端的能力划分为清晰的两大类别：**Server Features（服务端能力）** 与 **Client Features（客户端特性）**：

### 一、三大核心 Server Features（服务能力基石）
1. **Tools（工具 — 模型控制执行）**：LLM 可调用的函数，用于搜索、查询、写入、计算。核心特征是 **LLM 决定何时调用**。Server 暴露 `name`、`description`、`inputSchema`（JSON Schema），Client 通过 `callTool(name, args)` 触发执行。本课 `v1.4` Demo 中，`mcp-product-server` 的 `queryProductInfo` / `queryPrice` / `queryStock` 和 `mcp-knowledge-server` 的 `search_knowledge` / `search_memory` / `save_memory` / `index_knowledge` 都是 Tool。
2. **Resources（资源 — 应用主动读取）**：Client 可直接按 URI 读取的结构化数据或文件（如配置、用户画像、文档内容）。核心特征是 **Client 主动读取**，不需要 LLM 先做一次 tool_call 决策。Server 通过 `ResourceTemplate` 暴露 URI 模板（如 `user://{userId}`），Client 调用 `readResource("user://U001")` 直接拿到 JSON 画像。本课 `v1.4` Demo 中用户画像就是作为 Resource 暴露的。
3. **Prompts（提示词模板 — 统一声明复用）**：Server 预定义的标准化 Prompt 模板（带参数），供 Client 拉取复用。例如企业知识服务统一暴露 `analyze-review` 模板，多个 Agent 共享同一套高质量提示词，无需在每个 Agent 里复制粘贴。

### 二、Client Features 与 2026-07-28 规范演进弃用说明
在客户端特性中，包括了 Server 向 Client 请求协助的能力：
- **Elicitation**：Server 请求客户端引导终端用户输入补充信息（如缺少必要参数时的人机交互补充）；
- **Sampling（反向采样推理）** 与 **Roots（工作区目录暴露）**：早期规范允许 Server 请求 Client 代调 LLM 推理（Sampling）或读取 Client 宿主本地目录（Roots）。
- **⚠️ 2026-07-28 规范弃用要点**：
  在最新的 MCP 2026-07-28 规范中，**Roots、Sampling 以及协议级 Logging 已经正式进入弃用（Deprecated）阶段！**
  - **为什么弃用 Sampling？** 因为 Server 反向借用 Client 模型推理打破了清晰的单向依赖，导致安全鉴权与 Token 计费极其混乱；现代 Agent 架构要求推理职责全部收拢在 Host/Agent Runtime 统一编排；
  - **为什么弃用 Roots？** 工作区权限应由宿主环境通过隔离沙箱安全管控，而非通过协议层随意穿透暴露给外部 Server；
  - **为什么弃用 Logging？** 协议内置 Logging 功能简陋，生产环境已由标准 OpenTelemetry 分布式追踪全面接管。

## Key Emphasis
1. **协议官方分类**：Server Features 包括 Tools、Resources、Prompts；Client Features 包括 Elicitation、Roots、Sampling。
2. **2026-07-28 弃用认知**：严禁把 Roots、Sampling、Logging 当成现代项目的主推特性，它们已被最新规范弃用，由应用与架构层统一自治。
3. **Tasks 扩展独立**：Tasks 从核心通信协议中拆出，作为独立的 Extension 扩展机制存在。

## Optional Extension
**学员问：既然 Tools 什么都能查，为什么 MCP 还要单独设计 Resources？**
答：为了省去不必要的 LLM 决策轮次和 Token 消耗。
Demo 做法：用户画像是每次对话构建 context 都必定要读的数据，如果做成 Tool，LLM 每轮都要先多走一次 `tool_calls` 往返去查画像，既增加延迟又可能漏调；做成 Resource（`user://{userId}`），`MemoryManager.buildContext()` 在会话入口由代码直接调用 `McpKnowledgeClient.readUserProfile(userId)` 一步读出。
生产做法：固定必读上下文走 Resource，按需动态检索/执行走 Tool。page_14 会专门用一页决策树对比两者的选型边界。

## Take Away
记住 MCP 四大原语的分工：Tools 给模型长手脚（LLM 触发）、Resources 给应用喂上下文（Client 直读）、Prompts 共享模板、Sampling 反向借模型推理。

## Why This Slide Matters
跳过这页，学员会把 MCP 窄化为"又一个 Function Calling 包装器"，忽略 Resource 在确定性上下文加载上的低延迟优势，导致把所有数据读取都塞进 Tool 让 LLM 绕弯路。

## Transition
清楚了 MCP 暴露的四大能力，那 Client 和 Server 之间到底是怎么通信的？我们看下一页（page_11）：MCP 三层架构与 Transport 传输层演进。
