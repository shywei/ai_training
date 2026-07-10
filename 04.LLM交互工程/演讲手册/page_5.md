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

## Transition
协议的标准能力讲完了。接下来看两个直接影响成本的概念：Token 和 Context Window。
