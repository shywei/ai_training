## Slide Objective
展示 Demo 的完整项目结构，让学员理解每个文件的职责和后续扩展方式。

## Suggested Presentation
这是 enterprise-ai-agent 的完整结构。七个 Java 文件加一个 Vue 文件，不多，但每一个解决一个具体问题。

逐个过一遍。ModelRouter 解决"用哪个模型"——根据消息复杂度路由到快模型或推理模型。PromptManager 解决"怎么构建 Prompt"——System/User 分离，留了 context 扩展点。StructuredOutputHandler 解决"怎么拿到干净 JSON"——提取、验证、重试。GuardrailsService 解决"安全防护"——入口防注入、出口过滤敏感信息。ShoppingAgentController 是 API 入口，串联所有组件。CorsConfig 解决跨域。application.yml 存配置。

前端就一个 App.vue，用 Vue 2 + Element UI 做聊天界面，marked 做 markdown 渲染。

关键设计思想：这个结构不是最终形态。后续每课都会加东西。v2.0 加一个 Memory 组件和 Redis 集成，改的是 ShoppingAgentController（往 context 里注入对话历史）和 application.yml（加 Redis 配置）。v3.0 加 Knowledge 组件和向量数据库，改的是 PromptManager（往 context 里注入 RAG 结果）。v4.0 加 Tool 组件和 MCP Server。v5.0 加 Workflow 引擎。

每次升级，核心文件不变，只加新组件和改调用方式。这就是为什么要把每个能力独立成组件——方便扩展，不影响已有代码。

## Key Emphasis
让学员理解"逐渐进化"的设计思想。不是每课重新搭架构，而是在同一套代码上持续扩展。每个文件的职责要讲清楚。

## Transition
最后，做个总结。
