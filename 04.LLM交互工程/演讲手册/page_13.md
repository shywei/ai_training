## Slide Objective
展示 Demo 的完整项目结构，让学员理解每个文件的职责，以及后续课程会在此基础上增加什么。

## Suggested Presentation
这是 enterprise-ai-agent 的完整结构。左边是目录树，右边是每个核心文件的职责。

后端部分：ModelRouter 做模型路由，PromptManager 构建 Prompt，StructuredOutputHandler 处理结构化输出，GuardrailsService 做安全防护，ShoppingAgentController 是导购 Agent 的 API 入口，串联所有组件。CorsConfig 处理跨域。application.yml 存配置。

前端部分：App.vue 是聊天界面，vue.config.js 配置 dev server 代理。

这个结构不是最终形态。后续每课都会在上面加东西：v2.0 加 Memory 组件和 Redis 集成，v3.0 加 Knowledge 组件和向量数据库，v4.0 加 Tool 组件和 MCP Server，v5.0 加 Workflow 引擎。

底部的提示很重要：v1.0-prompt 是起点，不是终点。每课都在这个骨架上长出新的能力。

## Key Emphasis
让学员理解"逐渐进化"的设计思想。不是每课重新搭架构，而是在同一套代码上持续扩展。

## Take Away
v1.0 是骨架，后续每课在这个基础上增加新模块。

## Transition
最后，我们做个总结。
