## Slide Objective
展示 context 的四课演进和 PromptManager 的三段规则。

## Suggested Presentation
context 是 `PromptManager.buildUserPrompt(message, context)` 的第二个参数。每课都在填充它。

**第四课**：context = null。纯 Prompt，没有任何上下文。

**第五课**：context = 对话历史 + 用户画像。`SessionMemory` 从 Redis 取历史，`UserProfileMapper` 从 MySQL 取画像。

**第六课**：context = 对话历史 + 用户记忆 + RAG 知识。新增 `UserMemoryService` 从 pgvector 语义检索记忆，`RetrievalService` 从 pgvector 语义检索商品知识。

**第七课**：全部通过 MCP。画像通过 `McpKnowledgeClient.readUserProfile()`（MCP Resource）获取，记忆通过 `McpKnowledgeClient.searchMemory()`（MCP Tool）获取。RAG 通过 `McpKnowledgeClient.search()`（MCP Tool）获取。**路由决定注入内容**——只有 KNOWLEDGE 路由才注入 RAG。

`PromptManager.java` 有三段规则：

**行为规则**："用户问购物相关问题时，基于商品库知识推荐；非购物问题正常聊天。"这告诉 LLM 什么时候该推荐商品。

**知识使用规则**："如果参考知识中有商品知识，请优先基于这些知识回答；没有则基于常识，但不要编造商品信息。"这告诉 LLM 怎么使用 RAG 结果。

**记忆使用规则**（第七课新增）："如果参考信息中有用户记忆，请结合记忆个性化推荐；当用户表达偏好时，主动调用 save_memory 保存。"这告诉 LLM 怎么使用记忆和保存记忆。

Prompt 的作用：不是把 Tool 暴露给 LLM 就够了，还要告诉 LLM 什么时候用、怎么用。Tool + Prompt = 完整的 Agent 能力。

## Key Emphasis
context 演进体现 Agent 能力增强。每课增加新的信息源，LLM 拥有更多上下文来做决策。第七课的变化是"路由决定注入内容"——不再是自动拼接所有信息。

## Transition
Demo 做了简化，和生产环境有什么差距？
