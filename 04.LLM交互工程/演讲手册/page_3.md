## Slide Objective
让学员理解一次 LLM 调用的完整链路，建立"LLM 是 Agent 的 CPU"的认知。

## Suggested Presentation
你的 Java 代码调用 ChatClient.prompt()，背后发生了什么？

看这张流程图。首先，Spring AI 把你的消息序列化成 JSON，发一个 HTTP POST 请求到服务商的 API 地址。服务端收到后，把文本拆成 token——也就是数字序列，送进 GPU 做矩阵运算。推理完成后，把生成的 token 解码成文本，通过 SSE 流式返回。

整个过程中，Token 编码解码很快，毫秒级。瓶颈在 GPU 推理——可能几百毫秒到几秒。这就是为什么我们要用 Streaming：不用等全部生成完再返回，逐 token 返回，用户立刻看到效果。

右边两个卡片记住两点：LLM 是整个 Agent 的 CPU，所有能力最终都要通过它推理；推理是性能瓶颈，所以 Streaming 和模型选型很重要。

## Key Emphasis
不要深入讲 Transformer 或注意力机制。重点是链路：Java → HTTP → Token → GPU推理 → Token → SSE。学员需要理解的是"调用的本质是 HTTP 请求"。

## Take Away
LLM 调用的本质是 HTTP POST 请求。理解这个，后面所有章节都好理解。

## Transition
既然是 HTTP 请求，那请求和响应的格式是什么？这就是 OpenAI 接口协议。
