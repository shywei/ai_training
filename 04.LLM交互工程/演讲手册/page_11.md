## Slide Objective
拆解 Spring AI 的封装链路,让学员理解"不是魔法,是封装"。

## Suggested Presentation
看上面的调用链路。你写3行 Java 代码,Spring AI 在内部做了什么?

第一,Prompt 组装。你的 `.system(systemPrompt).user(userPrompt)` 变成了 messages 数组里的两条消息。Spring AI 在内部用 `ChatMessage` 对象封装,每个 message 有 role 和 content。

第二,HTTP 请求。Spring AI 读取 application.yml 的 `base-url` 和 `api-key`,自动构建 POST 请求,加上 `Authorization: Bearer ***` Header。你不需要写任何 HTTP 代码。

第三,SSE 解析。调用 `.stream().content()` 时,Spring AI 内部用 SSE 客户端逐行读取响应,解析 `data:` 前缀,处理 `[DONE]` 标记,拼接 `delta.content`。你拿到的是 `Flux<String>`,逐 token 的文本流。

第四,动态模型切换。`OpenAiChatOptions.builder().withModel(model)` 在运行时修改请求里的 model 字段。这就是 ModelRouter 能工作的底层机制。

看右边的代码对比。上面是你写的3行。下面是 Spring AI 内部的伪代码--做的事情和你手写 HTTP 请求完全一样,只是封装了。

有个真实的坑:我们一开始以为 `withModel()` 只是设置默认值,不影响已经配置的其他参数。实际上,`OpenAiChatOptions` 的 builder 会覆盖所有选项--如果你只设了 model,temperature 会用 builder 的默认值(1.0),而不是 application.yml 里的 0.7。所以 recommend 接口显式设置了 `withTemperature(0.2)`,确保不会被覆盖。

## Key Emphasis
重点是“封装了什么”——学员需要理解这个链路，遇到问题时知道去哪里排查。不要把 Spring AI 讲成黑盒。

## Demo 与生产的差距

Demo 里 Spring AI 的用法是最基础的。实际项目中还需要考虑：

**错误处理**：Demo 里 LLM 调用失败会直接抛异常。实际项目中需要重试机制——网络超时重试、模型过载重试、限流重试。Spring AI 支持配置重试策略，但 Demo 没有实现。

**Token 用量统计**：Demo 里没有记录每次调用消耗了多少 token。实际项目中必须记录——用于成本监控和用量分析。Spring AI 的响应里包含 usage 字段，但 Demo 没有解析。

**模型降级**：Demo 里模型路由只做了“选一个模型”。实际项目中需要降级策略——主模型调用失败时自动切换到备用模型。比如 GPT-4o 超时了，自动降级到 GPT-4o-mini。

**并发控制**：Demo 里没有限制并发请求数。实际项目中需要限流——防止用户大量并发请求把 LLM API 打爆，或者把 token 预算用完。

## Transition
后端讲完了。但一个完整的应用还需要前端。
