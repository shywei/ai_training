## Slide Objective
展示 Function Calling 的底层协议格式，让学员理解"自动封装"背后发生了什么。

## Suggested Presentation
Function Calling 不是魔法，底层就是 JSON。

**请求格式**：OpenAI API 请求中的 `functions` 参数是一个数组，每个元素有 `name`、`description`、`parameters`。`parameters` 是标准的 JSON Schema，定义了参数类型、描述、必填项。Spring AI 的 `FunctionCallbackWrapper` 自动生成这段 JSON——开发者定义 Java 函数，Spring AI 通过反射提取参数名和类型，生成对应的 JSON Schema。

**响应格式**：LLM 返回的不是普通文本，而是一个包含 `tool_calls` 的消息。`content` 为 null，`tool_calls` 是一个数组（LLM 可以一次请求调用多个函数）。每个 tool_call 有 `id`（用于关联执行结果）、`function.name`（函数名）、`function.arguments`（JSON 字符串格式的参数）。

**多轮对话**：执行完函数后，结果作为 `role=tool` 的消息追加到对话历史。tool_call_id 用于关联——"这个结果是哪个 tool_call 的执行结果"。LLM 看到 tool 消息后，基于结果生成最终回复。

Demo 中这些 JSON 是 Spring AI 自动生成的，开发者不需要手写。但理解底层格式很重要。调试时看到 LLM 响应中 content=null、tool_calls 非空，你知道这是正常的 Function Calling 流程，不是报错。很多学员第一次看到这个响应会以为 LLM 出了问题。

## Key Emphasis
content=null + tool_calls 非空 = LLM 请求调用函数。这不是错误，是 Function Calling 的正常流程。这是调试时最常见的"误以为报错"。

## Transition
Spring AI 提供了两种方式来定义函数——@Tool 注解和 FunctionCallbackWrapper。用哪种取决于 Spring AI 版本。
