## Slide Objective
讲解 MCP 的四大核心概念（Tool / Resource / Prompt / Sampling），让学员理解 MCP 协议的完整能力范围。Demo 覆盖了 Tool 和 Resource，Prompt 和 Sampling 仅做概念介绍。

## Suggested Presentation
MCP 是三层架构。

**Client Layer**：`McpClient` / `McpSyncClient`。负责连接 Server、发现 Tool、调用 Tool。Demo 中的 `McpProductClient` 和 `McpKnowledgeClient` 都是 MCP Client。看代码：`McpClient.sync(transport).build()` 创建 Client，`mcpClient.initialize()` 执行协议握手，`mcpClient.listTools()` 发现 Tool，`mcpClient.callTool(request)` 调用 Tool。

**Transport Layer**：通信传输层。MCP 基于 JSON-RPC 2.0 协议（和 JSON API 类似）。传输方式有三种：

**SSE（HTTP Server-Sent Events）**：基于 HTTP，适合远程微服务部署。Server 端用 `HttpServletSseServerTransportProvider`，Client 端用 `HttpClientSseClientTransport`。Demo 用的就是这种方式。SSE 是单向的（Server → Client），Client 发消息用普通 HTTP POST。这个组合既支持 Server 推送（SSE 流），又支持 Client 发送（POST 请求）。

**stdio（标准输入输出）**：适合本地进程通信。Claude Desktop 调用本地 MCP Server 用的就是 stdio。不需要网络，延迟最低。但只能本地用，不能远程。

**自定义传输**：实现 `Transport` 接口，适合特殊需求（如 WebSocket、gRPC）。Demo 没用。

**Server Layer**：`McpServer` / `McpSyncServer`。负责暴露 Tool、Resource、Prompt。Server 启动后通过 Servlet 暴露 SSE 端点，Client 通过 HTTP 连接。

Servlet 注册的关键代码：`new ServletRegistrationBean<>(tp, "/*")`。注册到 `/*` 是因为 `HttpServletSseServerTransportProvider` 需要处理两个路径：`/sse`（SSE 连接）和 `/mcp/message`（消息接收）。如果只注册到 `/sse`，消息端点会 404——这是 Demo 中踩过的坑。

传输方式选型看部署场景：远程微服务用 SSE（Demo 的选择），本地工具用 stdio（Claude Desktop 的选择），特殊需求用自定义。大多数企业场景用 SSE——MCP Server 作为独立服务部署，Agent 通过 HTTP 调用。

## Key Emphasis
三层架构让 MCP 有清晰的职责分离。Client 不关心 Server 用什么语言实现，Server 不关心 Client 是什么框架。Transport 层是中间的桥梁。

## Demo 与生产的差距
Demo 用 SSE 基于 HTTP/1.1，生产中可能需要 HTTP/2 或 gRPC 提升性能。Demo 没有做连接池管理，生产中 Client 需要维护到多个 Server 的连接池。Demo 没有做 TLS，生产中 MCP 通信必须加密。

## Transition
看 Demo 中的两个 MCP Server 具体怎么实现。
