## Slide Objective
讲清楚 OpenAI 协议的请求/响应结构，让学员理解"为什么配个 URL 和 Key 就能交互"。

## Suggested Presentation
这一页是整节课最重要的一页。后面六课全部建立在这个协议之上。

看左边的请求结构。就是一个 HTTP POST。你可以用 curl 自己验证——在终端里执行 `curl -N "http://localhost:8080/api/agent/chat?message=你好"`，看后端日志。Spring AI 在内部构建的就是这个 JSON。

请求体里最核心的是 messages 数组。三种角色：system 定义 Agent 是谁，user 是用户输入，assistant 是模型回复。多轮对话就是把历史消息全部塞进去。你可以验证：在前端连续问两个问题，看后端日志里 messages 数组是不是越来越长。

看右边的响应。Streaming 模式下，每个 `data:` 开头的行是一个 token。注意 2026 年对接推理模型（如 DeepSeek-R1、Qwen3）时，流式响应实行**双流分离**：模型先通过 `delta.reasoning_content` 流式输出思考过程（前端可渲染为可折叠的“深度思考”框），思考完毕再通过 `delta.content` 流式输出正式回答。而在非流式响应底部的 `usage` 中，除了输入/输出 token 数，还会返回 `cached_tokens`（前缀缓存命中数）。你可以打开浏览器开发者工具（F12），切到 Network 标签，看 `/api/agent/chat` 请求的响应——你会看到 SSE 流逐 token 返回。

右边的消息注入点是关键。现在 v1.0，messages 只有 system 和 user。到了 v2.0 Memory，会在中间注入历史对话。到了 v3.0 Knowledge，会注入 RAG 检索结果。到了 v4.0 Tool，请求里会多一个 tools 字段。所有后续课程的能力，都是通过修改这个 messages 数组或请求体来实现的。

底部这句话记住：OpenAI 协议不是"知道就行的知识"，而是后面六课的共同基础。这一课讲不透，后面都学不会。

## Key Emphasis
让学员用 curl 和浏览器开发者工具亲自验证。这不是"听"的知识，是"看"的知识。

## Optional Extension
**学员问：为什么不用各厂商自己的 SDK，而要统一到 OpenAI 协议？**
答：协议比 SDK 稳，而且是多供应商的前提。
Demo 做法：Spring AI 按 OpenAI 协议发请求，`application.yml` 只配 base-url + api-key，换服务商只改配置不动代码——Demo 里硅基流动就是 OpenAI 兼容端点。
生产做法：多供应商备份、模型降级都建立在协议兼容上（page_16 第 6 项）；统一接入收口到 LLM Gateway，业务代码只面向协议编程，第十一课网关支柱展开。

## Classroom Interaction
让学员看 `PromptManager.buildSystemPrompt()` / `buildUserPrompt()` 与 `ShoppingAgentController` 组装出的请求，对照协议里的 messages 数组，指出代码里的 `.system()` 和 `.user()` 分别对应数组里哪个元素——协议从此不是抽象概念。

## Take Away
掌握 messages 数组，就掌握了后续所有能力的注入点——记忆、RAG、工具结果都是往里加消息。

## Why This Slide Matters
跳过这页，学员不理解第五课注入记忆、第六课注入 RAG 改的是请求里的哪里，后面三课的知识挂不到点上。

## Transition
协议只定义了基本对话能力。但它还定义了更多标准能力——Function Calling 就是其中一个。
