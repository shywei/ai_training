## Slide Objective
展示 PromptManager 和 GuardrailsService 的实际代码，让学员理解"原理怎么落地成代码"。

## Suggested Presentation
看左边，PromptManager.java。两个方法：buildSystemPrompt 用 Text Block 构建系统提示词，变量从配置文件读取。buildUserPrompt 接收消息和 context——注意这里有个 context 参数，当前传的是 null。

为什么 context 是 null？因为第四课只做系统提示词加用户输入。这个参数是留给后续课程的：v2.0 Memory 会传入对话历史和用户画像，v3.0 Knowledge 会传入 RAG 检索结果，v4.0 Tool 会传入工具调用结果。PromptManager 的接口设计是不变的，变的是 context 的内容。这就是为什么要把 Prompt 构建独立成一个组件。

看右边，GuardrailsService。两个方法：checkInjection 是入口检查，用正则匹配危险模式；sanitize 是出口过滤，把敏感关键词替换成星号。两道防线各自独立，Controller 在入口调一次、在返回前调一次。

## Key Emphasis
context 参数的扩展设计是关键教学点。让学员理解"接口不变，内容渐进"的设计思想。

## Take Away
PromptManager 设计了 context 扩展点，GuardrailsService 分入口和出口两道防线。

## Transition
Demo 里的代码我们都看过了。但你可能注意到，我们没有自己写 HTTP 请求、没有自己解析 SSE——这些都是 Spring AI 帮我们做的。Spring AI 到底做了什么？
