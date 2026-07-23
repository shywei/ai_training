## Slide Objective
讲解 MCP 的三层架构（Client / Transport / Server）和传输方式选型——学员需要理解 MCP 的通信机制，才能看懂 Demo 中的 transport 配置。

## Suggested Presentation
MCP 是三层架构。

**Client Layer**：`McpClient` / `McpSyncClient`。负责连接 Server、发现 Tool、调用 Tool。Demo 中的 `McpProductClient` 和 `McpKnowledgeClient` 都是 MCP Client。

**Transport Layer**：通信传输层。MCP 基于 JSON-RPC 2.0 协议（和 JSON API 类似）。传输方式有三种：

- **SSE（HTTP Server-Sent Events）**：基于 HTTP，适合远程微服务部署。Server 端用 `HttpServletSseServerTransportProvider`，Client 端用 `HttpClientSseClientTransport`。Demo 用的就是这种方式。
- **stdio（标准输入输出）**：适合本地进程通信。Claude Desktop 调用本地 MCP Server 用的就是 stdio。不需要网络，延迟最低。
- **自定义传输**：实现 `Transport` 接口，适合特殊需求（如 WebSocket、gRPC）。

**Server Layer**：`McpServer` / `McpSyncServer`。负责暴露 Tool、Resource、Prompt。Server 启动后监听 SSE 端点，Client 通过 HTTP 连接。

传输方式选型看部署场景：远程微服务用 SSE（Demo 的选择），本地工具用 stdio（Claude Desktop 的选择），特殊需求用自定义。大多数企业场景用 SSE——MCP Server 作为独立服务部署，Agent 通过 HTTP 调用。

Demo 中的 SSE 端点配置：Server 端 `messageEndpoint("/mcp/message")`，Servlet 注册到 `/mcp/sse`。Client 端 `HttpClientSseClientTransport.builder(serverUrl)` 连接。

## Key Emphasis
三层架构让 MCP 有清晰的职责分离。Client 不关心 Server 用什么语言实现，Server 不关心 Client 是什么框架。Transport 层是中间的桥梁。

## Transition
看 Demo 中的两个 MCP Server 具体怎么实现。
