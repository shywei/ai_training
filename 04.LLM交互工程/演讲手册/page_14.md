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

## Optional Extension
**学员问：后面每课都往里加组件，架构会不会越来越乱？**
答：不会——前提是每个能力独立成组件、只改注入点。
Demo 做法：看 `core/` 下几个类各管一事——`PromptManager` 管构建、`StructuredOutputHandler` 管解析、`GuardrailsService` 管防护，`ShoppingAgentController` 只做串接；加新能力就是加类 + 在链路上插一步。
生产做法：组件边界对应治理边界——限流、脱敏、成本核算都是横切件，最终会从业务代码里抽出去收口到网关/平台层（第十一课），业务代码只留业务语义；第八课的 容错恢复机制、第十课的 Agent 编排也都是"横切能力独立成件"的延续。

## Classroom Interaction
让学员回答"如果要加记忆功能，你会改哪个文件"——预期答案是 `buildUserPrompt()` 的 context 注入点，不是新写一个 Controller。答错的学员正好当场纠偏。

## Take Away
扩展靠加组件和改注入点，不靠重写——这就是组件化在多课程演进里的真实收益。

## Why This Slide Matters
跳过这页，学员会把逻辑全堆在 Controller 里，第五课起每加一个能力都要重写一遍，到第八课彻底改不动——"逐渐演进"的架构课就白上了。

## Transition
项目结构看清楚了。接下来我们给出一份清晰的代码阅读路线，以及 7 个可以在本课 Demo（`v1.1.1`）中亲手跑出来的验证点。
