## Slide Objective
回顾第六课的具体技术实现，为第七课的升级建立基线。

## Suggested Presentation
第六课做了四件事，每件都有具体的代码和架构。

**RAG 知识检索**：`RetrievalService.java` 直接调用 `PgVectorStore.similaritySearch()`，把用户问题 Embedding 后在 pgvector 中做余弦相似度搜索。数据流是：用户消息 → Embedding → pgvector cosine similarity → Top-3 结果 → 注入 context。商品描述（名称、配置、亮点、适用场景）存在 `vector_store` 表，每个商品一个 Chunk。

**记忆增强**：`UserMemoryService.java` 直接调用 `UserMemoryMapper.java`，后者用 `pgvectorJdbcTemplate` 操作 `user_memory` 表。自动沉淀机制：每 10 条消息触发一次，LLM 从对话中提取关键信息，存入 pgvector。下次对话时，语义检索命中相关记忆。

**数据分层**：商品描述放 pgvector（RAG），价格库存放 MySQL。但价格库存在第六课没有被使用——Agent 无法查询。

**数据库直连**：`VectorStoreConfig.java` 手动创建 PostgreSQL 数据源，注入 `PgVectorStore`。`UserMemoryMapper.java` 直接用 `pgvectorJdbcTemplate` 写 SQL。`UserProfileMapper.java` 用 MyBatis Plus 操作 MySQL。Agent 和三个数据库直接耦合。

第七课的变化：同样的数据，同样的 MySQL 和 pgvector，但访问方式变了——全部通过 MCP 协议。

## Key Emphasis
第六课的"直连"不是错误，是当时最合理的实现。第七课的"MCP 化"不是推翻，是架构升级。两者是递进关系。

## Transition
第七课要解决三个问题：LLM 怎么知道该做什么（路由）、工具怎么标准化暴露（MCP）、LLM 怎么调用工具（Function Calling）。
