## Slide Objective
展示项目架构和代码阅读路线图，让学员知道从哪开始看代码。

## Suggested Presentation
代码阅读路线图，建议按顺序看：

**① LlmRouter.java**：路由入口。理解"怎么判断意图"。代码很短，核心就是一个 Prompt + 一次 LLM 调用。

**② ShoppingAgentController.chat()**：路由 + context + Tool 注册。理解"路由如何影响整个对话处理"。这是最核心的文件——所有逻辑在这里汇聚。

**③ McpKnowledgeClient.java**：MCP Client 实现。理解"怎么连接 MCP Server、怎么发现 Tool、怎么包装为 FunctionCallback"。重点看 `init()` 方法和 `discoverTools()` 方法。

**④ McpServerConfig.java**（knowledge-server）：Server 端 Tool + Resource。理解"怎么定义 Tool、怎么注册 Resource"。重点看 Tool 的 Schema 定义和处理函数。

**⑤ MemoryManager.java**：context 构建。理解"画像、记忆、历史怎么组装成 context"。重点看 `buildContext()` 方法——全部通过 MCP 访问。

**⑥ PromptManager.java**：三段规则。理解"怎么引导 LLM 使用 Tool"。重点看「记忆使用规则」。

**⑦ McpServerConfig.java**（product-server）：企业 Tool 实现。理解"Tool 怎么查 MySQL 返回实时数据"。

v4.0 新增了 3 个文件：`LlmRouter.java`、`McpProductClient.java`、`McpKnowledgeClient.java`。修改了 4 个文件：`ShoppingAgentController.java`、`PromptManager.java`、`MemoryManager.java`、`UserMemoryService.java`。新增了 2 个模块：`mcp-product-server/`、`mcp-knowledge-server/`。

删除了 6 个文件：`KnowledgeController.java`、`KnowledgeService.java`、`RetrievalService.java`、`VectorStoreConfig.java`、`UserMemoryMapper.java`、`UserMemory.java`。这些是 v3.0 的遗留代码，MCP 化后不再需要。agent-runtime 的 pom.xml 移除了 `spring-ai-pgvector-store-spring-boot-starter` 和 `postgresql` 驱动——agent-runtime 不再直接连接 PostgreSQL。

## Key Emphasis
按路线图顺序阅读代码，从路由入口开始，到 MCP Server 结束。不要随机打开文件——那样看不懂整体流程。

## Transition
来看 Agent 构建进度和能力地图。
