## Slide Objective
讲透语义检索的流程、`v1.3` 中 `VectorStoreConfig.java` 与 `RetrievalService.java`（基于 Spring AI `PgVectorStore` 与 `SearchRequest`）的核心实现、关键参数调优，以及纯稠密检索的局限性。

## Suggested Presentation
语义检索的完整流程分四步：用户问题 → Spring AI `EmbeddingModel` 转为 1024 维查询向量 → 与 PostgreSQL `vector_store` 表中的向量计算余弦相似度（Cosine Similarity）→ 过滤掉低于相似度阈值的噪声，返回最相似的 Top-K 个 `Document`。

看左边的三个核心参数（对应 Spring AI 的 `SearchRequest`）：
1. **Top-K（`withTopK`）**：返回几个结果？Demo 默认 3~5 个。太少可能漏掉关键商品，太多会把不相关的商品塞进 Prompt 干扰大模型。
2. **相似度阈值（`withSimilarityThreshold`）**：低于多少分直接丢弃？Demo 设为 `0.3`（或 `0.5`）。如果用户问的问题在知识库里根本没有对应商品，阈值能把低分噪声全部拦住，返回空列表。
3. **元数据过滤（`withFilterExpression`）**：利用 `vector_store` 的 JSONB `metadata` 列，在向量搜索时直接加上 `category == '笔记本' && brand == '联想'` 条件，先缩小范围再算相似度。

看右边的 `v1.3` 核心代码（位于 `agent-runtime` 的 `knowledge/` 包下）：
- 首先在 `VectorStoreConfig.java` 中组装好 1024 维的 `PgVectorStore` Bean。
- 然后在 `RetrievalService.java` 的 `retrieve(query, topK)` 方法里：
  - 第一步调用 `isProductRelated(query)` 做轻量关键词过滤（下一页细讲）；
  - 第二步通过 `SearchRequest.query(query).withTopK(topK).withSimilarityThreshold(0.3)` 构建检索请求；
  - 第三步调用 `vectorStore.similaritySearch(request)`，Spring AI `PgVectorStore` 自动将 `query` 向量化并执行 pgvector 的 `<=>` 余弦距离查询；
  - 第四步调用 `formatResults(docs)` 将召回的 `List<Document>` 格式化为编号文本。

最后强调左下角的局限性：稠密向量擅长"意会"（"写代码"匹配"编程开发"），但对精确字面符号（型号如 `"X1 Carbon"`、商品编号 `"NB001"`）不够敏感，且双编码器（Bi-Encoder）算出的余弦分數只是粗排——这正是第五章我们要引入 **Hybrid Search（BM25+向量）** 和 **Rerank（交叉编码器精排）** 的原因。

## Key Emphasis
在 `v1.3` 中，检索核心就是 `VectorStoreConfig` + `RetrievalService.retrieve()` 里的 `SearchRequest.query(query).withTopK(...).withSimilarityThreshold(...)` 与 `vectorStore.similaritySearch(request)`。理解它的三个旋钮（Top-K、Threshold、Metadata Filter）与两大局限（专有名词弱、粗排不够准）。

## Demo 与生产的差距（三格）
**✗ 单路向量粗排直接截断 Top-K，缺少 BM25 混合召回与 Cross-Encoder 精排**
问题：`v1.3` 的 `RetrievalService.retrieve()` 只走 `PgVectorStore` 单路余弦相似度粗排并直接截取 Top-K。
不解决会怎样：用户搜精确型号/SKU 时容易漏召回；最相关的文档若在粗排中排第 4~6 名会被 `withTopK(3)` 直接截掉。
怎么解决：召回层升级为 `pgvector + PostgreSQL 全文检索（tsvector/BM25）` 双路召回 + RRF 融合；先召回 Top-20，再用 `bge-reranker-v2-m3` 交叉编码器精排取 Top-3~5。取舍：Rerank 增加约 30~100ms 延迟，但能显著提升 Top-3 准确率。

## Optional Extension
**学员问：`withSimilarityThreshold` 设 0.3 还是 0.5 还是 0.7？怎么定？**
答：不同 Embedding 模型的余弦分数分布完全不同（例如 BGE 模型输出分数普遍偏高，而某些模型偏低），绝不能拍脑袋抄别人的阈值！
Demo 做法：在 `RetrievalService.java` 中调整 `withSimilarityThreshold`（比如从 `0.3` 调到 `0.5`、`0.8`），观察同一句提问召回的 `Document` 数量变化。
生产做法：用 20~30 条包含"有答案"和"无答案（闲聊/超纲）"的黄金评测集（page_17）跑一遍分数分布，选出能挡住 95% 无关问题且不误杀正确文档的分界点作为阈值。

## Classroom Interaction
现场在 `RetrievalService.java` 里把 `withSimilarityThreshold` 临时改成 `0.85`，重启或热加载后提问"推荐一台写代码的笔记本"，看日志是不是一条都召回不出来——30 秒让学员建立对阈值旋钮的直觉。

## Take Away
`RetrievalService` 通过 `SearchRequest`（`withTopK` + `withSimilarityThreshold`）驱动 `PgVectorStore` 完成语义召回；参数靠评测集分布定，型号匹配与精排靠 Hybrid + Rerank 补齐。

## Why This Slide Matters
跳过这页，学员只会调默认 `similaritySearch(query)`，既不知道怎么设阈值挡噪声，也不知道为什么搜商品型号时向量检索会失灵。

## Transition
`RetrievalService.retrieve()` 第一行有个 `isProductRelated(query)` 判断——为什么要加这一步？是不是所有用户消息都应该去查向量库？（page_12）
