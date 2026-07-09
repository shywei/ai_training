## Slide Objective
讲清楚 Spring AI 在 Demo 中封装了什么，让学员理解"框架的价值"和"理解底层协议的前提"。

## Suggested Presentation
Spring AI 做了四件事。

第一，自动配置连接。你只需要在 application.yml 里写 base-url 和 api-key，Spring AI 自动创建 HTTP 连接，处理认证。你不需要写任何 HTTP 请求代码。

第二，SSE 流式解析。调用 .stream().content() 时，Spring AI 自动处理 SSE 协议——解析 data: 前缀、处理 [DONE] 标记、拼接 delta.content。你拿到的是 Flux<String>，逐 token 的文本流。

第三，非流式调用。调用 .call().content() 时，等全部生成完返回完整字符串。Self-Correction 场景用这个。

第四，动态模型切换。通过 OpenAiChatOptions.builder().withModel() 在运行时切换模型。这就是 ModelRouter 能工作的底层机制。

看右边的代码对比：流式调用用 .stream()，非流式用 .call()，其他部分完全一样。Spring AI 把前面讲的 OpenAI 协议全部封装了。但理解底层协议是理解框架的前提——你知道它在做什么，才能在出问题时排查。

## Key Emphasis
不要把 Spring AI 讲成"黑盒"。学员需要理解它封装了什么，这样遇到问题时知道去哪里排查。

## Take Away
Spring AI 封装了 HTTP/SSE/模型切换，开发者只关注业务逻辑。但理解底层协议是排查问题的前提。

## Transition
后端讲完了。但一个完整的应用还需要前端。用户怎么和我们的导购 Agent 对话？
