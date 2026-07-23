## Slide Objective
展示 LLM 路由的代码实现，以及路由如何同时返回意图和推荐模型——一次调用做两件事。

## Suggested Presentation
`LlmRouter.java` 的实现。核心方法 `route(String userMessage)` 返回 `RouteResult(route, model)`——一次 LLM 调用同时拿到路由决策和推荐模型。

模型选择逻辑：KNOWLEDGE 和 TOOL 用 reasoning 模型（理解需求、结合知识推荐 / 理解参数、多步推理），CHAT 用 chat 模型（快、便宜）。这样不需要额外的 ModelRouter 组件，路由和模型选择在同一个快模型调用中完成。

这里有个细节：temperature=0 是必须的。如果 temperature > 0，同一个消息可能在不同时间得到不同的路由结果——用户体验不一致。路由需要确定性，生成需要创造性。

路由 Prompt 很短："判断用户消息的意图，只输出 KNOWLEDGE、TOOL 或 CHAT"。不需要 few-shot 示例，不需要复杂的指令。快模型做三分类已经足够准确。

`ShoppingAgentController.chat()` 中路由如何影响三个决策：

**① 注入什么 context**：始终构建 `memoryManager.buildContext(sessionId, userId, message)`（画像 + 记忆 + 历史）。只有 KNOWLEDGE 路由才追加 RAG 结果：`mcpKnowledgeClient.search(message)`。其他路由不注入 RAG——这是和第六课的核心区别。第六课每次都注入 RAG，第七课由路由决定。

**② 注册什么 Tool**：只有 TOOL 路由才注册 product tools：`mcpProductClient.getFunctionCallbacks()`。其他路由不注册，节省 token。注意：注册 FunctionCallback 会增加 LLM 请求中的 token 数（每个 tool definition 约 200-500 token），所以只在需要时注册。

**③ save_memory 不通过 LLM 调用**：save_memory 由系统内部调用（MemoryManager 自动沉淀），不注册给 LLM。这是设计决策——记忆保存的时机应该可控，不应该依赖 LLM 的判断。

这里有个坑：`getFunctionCallbacks()` 返回的列表在 MCP Server 未连接时为空。Demo 做了优雅降级——MCP Server 未启动时 Agent 仍可正常运行，只是 Tool 不可用。日志会显示 `[Tool] MCP Server 未连接，无可用 Tool`。生产中应该有告警机制。

## Key Emphasis
路由不只是分类，它决定了整个对话的处理方式。这是第七课和第六课的核心区别——第六课每次都注入 RAG，第七课由路由决定。

## Demo 与生产的差距
Demo 用 LLM 做三分类，生产中可能需要更复杂的路由：多标签分类（一个消息可能既是 KNOWLEDGE 又是 TOOL）、意图层级（TOOL 下分查价格/查库存/下单）、路由缓存（相似问题直接复用路由结果）。Demo 的路由是无状态的，生产中可能需要考虑对话上下文（前一轮是 KNOWLEDGE，这一轮说"多少钱"应该路由到 TOOL）。

## Transition
路由决定了注册什么 Tool。那 LLM 怎么知道这些 Tool 的存在？看 Function Calling 的完整链路。
