## Slide Objective
拆解 Spring AI 的封装链路，让学员理解"不是魔法，是封装"。

## Suggested Presentation
看上面的调用链路。你写3行 Java 代码，Spring AI 在内部做了什么？

第一，Prompt 组装。你的 `.system(systemPrompt).user(userPrompt)` 变成了 messages 数组里的两条消息。Spring AI 在内部用 `ChatMessage` 对象封装，每个 message 有 role 和 content。

第二，HTTP 请求。Spring AI 读取 application.yml 的 `base-url` 和 `api-key`，自动构建 POST 请求，加上 `Authorization: Bearer ***` Header。你不需要写任何 HTTP 代码。

第三，SSE 解析。调用 `.stream().content()` 时，Spring AI 内部用 SSE 客户端逐行读取响应，解析 `data:` 前缀，处理 `[DONE]` 标记，拼接 `delta.content`。你拿到的是 `Flux<String>`，逐 token 的文本流。

第四，动态模型切换。`OpenAiChatOptions.builder().withModel(model)` 在运行时修改请求里的 model 字段。这就是 ModelRouter 能工作的底层机制。

看右边的代码对比。上面是你写的3行。下面是 Spring AI 内部的伪代码——做的事情和你手写 HTTP 请求完全一样，只是封装了。

有个真实的坑：我们一开始以为 `withModel()` 只是设置默认值，不影响已经配置的其他参数。实际上，`OpenAiChatOptions` 的 builder 会覆盖所有选项——如果你只设了 model，temperature 会用 builder 的默认值（1.0），而不是 application.yml 里的 0.7。所以 recommend 接口显式设置了 `withTemperature(0.2)`，确保不会被覆盖。

## Key Emphasis
重点是"封装了什么"——学员需要理解这个链路，遇到问题时知道去哪里排查。不要把 Spring AI 讲成黑盒。

## Transition
后端讲完了。但一个完整的应用还需要前端。
