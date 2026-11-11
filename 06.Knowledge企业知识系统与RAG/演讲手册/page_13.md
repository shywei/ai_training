## Slide Objective
展示 `context` 从 v1.0 到 v3.0（Git 标签 `v1.3`）的演进过程，并串起 `v1.3` 中 `KnowledgeController`（`POST /api/knowledge/index`）→ `KnowledgeService.indexProducts()` 与 `RetrievalService.retrieve(query)` → `MemoryManager.buildContext()` 的完整代码落点。

## Suggested Presentation
`context` 是整个 Agent 的核心上下文参数。第四课 `buildUserPrompt(message, context)` 预留了这个扩展点，每课逐步填充，Agent 能力随之逐级跃升：
- **v1.0（第四课）**：`context = null`。纯 Prompt，每次对话都是陌生人。
- **v2.0（第五课）**：`context = 用户画像（MySQL）+ 对话历史（Redis）`。Agent 记住用户了，但不知道公司卖什么商品。
- **v3.0 / Git tag `v1.3`（第六课，当前版本）**：`context = 用户画像 + 相关语义记忆（pgvector）+ 对话历史 + RAG 检索结果（pgvector）`。Agent 既懂用户，又懂商品了！
- **v4.0（第七课预告）**：再加上 Tool 调用结果，从"能说"升级到"能做"。

看左下角的 `v1.3` 核心代码闭环（全部位于单体模块 `agent-runtime` 内）：
1. **知识入库链路**：调用 `POST /api/knowledge/index`（`KnowledgeController`）触发 `KnowledgeService.indexProducts()`，读取 `products.json` 的 26 条商品生成 `List<Document>`，通过 `vectorStore.add(docs)` 批量向量化写入 `PgVectorStore`。
2. **知识检索链路**：当用户提问时，`RetrievalService.retrieve(query)` 构建 `SearchRequest.query(query).withTopK(5).withSimilarityThreshold(0.5)`（阈值与 Top-K 可按需设为 `3~5` 和 `0.3~0.5`），调用 `vectorStore.similaritySearch(req)` 召回最匹配的 `Document` 并格式化。

看右边的 `v3.0` context 四部分组装（由 `MemoryManager.buildContext()` 统一编排）：
- **① 用户画像（MySQL）**：通过 `UserProfileMapper` 读取结构化偏好与事实标签。
- **② 相关记忆（pgvector）**：通过 `UserMemoryService.search(userId, userMessage)` 从 `user_memory` 表语义检索历史对话沉淀片段。
- **③ 对话历史（Redis）**：通过 `SessionMemory.getMessages(sessionId)` 读取当前会话缓冲。
- **④ 参考知识（pgvector）**：通过 `RetrievalService.retrieve(userMessage)` 从 `vector_store` 表检索相关商品知识。

底部是 **Token 预算分配**：画像 10%、历史 30%、知识 40%、余量 20%（留给模型推理和输出）。

## Key Emphasis
学员应该理解：1) Agent 能力是沿着 `context` 逐层叠加演进的；2) 在 `v1.3` 中，索引（`KnowledgeService.indexProducts()`）与检索（`RetrievalService.retrieve()`）均基于 Spring AI `PgVectorStore` / `SearchRequest` 直接在 `agent-runtime` 内完成。

## Optional Extension
**学员问：context 四部分都塞进去，超出模型窗口或挤占输出空间怎么办？**
答：必须定预算、按优先级截断！
Demo 做法：打开 `logs/llm-chat.log` 查看 `[User Prompt]` 中四块内容的实际长度。
生产做法：按"知识 40% > 画像 10% > 历史 30%"定 Token 配额，超长时优先裁剪旧历史对话，绝不能把刚检索出来的核心知识截掉。

## Classroom Interaction
让学员对照 `MemoryManager.java` 说出 `v1.3` 的四块上下文分别来自哪三个存储引擎（MySQL、Redis、PostgreSQL + pgvector）。

## Take Away
`context` 是分层拼装的工程容器——在 `v1.3` 中由 `UserProfileMapper`、`UserMemoryService`、`SessionMemory`、`RetrievalService` 四路汇聚，并受 Token 预算约束。

## Why This Slide Matters
跳过这页，学员看不清 RAG 检索结果在整条 Agent 上下文装配链中的位置，容易把记忆、画像、知识混为一谈。

## Transition
`context` 的四部分拿到手了，下一步是最关键的"最后一公里"：检索结果怎么格式化注入 Prompt？怎么防止外部文档里的恶意指令劫持大模型？（page_14）
