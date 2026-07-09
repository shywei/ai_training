## Slide Objective
讲清楚前后端分离架构和 SSE 在前端的处理方式。

## Suggested Presentation
架构很简单：Vue 前端跑在 3000 端口，Spring Boot 后端跑在 8080 端口。开发时用 Vue 的 dev server proxy 把 /api/* 请求转发到后端，解决跨域问题。生产部署时用 Nginx 做反向代理。

前端处理 SSE 的关键代码在右边。用 fetch 发请求，拿到 ReadableStream，逐 chunk 读取。每个 chunk 里可能包含多个 data: 行，需要按行分割、提取 content、拼接到完整文本里。遇到 [DONE] 就结束。

还有一个细节：LLM 返回的是 markdown 格式，前端需要正确渲染代码块、列表、表格、加粗。Demo 里用正则实现的，没有引入外部 markdown 库。够用，但企业级项目建议用 marked 或 markdown-it。

前后端分离的好处：独立开发、独立部署、独立扩展。前端可以换成 React、移动端、甚至桌面端，后端不用改。

## Key Emphasis
SSE 处理是前端最容易出问题的地方。重点讲 data: 行的解析和 [DONE] 的处理。

## Take Away
前后端分离，SSE 逐 token 更新 UI，Markdown 正则渲染。

## Transition
到这里，Demo 的每个组件我们都讲过了。最后，我们看一眼完整的项目结构，理解每个文件的职责。
