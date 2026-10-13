## Slide Objective
讲透 RAG 的四个企业级优化方向——每个都是"生产必备"，不是"高级选修"。学员做项目时一定会遇到这些问题，必须知道"是什么、为什么需要、怎么落地"。

## Suggested Presentation
这一页的四个优化方向，Demo 都没做，但它们不是"高级话题"，而是"生产必备"。学员做项目时一定会遇到。

### Cache（第1个做）

相同或相似的问题被反复问，每次都走检索+生成，浪费 token 和时间。"你们有什么笔记本"被问了 100 次，不需要每次都检索+生成。第一次的结果缓存，后面直接返回。

语义缓存更进一步。"推荐笔记本"和"有什么笔记本推荐"字面不同但语义相同，可以共用缓存。实现方式：用 Redis 存问题的 Embedding + 结果。新问题先算 Embedding，和缓存中的 Embedding 做相似度匹配，超过阈值直接返回。

改动最小，效果最明显。Demo 可以在 `RetrievalService` 前加一层 `CacheService`。学员做项目时，第一个优化就做这个。

### Hybrid Search（第2个做）

向量检索擅长语义理解，但对专有名词不敏感。用户问"ThinkPad X1 Carbon 多少钱"，向量检索可能匹配不到，因为"X1 Carbon"是型号，Embedding 模型不一定理解它的含义。但 BM25 关键词检索能精确命中——只要文档里包含"X1 Carbon"这几个字。

两者融合，各取所长。pgvector 的好处是 PostgreSQL 原生支持全文搜索（tsvector），不需要引入额外组件。实现上：先做向量召回 Top-K，再做 BM25 召回 Top-K，合并去重排序。

Demo 的 `isProductRelated()` 关键词过滤是 BM25 的简化版。升级到 Hybrid Search 后，可以去掉关键词过滤，让检索引擎自己处理。

### Rerank（第3个做）

向量检索返回的结果不一定按相关度排序。cosine similarity 是粗排，精度有限。Top-3 里可能混入不相关的结果，或者最相关的结果排在后面被 Top-K 截断。

Rerank 用交叉编码器（Cross-Encoder）对 query 和每个候选文档做精细打分。和向量检索的双编码器（Bi-Encoder）不同，交叉编码器能捕捉 query 和文档之间的细粒度交互，排序精度更高。

实现：在 `RetrievalService.retrieve()` 的 `similaritySearch()` 之后、`formatResults()` 之前，加一步 `rerank()`。调用 BGE-Reranker 或 Cohere Rerank API，对 Top-K 结果重新打分，取 Top-N。

### RAG Evaluation（第4个做）

"为什么检索不到"是企业里最常被问的问题。没有量化指标，调优就是盲人摸象——改了 Chunk Size，不知道是变好了还是变差了。

核心指标三个：Recall（正确文档是否被召回）、Precision（召回的文档是否相关）、Answer Relevancy（LLM 的回答是否基于检索结果）。准备评测集——一组"问题 + 标准答案 + 相关文档 ID"的数据。跑一遍检索，计算 Recall@K 和 Precision@K。

Demo 可以加一个 `/api/knowledge/evaluate` 接口，读取评测集，自动计算指标。学员做项目时，第一件事就是建评测集——没有评测集，所有调优都是盲目的。

### 生产落地优先级

学员做项目时，按这个顺序落地：Cache（最简单，改动最小）→ Hybrid Search（解决专有名词匹配）→ Rerank（提升排序精度）→ Evaluation（量化调优基础）。

## Key Emphasis
这四个优化方向是知识体系的完整部分，不是"高级选修"。学员做项目时一定会遇到，必须知道"是什么、为什么需要、怎么落地"。

## Demo 与生产的差距
Demo 没有任何优化。每个优化方向都可以在现有代码上扩展：Cache 可以在 `RetrievalService` 前加一层，Hybrid Search 可以在 `retrieve()` 里加 BM25 召回，Rerank 可以在 `formatResults()` 前加一步。学员知道从哪里开始补。

## Transition
了解了这些企业级优化手段，接下来让我们切回 Demo，看看当前在 v3.0 版本中，我们做出了一个什么样的骨架，以及如何在演示中验证这些逻辑。
