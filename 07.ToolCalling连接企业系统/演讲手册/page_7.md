## Slide Objective
对比两种实现方式，解释 Demo 为什么选 M4，以及代码怎么写。

## Suggested Presentation
两种方式的底层是一样的——最终都转成 OpenAI function definitions 给 LLM。区别只是代码风格。

**方式一：@Tool 注解**（Spring AI 1.0.0+ GA）。声明式，简洁。一个 `@Tool(description="查询商品价格")` 注解搞定，参数用 `@ToolParam("商品ID")` 标注。Spring AI 自动提取方法名、参数名、参数类型，生成 JSON Schema。代码量最少。

**方式二：FunctionCallbackWrapper**（Spring AI 1.0.0-M4）。编程式，灵活。手动定义 `Function<Map, String>`，手动指定 name、description。参数 Schema 需要自己写（或者通过泛型类型推断）。代码量多，但完全可控。

Demo 用 M4 是因为要保持和前几课 Spring AI 版本一致。M4 版本没有 @Tool 注解——这是 GA 版本才引入的。

看 Demo 代码。`McpProductClient.java` 中的 `wrapMcpToolAsFunctionCallback()` 方法：用 `FunctionCallbackWrapper.builder(function)` 创建 callback，`.withName("mcp_" + toolName)` 设置名称（加 `mcp_` 前缀避免和本地函数冲突），`.withDescription(toolDescription)` 设置描述。function 是一个 lambda，接收 `Map<String, Object>` 参数，调用 MCP Client 的 `callTool()` 方法。

这里有个坑：`Map<String, Object>` 作为参数类型时，Spring AI 无法自动生成 JSON Schema。MCP Tool 的 Schema 来自 MCP Server 的 `listTools()` 返回值，不需要 Spring AI 生成。但如果用 @Tool 注解，Schema 从 Java 方法签名自动生成，不需要手动写。

## Key Emphasis
@Tool 注解更简洁，但需要 GA 版本。FunctionCallbackWrapper 更灵活，适合 M4 版本和需要动态注册的场景。Demo 中 MCP Tool 的动态注册必须用 Wrapper——@Tool 是编译时注解，不能动态注册。

## Transition
两种方式定义了函数。但 Function Calling 的思想比这更深——它背后是 ReAct 模式。
