## Slide Objective
展示 context 的四课演进和 PromptManager 的三段规则——学员看到 Agent 能力如何通过 context 填充逐步增强。

## Suggested Presentation
context 是 `PromptManager.buildUserPrompt(message, context)` 的第二个参数。每课都在填充它。

**第四课**：context = null。纯 Prompt，没有任何上下文。LLM 只看到系统提示词和用户消息。

**第五课**：context = 对话历史 + 用户画像。`SessionMemory` 从 Redis 取历史，`UserProfileMapper` 从 MySQL 取画像。LLM 知道"用户之前说了什么"和"用户是谁"。

**第六课**：context = 对话历史 + 用户记忆 + RAG 知识。新增 `UserMemoryService` 从 pgvector 语义检索记忆，`RetrievalService` 从 pgvector 语义检索商品知识。LLM 知道"用户之前的需求"和"商品库有什么"。

**第七课**：全部通过 MCP。画像通过 `McpKnowledgeClient.readUserProfile()`（MCP Resource）获取，记忆通过 `McpKnowledgeClient.searchMemory()`（MCP Tool）获取。RAG 通过 `McpKnowledgeClient.search()`（MCP Tool）获取。**路由决定注入内容**——只有 KNOWLEDGE 路由才注入 RAG。

### PromptManager 三段规则

**行为规则**："用户问购物相关问题时，基于商品库知识推荐；非购物问题正常聊天。"这告诉 LLM 什么时候该推荐商品。

**知识使用规则**："如果参考知识中有商品知识，请优先基于这些知识回答；没有则基于常识，但不要编造商品信息。"这告诉 LLM 怎么使用 RAG 结果。

**记忆使用规则**（第七课新增）："「参考信息」中的用户记忆，记录了用户之前的需求和决策。推荐商品时，结合记忆了解用户的真实需求。"这告诉 LLM 怎么使用记忆。记忆保存由系统自动处理，不需要 LLM 参与。

Prompt 的作用：不是把 Tool 暴露给 LLM 就够了，还要告诉 LLM 什么时候用、怎么用。Tool + Prompt = 完整的 Agent 能力。

### 第七课的变化

context 不再自动拼接所有内容。路由决定——KNOWLEDGE 注入 RAG，TOOL 注册 FunctionCallback，CHAT 什么都不加。这是和第六课的核心区别：第六课每次都注入 RAG（浪费 token），第七课按需注入。

## Key Emphasis
context 演进体现 Agent 能力增强。每课增加新的信息源，LLM 拥有更多上下文来做决策。但第七课的关键变化是"按需注入"——不是所有信息都需要每次都给 LLM。

## Demo 与生产的差距
Demo 的 context 构建是同步的——每次对话都要查画像、查记忆、可能查 RAG。生产中应该做异步预加载——画像和记忆在会话开始时预加载到缓存，对话时直接读缓存。Demo 没有做 token 预算控制——context 可能超过模型的 context window。生产中需要按优先级截断：画像（必须）> 记忆（重要）> 对话历史（可截断）> RAG（按需）。

## Transition
Demo 做了简化，和生产环境有什么差距？
