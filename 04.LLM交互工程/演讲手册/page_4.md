## Slide Objective
讲清楚 OpenAI 协议的请求/响应结构，让学员理解"为什么配个 URL 和 Key 就能交互"。

## Suggested Presentation
这一页是整节课最重要的一页。后面六课全部建立在这个协议之上。

看左边的请求结构。就是一个 HTTP POST。你可以用 curl 自己验证——在终端里执行 `curl -N "http://localhost:8080/api/agent/chat?message=你好"`，看后端日志。Spring AI 在内部构建的就是这个 JSON。

请求体里最核心的是 messages 数组。三种角色：system 定义 Agent 是谁，user 是用户输入，assistant 是模型回复。多轮对话就是把历史消息全部塞进去。你可以验证：在前端连续问两个问题，看后端日志里 messages 数组是不是越来越长。

看右边的响应。Streaming 模式下，每个 `data:` 开头的行是一个 token。你可以打开浏览器开发者工具（F12），切到 Network 标签，看 `/api/agent/chat` 请求的响应——你会看到 SSE 流，一行一行的 `data:` 就是逐 token 返回的。

右边的消息注入点是关键。现在 v1.0，messages 只有 system 和 user。到了 v2.0 Memory，会在中间注入历史对话。到了 v3.0 Knowledge，会注入 RAG 检索结果。到了 v4.0 Tool，请求里会多一个 tools 字段。所有后续课程的能力，都是通过修改这个 messages 数组或请求体来实现的。

底部这句话记住：OpenAI 协议不是"知道就行的知识"，而是后面六课的共同基础。这一课讲不透，后面都学不会。

## Key Emphasis
让学员用 curl 和浏览器开发者工具亲自验证。这不是"听"的知识，是"看"的知识。

## Transition
协议只定义了基本对话能力。但它还定义了更多标准能力——Function Calling 就是其中一个。
