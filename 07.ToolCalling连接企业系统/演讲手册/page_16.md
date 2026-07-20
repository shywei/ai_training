## Slide Objective
展示 Function Calling 的完整链路和 MCP Client 的 Tool 发现机制。

## Suggested Presentation
`McpProductClient.java` 的 `init()` 方法：启动时连接 MCP Server，发现 Tool，包装为 FunctionCallback。

**① 连接 MCP Server**：`HttpClientSseClientTransport.builder(serverUrl)` 创建 SSE 传输，`McpClient.sync(transport).build()` 创建同步 Client，`mcpClient.initialize()` 执行协议握手。握手过程：Client 发送 `initialize` 请求（包含客户端信息和能力），Server 响应（包含服务端信息和能力），Client 发送 `initialized` 通知。三步完成。

**② 发现 Tool**：`mcpClient.listTools()` 返回 Server 暴露的所有 Tool。每个 Tool 有 name、description、inputSchema。inputSchema 是 JSON Schema 格式，定义了参数结构。

**③ 包装为 FunctionCallback**：遍历 Tool 列表，用 `FunctionCallbackWrapper.builder(function)` 创建 callback。function 是一个 lambda：接收 `Map<String, Object>` 参数，调用 `mcpClient.callTool(new CallToolRequest(toolName, arguments))`，提取结果中的 `TextContent`，返回字符串。

名称加 `mcp_` 前缀——`"mcp_" + tool.name()`。这是为了避免和本地 FunctionCallback 冲突。如果本地也有一个 `queryProductInfo` 函数，不加前缀会冲突。

调用时序：用户提问 → LlmRouter 判断 TOOL → 注册 FunctionCallback → ChatClient 构建 LLM API 请求（自动把 callback 转成 function definitions）→ LLM 返回 tool_call → Spring AI 拦截 tool_call → 找到对应的 FunctionCallback → 执行 → 结果回传 LLM → LLM 生成最终回复。

关键点：LLM 不知道 MCP 的存在。它只看到标准的 function definitions（名称、描述、参数 Schema）。MCP 是 Agent 和 Server 之间的协议，对 LLM 完全透明。

## Key Emphasis
MCP Tool → FunctionCallback → LLM function definitions，这三层转换是自动的。开发者只需要定义 Tool（Server 端）和注册 callback（Client 端），中间的协议转换由 MCP SDK 和 Spring AI 完成。

## Transition
这里有一个设计决策需要讲清楚：save_memory 应该在什么路由下注册？
