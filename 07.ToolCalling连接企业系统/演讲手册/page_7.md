## Slide Objective
对比两种实现方式，解释 Demo 为什么选 M4，以及代码怎么写。

## Suggested Presentation
两种方式的底层是一样的——最终都转成 OpenAI function definitions 给 LLM。区别只是代码风格。

**方式一：@Tool 注解**（Spring AI 1.0.0+ GA）。声明式，简洁。一个 `@Tool(description="查询商品价格")` 注解搞定，参数用 `@ToolParam("商品ID")` 标注。Spring AI 自动提取方法名、参数名、参数类型，生成 JSON Schema。代码量最少。

**方式二：FunctionCallbackWrapper**（Spring AI 1.0.0-M4）。编程式，灵活。手动定义 `Function<Map, String>`，手动指定 name、description。参数 Schema 需要自己写（或者通过泛型类型推断）。代码量多，但完全可控。

Demo 用 M4 是因为要保持和前几课 Spring AI 版本一致。M4 版本没有 @Tool 注解——这是 GA 版本才引入的。

看 Demo 代码。`McpProductClient.java` 的 `init()` 方法里，每个 MCP Tool 对应一个 `new McpToolCallback(...)`——注意不是 `FunctionCallbackWrapper`，而是独立的桥接类 `tool/McpToolCallback.java`，直接实现 Spring AI 的 `FunctionCallback` 接口。构造参数五个：名称 `"mcp_" + toolName`（加 `mcp_` 前缀避免和本地函数冲突）、描述 `"[MCP] " + toolDescription`、MCP Server 给的 `inputSchema`、`mcpClient`、原始 Tool 名。它的 `getInputTypeSchema()` 直接返回 MCP Tool 自带的 inputSchema，`call()` 接收 JSON 字符串参数、转成 `Map<String, Object>` 后调用 MCP Client 的 `callTool()` 方法。

为什么写独立的 `McpToolCallback` 而不用 `FunctionCallbackWrapper.builder(function)`？因为有个坑：`Map<String, Object>` 作为参数类型时，Spring AI 无法自动生成 JSON Schema。MCP Tool 的 Schema 来自 MCP Server 的 `listTools()` 返回值，不需要 Spring AI 生成。但如果用 @Tool 注解，Schema 从 Java 方法签名自动生成，不需要手动写。

## Key Emphasis
@Tool 注解更简洁，但需要 GA 版本。FunctionCallbackWrapper 更灵活，适合 M4 版本和需要动态注册的场景。Demo 中 MCP Tool 的动态注册必须走编程式（独立的 `McpToolCallback`）——@Tool 是编译时注解，不能动态注册。

## Optional Extension
**学员问：@Tool 注解这么简洁，为什么 Demo 不用它，非要写编程式注册？**
答：判据不是简洁度，是"Tool 列表什么时候才能知道"。
Demo 做法：MCP 的 Tool 清单来自 `mcpClient.listTools()` 的运行时返回，注册逻辑在 `McpProductClient.init()` 里对每个 Tool 动态 `new McpToolCallback(...)`、加 `mcp_` 前缀防冲突——注解是编译期定死的元数据，做不到"Server 暴露什么就注册什么"。
生产做法：动态注册的 Tool 要做 Schema 版本化管理（name/inputSchema 变更即破坏性变更），Client 与 Server 分开发布、契约回归测试兜底；平台期由 MCP Gateway 统一注册与版本治理（第十一课支柱一）。取舍：编程式多写几十行胶水，换来的是 Server 端增删 Tool 不用动客户端。第九课用 AgentScope Java 2.0 重构时会对比框架的 Tool 注册方式。

## Take Away
注解式还是编程式注册，只看一个判据：Tool 列表是编译期已知还是运行时发现——运行时发现（MCP 动态注册）必须走编程式，注解留给静态工具。

## Why This Slide Matters
跳过这页，学员会用注解硬编码 MCP 工具——Server 端增删 Tool 后客户端注册表和实际不一致，调用报"函数不存在"，还得改客户端重新发版才能恢复。

## Transition
两种方式定义了函数。但 Function Calling 的思想比这更深——它背后是 ReAct 模式。
