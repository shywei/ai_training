## Slide Objective
讲清楚前后端分离架构和 SSE 在前端的处理方式。

## Suggested Presentation
架构很简单：Vue 前端跑在 3000 端口，Spring Boot 后端跑在 8080 端口。开发时用 Vue 的 dev server proxy 把 `/api/*` 转发到后端。配置在 `web/vue.config.js` 里。

前端处理 SSE 是最容易出问题的地方。看右边的代码。用 fetch 发请求，拿到 ReadableStream，逐 chunk 读取。每个 chunk 里可能包含多个 `data:` 行，需要按行分割、提取 content、拼接到完整文本里。遇到 `[DONE]` 就结束。

有个真实的坑：一开始我们没有处理 buffer 拆分问题。一个 chunk 可能只包含半个 `data:` 行——比如 `data: {"choi` 在一个 chunk 里，`ces":{"delta"...}` 在下一个 chunk 里。如果不做 buffer 拼接，JSON 解析会报错。所以代码里有一个 `buffer` 变量，把未完成的行保留到下一个 chunk 处理。

Markdown 渲染用的是 marked 库。LLM 返回的内容经常包含 markdown——代码块、列表、表格、加粗。一开始我们用正则渲染，效果还行。后来发现正则处理不了嵌套的 markdown（比如列表里有代码块），就换成了 marked。`npm install marked`，import 后一行调用 `marked.parse(text)`。

前端还加了几个演示用的快捷按钮：推荐笔记本（普通对话）、分析对比（触发模型路由）、测试注入防御（触发 Guardrails）、结构化输出演示（看后端日志）。这些按钮是为了让学员在演示时能快速触发各种场景。

## Key Emphasis
SSE buffer 处理是最容易被忽略的问题。让学员看前端代码里的 buffer 逻辑。

## Demo 与生产的差距

Demo 的前端是最基础的聊天界面。实际项目中还需要：

**会话管理**：Demo 里 Session ID 存在 localStorage，用户清浏览器就丢了。实际项目中应该用 JWT Token 或服务端 Session 管理，支持多设备同步。

**消息持久化**：Demo 里刷新页面消息就没了。实际项目中需要从 Redis 或数据库恢复历史消息，用户打开页面能看到之前的对话。

**Markdown 渲染安全**：Demo 里用 `marked.parse()` 渲染 LLM 返回的 Markdown，然后用 `v-html` 显示。这有 XSS 风险——LLM 可能返回恶意 HTML。实际项目中需要用 DOMPurify 做 HTML 消毒。

**错误处理**：Demo 里网络断了只显示一行错误文字。实际项目中需要断线重连、消息发送失败重试、loading 状态管理。

## Transition
后端和前端都讲完了。最后看一眼完整的项目结构。
