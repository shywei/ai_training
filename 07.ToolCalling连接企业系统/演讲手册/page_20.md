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

### pgvector 内部的数据分层（Demo 未覆盖）

当前 Demo 把商品所有信息都塞进 `content` 字段，没有利用 `metadata` 列。生产中应该做 Metadata Filtering：

- `content`：纯描述文本，用于 Embedding 和语义匹配
- `metadata`：结构化字段（category、brand、price_range、tags），用于前置过滤

查询时先用 metadata 缩小范围（`category = '笔记本电脑' AND price < 10000`），再做向量相似度搜索。避免“推荐笔记本”搜到手机。

### Query Rewrite（Demo 未覆盖）

当前 Demo 直接拿用户原文做 RAG 检索，准确率低。生产中应该先用 LLM 抽取意图，生成结构化检索 query：

- 用户：“推荐一台笔记本” → LLM 分析：笔记本、无预算 → query：“笔记本电脑 轻薄 办公”
- 用户：“预算8000写代码” → LLM 分析：笔记本、编程、8000 → query：“笔记本电脑 编程 8000元”
- 用户：“多少钱” → LLM 分析：问价格、上下文是 ThinkPad → query：“ThinkPad T14s 价格”

Demo 跳过了 Query Rewrite 和 Metadata Filtering，直接拿用户原文检索。生产必须有。

## Key Emphasis
MCP 化的代价是多一层网络调用（毫秒级），收益是解耦、复用、标准化、独立部署。在企业场景中，收益远大于代价。

## Transition
context 怎么演进的？Prompt 怎么配套？
