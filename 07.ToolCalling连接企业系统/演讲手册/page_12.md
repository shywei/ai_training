## Slide Objective
展示 Demo 中 MCP Server 的实际代码和 Tool 注册方式。

## Suggested Presentation
两个 MCP Server 的实现。

**mcp-product-server**（端口 8081）：`McpServerConfig.java` 中定义了 3 个 Tool。每个 Tool 用 `new McpSchema.Tool(name, description, schema)` 创建，schema 是 JSON Schema 字符串。然后用 `McpServerFeatures.SyncToolSpecification` 把 Tool 和处理函数绑定。处理函数接收 `(exchange, request)` 参数，从 `request.arguments()` 取参数，调用 `ProductRepository` 查 MySQL，返回 `McpSchema.CallToolResult`。

`ProductRepository.java` 用 `JdbcTemplate` 查 MySQL 的 `product` 表。替代了原来的 `ProductData.java`（内存硬编码）。这是一个升级——从模拟数据变成真实数据库查询。

**mcp-knowledge-server**（端口 8082）：4 个 Tool + 1 个 Resource。Tool 注册方式和 product-server 一样。Resource 用 `McpSchema.ResourceTemplate` 定义 URI 模板（`user://{userId}`），用 `McpServerFeatures.SyncResourceSpecification` 绑定读取回调。回调从 URI 中提取 userId，调用 `UserProfileService` 查 MySQL，返回 JSON。

这里有个设计决策：Resource 的 URI 模板用 `user://{userId}`，而不是 `user://get?userId=xxx`。URI 模板是 MCP 协议推荐的风格——RESTful、语义清晰。Client 看到 `user://U001` 就知道这是在读取用户 U001 的画像。

Server 端代码很简洁——定义 Tool/Resource，注册到 McpServer，完成。复杂的业务逻辑在 Service 层（`VectorSearchService`、`MemoryService`、`UserProfileService`）。

## Key Emphasis
MCP Server 的职责是"暴露能力"，不是"实现业务"。业务逻辑在 Service 层，MCP 层只做协议转换。这是关注点分离。

## Transition
MCP Server 暴露了 Tool，Agent 怎么知道该用哪个？需要路由机制。
