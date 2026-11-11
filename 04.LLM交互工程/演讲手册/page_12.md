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

**版本提示(一句话,避免把版本当常识):** Spring AI 2.0 GA 已于 2026-06 发布——统一 Model API、ToolCallingAdvisor 统一编排 Tool Calling、MCP 注解驱动、Micrometer Observation 可观测,基线是 JDK 21+ / Spring Boot 4 / Spring Framework 7 / Jackson 3。本课 demo 刻意停在 **1.0.0-M4**——它是用来讲"封装原理"的,不是现行版;第九课会把同一功能升到 2.0 并逐模块对比"框架接管了什么、边界在哪"。学员做笔记时别把 1.0.0-M4 的具体 API 当通用知识记:**封装链路(messages 组装 / HTTP / SSE 解析)两代是共通的,具体 API 以 2.0 为准。**

## Key Emphasis
重点是“封装了什么”——学员需要理解这个链路，遇到问题时知道去哪里排查。不要把 Spring AI 讲成黑盒。同时点清版本边界：demo 是 1.0.0-M4（讲原理），现行版是 2.0（2026-06 GA），别让学员把版本 API 记成通用知识。

## Demo 与生产的差距

Demo 里 Spring AI 的用法是最基础的。实际项目中还需要考虑：

**错误处理**：Demo 里 LLM 调用失败会直接抛异常。实际项目中需要重试机制——网络超时重试、模型过载重试、限流重试。Spring AI 支持配置重试策略，但 Demo 没有实现。

**Token 用量统计**：Demo 里没有记录每次调用消耗了多少 token。实际项目中必须记录——用于成本监控和用量分析。Spring AI 的响应里包含 usage 字段，但 Demo 没有解析。

**模型降级**：Demo 里模型路由只做了“选一个模型”。实际项目中需要降级策略——主模型调用失败时自动切换到备用模型。比如 GPT-4o 超时了，自动降级到 GPT-4o-mini。

**并发控制**：Demo 里没有限制并发请求数。实际项目中需要限流——防止用户大量并发请求把 LLM API 打爆，或者把 token 预算用完。

## Optional Extension
**学员问：框架封装这么好，为什么还要花时间懂内部链路？**
答：出问题时你得知道去哪一层排查，否则只能重启碰运气。
Demo 做法：在后端控制台观察 `/recommend` 打印的原始输出、提取结果与异常信息，快速定位是 Prompt 问题、模型问题还是网络问题——这就是懂链路的直接收益（在生产实践中建议引入独立日志文件进行全量留存）。
生产做法：超时/重试/降级配在哪一层要想清楚（page_16 第 5、6 项）——Spring AI 只管发请求，链路治理要自己加；到第九课用 AgentScope Java 2.0 重构同一功能，会逐模块对比"框架接管了什么、边界在哪"（结论：框架接管胶水层与基础设施，业务语义仍要自己写）。

## Classroom Interaction
让学员在调用处把 temperature 改一档再发一次请求，从日志/响应确认请求体跟着变——验证 options 是运行时覆盖，不是默认值。

## Take Away
封装省的是胶水，不是治理——超时、重试、降级、成本该配还得自己配。

## Why This Slide Matters
跳过这页，学员会把 Spring AI 当黑盒：线上一慢就只会加机器，调用失败只会重试，永远建立不起"分层排查、分层治理"的思路。

## Transition
后端讲完了。但一个完整的应用还需要前端。
