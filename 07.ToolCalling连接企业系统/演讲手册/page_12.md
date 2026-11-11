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

## Optional Extension
**学员问：MCP Server 里的 Tool 处理函数应该写多厚？业务逻辑能不能直接写进去？**
答：处理函数只做"取参 → 调 Service → 包结果"，业务逻辑留 Service 层。这不是分层洁癖，是给治理留挂载点。
Demo 做法：`McpServerConfig` 的 `SyncToolSpecification` 从 `request.arguments()` 取参数，调 `ProductRepository`（MySQL）/ `VectorSearchService`（pgvector）/ `UserProfileService`，结果包成 `CallToolResult`——SQL 和检索逻辑都在 Service/Mapper 层，协议层不含业务。
生产做法：协议层是参数校验、审计埋点、限流的统一挂载点（每个 Tool 一处覆盖），业务 Service 可脱离协议做单元测试；写操作在这里认幂等键。取舍：多一层转发的几毫秒，换所有 Tool 的治理一致性。

## Take Away
Tool 处理函数固定只做"取参→调 Service→包结果"三件事——校验、审计、限流才有统一挂载点，业务逻辑也才能脱离协议层测试。

## Why This Slide Matters
跳过这页，学员会把 SQL 和业务规则写进 Tool 处理函数——治理逻辑没有统一挂载点只能逐个补，测试要穿过 MCP 协议层，Server 端演进越来越难。

## Transition
看完了 Demo 里两个 MCP Server 的代码实现，那在行业里还有哪些客户端和生态在用 MCP？暴露外部工具又必须防范哪些安全风险？我们看下一页（page_13）：MCP 生态与 Tool 调用安全。
