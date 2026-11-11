## Slide Objective
通过 Function Calling 的完整 JSON 示例，让学员理解"模型不直接调用工具，而是返回调用意图"。

## Suggested Presentation
Function Calling 是 OpenAI 协议里最复杂也最重要的能力。第七课 Tool 全部建立在这个机制上。

看左边的请求。和普通对话比，多了一个 tools 字段。你在这里定义你有哪些工具——每个工具有名字、描述、参数格式。这里定义了一个 check_inventory 工具，接收 product_name 参数。

看右边的响应。关键点：模型返回的不是库存数据，而是一个 tool_calls。它告诉你"我要调用 check_inventory，参数是 iPhone 16"。模型不直接调用——它只是返回调用意图。

你的代码拿到 tool_calls 后，自己去调库存 API（这就是第七课要讲的），拿到结果，然后把结果作为 role=tool 的消息传回给模型。模型拿到结果后，生成最终回复。

这个三步走的流程——定义工具、模型返回意图、你的代码执行后传回结果——就是第七课 Tool 的核心。整个流程都是 OpenAI 协议定义的，跟具体框架无关。

你可以现在就在 Demo 里验证：打开 `ShoppingAgentController`，看 `/chat` 接口——它现在没有传 tools 字段，所以模型不会返回 tool_calls。到了第七课，我们会加上 tools 字段，你就能看到 tool_calls 长什么样了。

## Key Emphasis
Function Calling 的核心是"模型返回意图，代码执行"。这个设计让同一个协议能支持各种工具调用场景。

## Optional Extension
**学员问：模型会不会直接调用我的 API？工具执行的安全边界在哪？**
答：不会。模型只返回调用意图，执行权永远在你的代码里——这就是安全边界。
Demo 做法：本课 Demo（`v1.1.1`）的 `ShoppingAgentController` 不传 tools 字段，所以响应里没有 tool_calls；这就是第七课的对比基线。
生产做法：第七课 `MCP工具回调组件` 把 MCP 工具注册进 tools 字段，拿到 tool_calls 后由你的代码执行——执行必须配治理五件套（限流/幂等/超时/补偿/审计），工具不是接口，是安全边界；多 Agent 场景下工具授权更复杂，那是第十课 Router / Supervisor–Worker / Handoff 编排里的话题。

## Classroom Interaction
让学员打开 `ShoppingAgentController.java`，查看 `/chat` 和 `/recommend` 的 `ChatClient` 调用链，确认里面没有挂载 tools/functions——亲手确认"模型现在真的不会调工具"，为第七课留基线。

## Take Away
Function Calling 是意图与执行的分离——模型出意图，代码做执行，执行环节的治理比调用环节更容易出事。

## Why This Slide Matters
跳过这页，学员会在第七课误以为模型能直接操作企业系统，权限和审计边界的设计会做错——以为在限制模型，其实在限制自己代码都没搞清。

## Transition
协议的标准能力讲完了。但 2026 年的 LLM 接口已经分代演进——除了 Chat Completions，还有 Responses API 和 Open Responses，下一页看怎么选型。
