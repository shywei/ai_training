## Slide Objective
展示 LLM 路由的代码实现，以及路由如何影响 context 构建和 Tool 注册。

## Suggested Presentation
`LlmRouter.java` 的实现。核心是一个方法：`route(String userMessage)`。用快模型（Qwen2.5-7B）做三分类，temperature=0 保证确定性输出。Prompt 很简单："判断用户消息的意图，只输出 KNOWLEDGE、TOOL 或 CHAT"。

这里有个细节：temperature=0 是必须的。如果 temperature > 0，同一个消息可能在不同时间得到不同的路由结果——用户体验不一致。路由需要确定性，生成需要创造性。

`ShoppingAgentController.chat()` 中路由如何影响三个决策：

**① 注入什么 context**：始终构建 `memoryManager.buildContext(sessionId, userId, message)`（画像 + 记忆 + 历史）。只有 KNOWLEDGE 路由才追加 RAG 结果：`mcpKnowledgeClient.search(message)`。

**② 注册什么 Tool**：只有 TOOL 路由才注册 product tools：`mcpProductClient.getFunctionCallbacks()`。其他路由不注册，节省 token。

**③ save_memory 跨路由注册**：`mcpKnowledgeClient.getFunctionCallbacks()` 在所有路由下都注册。这是设计决策——用户随时可能表达偏好。

这里有个坑：`getFunctionCallbacks()` 返回的列表在 MCP Server 未连接时为空。Demo 做了优雅降级——MCP Server 未启动时 Agent 仍可正常运行，只是 Tool 不可用。日志会显示 `[Tool] MCP Server 未连接，无可用 Tool`。

## Key Emphasis
路由不只是分类，它决定了整个对话的处理方式。这是第七课和第六课的核心区别——第六课每次都注入 RAG，第七课由路由决定。

## Transition
路由逻辑理顺了，但在设计 MCP 接口时，我们应该怎么在 Tool 和 Resource 两个核心概念之间做选型决策呢？我们看下一页：选型决策。
