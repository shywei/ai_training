## Slide Objective
讲解 MCP 的四大核心概念(Tool / Resource / Prompt / Sampling),让学员理解 MCP 协议的完整能力范围。Demo 覆盖了 Tool 和 Resource,Prompt 和 Sampling 仅做概念介绍。

## Suggested Presentation

上一页讲了 MCP 的架构--谁来连接、怎么连接。这一页讲 MCP 连接之后能做什么。MCP 定义了四种能力,不是所有都要用,但学员需要知道完整的能力范围。

**Tool** 是最核心的能力。LLM 可以调用的函数--搜索、查询、写入、计算。关键点是"LLM 决定何时调用",不是开发者预设的。Demo 中的 `search_knowledge`、`queryProductInfo`、`save_memory` 都是 MCP Tool。看 PPT 右侧的代码:Server 暴露 Tool 的 Schema(name + description + inputSchema),Client 用 `callTool("queryPrice", {id:"NB001"})` 调用。Tool 的核心价值是"LLM 自主决策"--不是写死的 if-else,而是 LLM 看到用户需求后自己判断该调什么。

**Resource** 是 Client 可读取的结构化数据。和 Tool 的区别是"谁发起"--Tool 是 LLM 决定调用,Resource 是 Client 主动读取。Demo 中的 `user://{userId}` 用户画像是典型例子:Client 通过 `readResource("user://U001")` 读取画像 JSON。但注意:**MCP 协议没有规定 Resource 读了之后怎么用。** 是否注入 LLM context、什么时候读、缓存还是实时读--这些都是 Client 自己的实现决策。我们的 Demo 选择在会话开始时读取画像并注入 prompt,这是一种常见做法,但不是 MCP 协议的要求。Resource 本质上只是"数据源",怎么用是 Client 的事。

**Prompt** 是 Server 预定义的 Prompt 模板。Server 提供标准化的 Prompt,Client 选择使用。比如一个"分析评论"的 MCP Server 可以提供 `analyze-review` Prompt,Client 不需要自己写分析评论的提示词,直接调用 Server 提供的。这是"声明式能力暴露"--Server 不只暴露函数,还暴露最佳实践。Demo 没有实现,但学员需要知道这个能力存在--企业内部可以建立 Prompt 市场,不同部门共享标准化的 Prompt 模板。

**Sampling** 是反向调用模式--Server 请求 Client 调用 LLM。正常流程是 Client 调 Server,Sampling 反过来:Server 在处理过程中需要 LLM 做中间推理,就通过 Sampling 请求 Client 调 LLM。典型场景是多 Agent 协作--一个 MCP Server 内部需要调用另一个 Agent 的能力。Demo 没有实现,学员知道概念就行。这是 MCP 最高级的能力,目前社区用得不多,但未来多 Agent 架构会越来越重要。

四个能力不是都要用。Tool 和 Resource 是基础,覆盖 90% 的场景。Prompt 和 Sampling 是进阶能力,学员知道存在、理解适用场景就够了。

## Key Emphasis

- Tool vs Resource 的核心区别是“谁发起”——Tool 是 LLM 决策，Resource 是 Client 读取。但“谁发起”和“怎么用”是两回事——Resource 读了之后是否注入 context，是 Client 的实现决策，不是 MCP 协议的规定
- Prompt 和 Sampling 仅做概念介绍,不需要深入代码
- 不要让学员觉得四个都要用--Tool + Resource 覆盖大多数场景
- Demo 中 `save_memory` 是 Tool 但不暴露给 LLM(系统内部调用)--不是所有 Tool 都要给 LLM 决策

## Optional Extension

学员问"MCP 和 REST API 有什么区别"时:
- REST API 是"调用接口",MCP 是"发现+调用+上下文"的完整协议
- MCP 有 Tool 发现(listTools)、Schema 描述、Resource 读取--REST 没有这些
- MCP 是给 AI Agent 设计的,REST 是给开发者设计的

学员问"为什么不直接用 Function Calling,要加 MCP"时:
- Function Calling 是 LLM 的能力,MCP 是工具暴露的标准
- 没有 MCP:每个工具写一套适配代码
- 有 MCP:一次暴露,所有 Agent 都能用

## Take Away

MCP 四大能力:Tool(LLM 调用)+ Resource(Client 读取)+ Prompt(标准化模板)+ Sampling(反向调用)。前两个是基础,后两个是进阶。

## Why This Slide Matters

没有这页,学员只知道 MCP 能"调工具",不知道 MCP 还能"读数据"(Resource)、"共享 Prompt"(Prompt)、"反向调用 LLM"(Sampling)。能力范围不完整,选型时会遗漏方案。

## Transition

四大能力讲完了,下一页看 MCP 的三层架构--Client 怎么连 Server、Transport 用什么协议、Server 怎么暴露能力。从"能做什么"到"怎么实现"。
