## Slide Objective
展示项目架构和代码阅读路线图，让学员知道从哪开始看代码。

## Suggested Presentation
代码阅读路线图，建议按顺序看：

**① LlmRouter.java**：路由入口。理解"怎么判断意图"。代码很短，核心就是一个 Prompt + 一次 LLM 调用。

**② ShoppingAgentController.chat()**：路由 + context + Tool 注册。理解"路由如何影响整个对话处理"。这是最核心的文件——所有逻辑在这里汇聚。

**③ McpProductClient.java**：MCP Client 实现。理解"怎么连接 MCP Server、怎么发现 Tool、怎么包装为 FunctionCallback"。重点看 `init()` 方法中连接与包装为 `McpToolCallback` 的完整逻辑。

**④ McpServerConfig.java**（knowledge-server）：Server 端 Tool + Resource。理解"怎么定义 Tool、怎么注册 Resource"。重点看 Tool 的 Schema 定义和处理函数。

**⑤ MemoryManager.java**：context 构建。理解"画像、记忆、历史怎么组装成 context"。重点看 `buildContext()` 方法——全部通过 MCP 访问。

**⑥ PromptManager.java**：三段规则。理解"怎么引导 LLM 使用 Tool"。重点看「记忆使用规则」。

**⑦ McpServerConfig.java**（product-server）：企业 Tool 实现。理解"Tool 怎么查 MySQL 返回实时数据"。

`v1.4`（对应架构版本 v4.0）新增了 5 个文件：`tool/LlmRouter.java`、`tool/McpProductClient.java`、`tool/McpKnowledgeClient.java`、`tool/McpToolCallback.java`、`tool/ProductApiController.java`。修改了 4 个文件：`ShoppingAgentController.java`、`PromptManager.java`、`MemoryManager.java`、`UserMemoryService.java`。新增了 2 个模块：`mcp-product-server/`、`mcp-knowledge-server/`。

删除了 v3.0 的数据库直连代码：`KnowledgeController.java`、`RetrievalService.java`、`VectorStoreConfig.java`、`UserMemoryMapper.java`、`ProductInfo.java`、`ProductInfoMapper.java`。这些是第六课直连架构的代码，MCP 化后全部下沉到两个 MCP Server，agent-runtime 的 `pom.xml` 移除了 `spring-ai-pgvector-store-spring-boot-starter` 和 `postgresql` 驱动——agent-runtime 不再直接连接 PostgreSQL。

## Key Emphasis
按路线图顺序阅读代码，从路由入口开始，到 MCP Server 结束。不要随机打开文件——那样看不懂整体流程。

## Optional Extension
**学员问：几十个文件，从哪读起最快看懂一次完整调用？**
答：顺着一次真实请求的调用序读，不要按文件名顺序读。
Demo 做法：按本页路线图——`LlmRouter`（怎么判意图）→ `ShoppingAgentController`（路由如何决定注入与注册）→ `McpKnowledgeClient`（协议 Client 怎么发现/包装 Tool）→ 两端 `McpServerConfig`（Tool/Resource 怎么定义）→ `MemoryManager`（context 怎么拼）→ `PromptManager`（三段规则）；一遍走完主链，再回头补旁路（`GuardrailsService`、`LlmInteractionLogger`）。
生产做法：读代码时顺手给每步标注治理挂载点——超时设在哪、审计埋在哪、幂等在哪认——对照治理五件套找缺口，读完代码就得到一张缺口清单。取舍：先主链后旁路，比逐文件通读更不容易丢流程。

## Take Away
读陌生 Agent 代码的顺序固定为：路由 → 编排入口 → 协议 Client → Server 端 Tool → context 组装 → Prompt 规则——顺着一次调用走完主链再补旁路，才不会把胶水层误当核心。

## Why This Slide Matters
跳过这页，学员会随机打开文件逐个啃，把 Client 包装层的胶水当核心逻辑研究——读完拼不出一次调用的完整链路，接手维护时改错层。

## Transition
先别急着看能力地图——下一页把这门课的关键断言逐条变成动手实验（8 个验证点），验完再回来对号入座。
