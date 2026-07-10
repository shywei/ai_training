## Slide Objective
展示课程全景，让学员知道今天学什么、每部分对应 Demo 的哪里。

## Suggested Presentation
今天的课程分八个部分。我先说一下学习方法：不要只听概念，要跟着 Demo 跑代码。

Demo 在 `enterprise-ai-agent/` 目录下。后端是 Spring Boot，前端是 Vue 2。你现在就可以 clone 下来，`./mvnw spring-boot:run` 启动后端，`npm run dev` 启动前端，浏览器打开 localhost:3000 就能用。

每讲完一个概念，我会切到 Demo 代码，告诉你"这个概念对应哪个文件的哪段代码"。你能在 IDE 里打断点、看日志、改参数，亲自验证。

今天的核心文件就七个：ModelRouter、PromptManager、StructuredOutputHandler、GuardrailsService、ShoppingAgentController、CorsConfig、App.vue。不多，但每一个都解决一个具体问题。

## Key Emphasis
强调"跟着跑代码"。学员如果只是听，效果减半。必须动手。

## Transition
先从最基础的问题开始：你调用一次大模型，到底发生了什么？
