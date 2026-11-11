## Slide Objective
展示 Function Calling 的完整链路和 MCP Client 的 Tool 发现机制。

## Suggested Presentation
`McpProductClient.java` 的 `init()` 方法：启动时连接 MCP Server，发现 Tool，包装为 FunctionCallback。

**① 连接 MCP Server**：`HttpClientSseClientTransport.builder(serverUrl)` 创建 SSE 传输，`McpClient.sync(transport).build()` 创建同步 Client，`mcpClient.initialize()` 执行协议握手。握手过程：Client 发送 `initialize` 请求（包含客户端信息和能力），Server 响应（包含服务端信息和能力），Client 发送 `initialized` 通知。三步完成。
> **双轨制技术对照**：这里的 `mcpClient.initialize()` 属于 Spring AI 1.0.0-M4 的 Demo 实践基线（有状态 SSE 长连接模式）；在 MCP 2026 最新无状态演进规范中，无状态 HTTP POST 交互模式已**彻底免除显式的 initialize 握手**，请求天然无状态自包含。

**② 发现 Tool**：`mcpClient.listTools()` 返回 Server 暴露的所有 Tool。每个 Tool 有 name、description、inputSchema。inputSchema 是 JSON Schema 格式，定义了参数结构。在 2026 最新规范中，服务发现进一步升级为 `server/discover` 统一端点，一次性获取包含元数据与能力协商的完整视图。

**③ 包装为 FunctionCallback**：遍历 Tool 列表，为每个 Tool `new McpToolCallback(...)` 创建 callback——不是 `FunctionCallbackWrapper`，而是独立的桥接类 `tool/McpToolCallback.java`，直接实现 Spring AI 的 `FunctionCallback`，参数 Schema 直接用 MCP Tool 自带的 `inputSchema`（`getInputTypeSchema()` 返回它）。`call()` 接收 JSON 字符串参数、转成 `Map<String, Object>`，调用 `mcpClient.callTool(new CallToolRequest(toolName, arguments))`，提取结果中的 `TextContent`，返回字符串。

名称加 `mcp_` 前缀——`"mcp_" + tool.name()`。这是为了避免和本地 FunctionCallback 冲突。如果本地也有一个 `queryProductInfo` 函数，不加前缀会冲突。

调用时序：用户提问 → `LlmRouter` 判断 `TOOL` → `ShoppingAgentController` 通过 `clientBuilder.functions(mcpProductClient.getFunctionCallbacks())` 注册工具 → `ChatClient` 构建 LLM API 请求（注意：Java 代码里的 `.functions(...)` 只是 Spring AI 1.0.0-M4 的 Builder 方法命名，底层序列化为 OpenAI 协议标准 `"tools"` 数组发给大模型；Spring AI 1.0 GA 已升级为 `.tools(...)`）→ LLM 返回 `tool_calls` → Spring AI 拦截 `tool_calls` → 找到对应的 `McpToolCallback` → 运行时入参安全校验 → 通过 MCP Client 远程调用 MCP Server → 结果回传 LLM → LLM 生成最终回复。

关键点：**严守执行权力边界**——LLM 不知道 MCP 的存在，更绝不执行实际代码。它只看到标准的 OpenAI `"tools"` 决策契约；真正的入参白名单校验、物理网络调用、沙箱隔离与异常捕获完全由 Agent Runtime 独占闭环。

## Key Emphasis
MCP Tool → FunctionCallback → LLM function definitions，这三层转换是自动的。开发者只需要定义 Tool（Server 端）和注册 callback（Client 端），中间的协议转换由 MCP SDK 和 Spring AI 完成。

## Optional Extension
**学员问：MCP Server 挂了，Agent 应该崩掉还是硬扛？**
答：降级，不崩。Tool 是能力增强，不是可用性依赖——Server 挂了 Agent 应该退化成"没有工具的对话"，而不是整体不可用。
Demo 做法：`McpProductClient.init()` 连接失败只打日志、置 `connected=false`，`getFunctionCallbacks()` 返回空列表，Agent 正常对话只是查不了价格；`ensureConnected()` 懒重连带 10 秒冷却，Server 重启后自动恢复。
生产做法：连接状态打点告警（不能只等用户反馈"查不到价格"）+ 失败计数熔断（第八课 `ErrorRecovery`：连续失败 5 次熔断 60 秒、到期自动恢复探测）+ 明确的降级话术（告诉用户"价格暂时查不到"，而不是编一个价格）。取舍：降级期功能受损但服务活着，事故等级比整体不可用低一档。

## Take Away
MCP Client 的连接失败必须表现为"功能降级"而不是"服务不可用"——Tool 列表为空时 Agent 要能带着无工具状态继续对话，重连要带冷却避免雪崩。

## Why This Slide Matters
跳过这页，学员会把 MCP 连接做成启动硬依赖——Server 一次重启或一次网络抖动拖垮整个 Agent 服务，Tool 的可用性问题升级成业务的可用性问题。

## Transition
Function Calling 与 MCP 的机制到这里讲完了——但 Agent 互操作不止 MCP 一个协议，先看协议全景（page_16），把 MCP/A2A/AG-UI 的边界立住；再回到应用层，看这些能力怎么改变 Agent 架构，以及那个重要的设计决策：记忆保存是系统行为还是 LLM 行为。
