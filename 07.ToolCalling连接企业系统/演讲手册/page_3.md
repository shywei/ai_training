## Slide Objective
回顾第六课的具体技术实现，为第七课的升级建立基线。

## Suggested Presentation
第六课做了四件事，每件都有具体的代码和架构。

**RAG 知识检索**：`RetrievalService.java` 直接调用 `PgVectorStore.similaritySearch()`，把用户问题 Embedding 后在 pgvector 中做余弦相似度搜索。数据流是：用户消息 → Embedding → pgvector cosine similarity → Top-3 结果 → 注入 context。商品描述（名称、配置、亮点、适用场景）存在 `vector_store` 表，每个商品一个 Chunk。

**记忆增强**：`UserMemoryService.java` 直接调用 `UserMemoryMapper.java`，后者用 `pgvectorJdbcTemplate` 操作 `user_memory` 表。自动沉淀机制：满 20 条消息触发一次（提取前 10 条沉淀、保留后 10 条在 Redis），LLM 从对话中提取关键信息，存入 pgvector。下次对话时，语义检索命中相关记忆。

**数据分层**：商品描述放 pgvector（RAG），价格库存放 MySQL。但价格库存在第六课没有被使用——Agent 无法查询。

**数据库直连**：`VectorStoreConfig.java` 手动创建 PostgreSQL 数据源，注入 `PgVectorStore`。`UserMemoryMapper.java` 直接用 `pgvectorJdbcTemplate` 写 SQL。`UserProfileMapper.java` 用 MyBatis Plus 操作 MySQL。Agent 和三个数据库直接耦合。

第七课的变化：同样的数据，同样的 MySQL 和 pgvector，但访问方式变了——全部通过 MCP 协议。

## Key Emphasis
第六课的"直连"不是错误，是当时最合理的实现。第七课的"MCP 化"不是推翻，是架构升级。两者是递进关系。

## Optional Extension
**学员问：MCP 化之后，第六课直连数据库那套代码是不是白写了？**
答：不是白写，是换了部署位置。业务逻辑（SQL、检索算法）没有变，变的只是"谁连库"。
Demo 做法：第六课 `RetrievalService` / `UserMemoryMapper` 的查询逻辑下沉到 mcp-knowledge-server 的 `VectorSearchService` / `MemoryService`，agent-runtime 删掉 `VectorStoreConfig` 和 pgvector 直连，访问统一走 `McpKnowledgeClient`——逻辑平移，验证口径不变。
生产做法：迁移期新旧链路双跑对账（同一批请求比对结果），按流量灰度切换，出问题回滚到直连。取舍：平移比 rewrite 便宜在一个可验证点——同一组问题在新旧链路上的答案应该一致。第九课用 AgentScope Java 2.0 重构时是同样的搬法，到时会再对比一次。

## Take Away
架构升级要"搬访问路径、不动业务逻辑"——SQL 与检索算法原样下沉到服务端，只改连接方式，升级才可比对、可灰度、可回滚。

## Why This Slide Matters
跳过这页，学员会把"架构升级"理解成"推倒重写"，把已经调通的检索逻辑重写一遍——回归问题混在架构变更里，出了偏差定位不了是架构错了还是逻辑错了。

## Transition
第六课让 Agent "懂"了商品描述和用户记忆，那怎么让 LLM 从"懂"跨到"做"？我们看下一页（page_4）：核心问题——怎么让 LLM "做事"？
