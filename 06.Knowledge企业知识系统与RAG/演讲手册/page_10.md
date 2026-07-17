## Slide Objective
讲透语义检索的流程、关键参数、以及局限性。学员需要理解"语义检索是 RAG 的核心，但不是万能的"。

## Suggested Presentation
语义检索的流程：用户问题 → Embedding → 查询向量 → 和知识库中的向量做余弦相似度计算 → 返回最相似的 Top-K 个结果 → 相似度阈值过滤。

关键参数有三个，每个都会影响检索效果。

Top-K：返回几个结果？Demo 默认 3 个。太少了可能漏掉相关结果，太多了会注入无关信息干扰 LLM。一般 3-5 个，根据知识库大小调优。

相似度阈值：低于多少分的不要？Demo 默认 0.3。太低会返回不相关的结果，太高会漏掉相关结果。这个值需要根据实际数据调优——先设低一点看效果，再逐步提高。

距离度量：用什么算法算相似度？Demo 用 cosine（余弦相似度）。还有 L2（欧氏距离）和 inner product（内积）。cosine 最常用，适合大多数场景。

看右边的代码。`RetrievalService.retrieve()` 方法。先调 `isProductRelated()` 做关键词过滤（下一节讲），再调 `vectorStore.similaritySearch()` 做向量搜索，最后 `formatResults()` 格式化结果。

`SearchRequest.query(query).withTopK(topK).withSimilarityThreshold(0.3)` — 这一行代码就是整个检索的核心。Spring AI 封装了 pgvector 的调用细节，开发者只需要指定参数。

语义检索的局限很重要。对专有名词不敏感——用户问"ThinkPad X1 Carbon 多少钱"，向量检索可能匹配不到，因为"X1 Carbon"是型号，Embedding 模型不一定理解它的含义。需要调优（Chunk Size、Top-K、阈值都会影响效果）。这就是后面要讲 Hybrid Search 的原因。

## Key Emphasis
语义检索是 RAG 的核心，但不是万能的。学员需要理解它的能力和局限，以及关键参数的调优方向。

## Demo 与生产的差距
Demo 只有单路向量召回。生产中应该做 Hybrid Search（向量 + BM25 融合），解决专有名词匹配问题。Demo 没有 Rerank，Top-3 的排序可能不是最优的。Demo 的关键词过滤是静态的，生产中应该用 LLM 路由动态判断。

## Transition
语义检索清楚了。但有一个问题：不是所有消息都需要检索。
