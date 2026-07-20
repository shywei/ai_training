## Slide Objective
对比第六课和第七课的数据分层，讲清"为什么不直接查数据库"。

## Suggested Presentation
第六课：Agent 直连数据库。`RetrievalService` → pgvector，`UserMemoryMapper` → pgvector，`UserProfileMapper` → MySQL。Agent 和三个数据库直接耦合。价格库存？第六课没有——Agent 无法查询实时数据。

第七课：全部通过 MCP。商品知识通过 MCP Tool → mcp-knowledge-server → pgvector。用户记忆通过 MCP Tool → mcp-knowledge-server → pgvector。用户画像通过 MCP Resource → mcp-knowledge-server → MySQL。价格库存通过 MCP Tool → mcp-product-server → MySQL。

数据没变，还是那些 MySQL 和 pgvector。变的是访问方式。

看起来多了一层网络调用，但价值很大：

**解耦**：Agent 不依赖具体数据库。pgvector 换成 Milvus，只改 mcp-knowledge-server 的实现，agent-runtime 代码不变。

**复用**：MCP Server 可以被多个 Agent 调用。导购 Agent 和客服 Agent 都可以调用同一个 mcp-knowledge-server。

**标准化**：Tool 有统一的暴露和发现机制。任何支持 MCP 的 Agent 都能自动发现和调用 Tool。

**独立部署**：知识服务和商品服务可以独立扩缩容。商品查询是高频操作，可以多部署几个 mcp-product-server 实例。

agent-runtime 不直接访问任何数据库——这是第七课最重要的架构变化。`VectorStoreConfig.java`（PostgreSQL 数据源配置）、`UserMemoryMapper.java`（pgvector SQL 操作）、`RetrievalService.java`（向量检索）全部删除。agent-runtime 的 pom.xml 中移除了 `spring-ai-pgvector-store-spring-boot-starter` 和 `postgresql` 驱动。

## Key Emphasis
MCP 化的代价是多一层网络调用（毫秒级），收益是解耦、复用、标准化、独立部署。在企业场景中，收益远大于代价。

## Transition
context 怎么演进的？Prompt 怎么配套？
