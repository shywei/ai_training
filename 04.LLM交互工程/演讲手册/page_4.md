## Slide Objective
讲清楚 OpenAI 接口协议的请求/响应结构，让学员理解"为什么配个 URL 和 Key 就能交互"。

## Suggested Presentation
看左边，这是请求的完整结构。就是一个 HTTP POST，URL 是服务商地址加 /v1/chat/completions，Header 里放 Bearer Token 认证。

请求体是 JSON，三个关键字段：model 指定用哪个模型，messages 是对话历史（system 定义角色，user 是用户输入），stream 控制是否流式返回。

右边是 Streaming 响应的格式。每个 data: 开头的行是一个 token，模型每生成一个 token 就发一行。最后发一个 [DONE] 表示结束。

非 Streaming 时更简单——等全部生成完，一次性返回完整的 message.content。

底部这句话是关键：所有服务商——OpenAI、DeepSeek、硅基流动——都遵循同一套协议。这就是为什么你只需要改 URL 和 Key 就能切换服务商。Spring AI 封装了 HTTP 请求和 SSE 解析的细节，但底层就是这个 JSON 协议。

## Key Emphasis
这是整节课的底层基础。学员必须理解 messages 数组的三种 role 和 Streaming 的 data: 格式。不需要记住每个字段，但要理解结构。

## Take Away
OpenAI 协议 = HTTP POST + JSON。所有服务商遵循同一套标准。

## Transition
协议只定义了基本的对话能力。但它实际上还定义了更多标准能力——Function Calling、Structured Output 等。这些能力是后续课程的基础。
