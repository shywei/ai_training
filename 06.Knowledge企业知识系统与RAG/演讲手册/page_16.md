## Slide Objective
讲透企业级 RAG 的四个核心优化方向（Cache 语义缓存、真正的 Hybrid Search 混合检索、Query Rewrite 与多查询扩展、RAG Evaluation 量化评测）——每个都是生产必备，明确它们的落地优先级与在现有 `v1.3` Demo `RetrievalService` 上的扩展落点。**严正强调：混合检索必须是“稠密向量 + 稀疏词法 BM25 + RRF 融合”，绝不能拿 SQL 元数据过滤充数；Reranker 精排已在 P12 混合检索与 P15 双管线详述，本页重点讲透查询改写的适用场景与策略。**

## Suggested Presentation
这一页的四个优化方向，`v1.3` Demo 为了保持教学主干清晰极简没有全部内置，但在企业生产环境中，这四件事是系统走向工业可用性的分水岭。

### 1. Cache 语义缓存（第 1 个做，性价比最高）
相同或高度相似的问题被用户高频重复提问（如"有什么适合女生的轻薄笔记本推荐"），每次都调一次 Embedding 计算、查一次 pgvector、再调一次 LLM 深度生成，既浪费 Token 又拉长延迟。
- **实现手段**：在 `RetrievalService.retrieve()` 或 `ChatService` 最前置加一层 Redis 语义缓存——存入高频典型提问的 Embedding 向量与标准回答，设定 TTL。新问题进来先计算向量余弦相似度，高于 `0.92` 直接命中并秒级返回，端到端延迟从 3~5 秒直接降到几十毫秒，成本断崖式下降！

---

### 2. 真正的 Hybrid Search 混合检索（第 2 个做，解决型号与专有名词硬伤）
**【P0 级纠正强化】** 在这一项，讲师必须再次敲黑板提醒学员：**千万不要把 SQL 的 `WHERE category = '笔记本'` 叫做混合检索！元数据过滤只是对候选范围的硬性裁剪，真正的混合检索是多路异构相关性特征的召回与融合！**
- **痛点**：向量检索擅长语义泛化，但对型号、SKU（如 `"ThinkPad X1 Carbon Gen12"`、`"NB001"`）、专业代码错误码极不敏感；BM25 全文检索恰好相反，能精准命中字面关键词。
- **工业实现**：PostgreSQL 原生自带 `tsvector / tsquery` 全文检索能力（单库即可搞定，无需额外部署庞大的 Elasticsearch 集群）。在 `RetrievalService` 中：
  1. `pgvector` 稠密向量召回 Top-20；
  2. PostgreSQL 全文检索（BM25 词法）召回 Top-20；
  3. 通过 **RRF（Reciprocal Rank Fusion，倒数排名融合算法）** 合并候选集并去重。双路优势互补，彻底解决专有名词漏召回的痛点。

---

### 3. Query Rewrite 与多查询扩展（第 3 个做，解决口语与表达不对齐）
- **痛点**：用户在真实多轮会话中提问往往极端口语化、充满代词（“那刚才那台电脑重量是多少？”、“有没有散热更好的？”）或者单次包含多重意图，直接拿原始用户输入去检索向量库和 BM25，命中率严重扑街。
- **工业实现**：在多路检索之前引入轻量快模型执行查询改写：
  1. **指代消解与补全（Coreference Resolution）**：结合短期会话历史，将“那台电脑”重写还原为“联想 ThinkPad X1 2026 款”；
  2. **多查询并行扩展（Multi-Query Expansion）**：从不同视角生成 2~3 个同义但侧重不同的衍生 Query 并发检索，再合并候选集（扩大召回视野）；
  3. **假设性文档嵌入（HyDE）**：对于开放式长问题，让快模型先生成一段虚构但符合事实结构的假设性答案，再以该答案的向量做检索。
  （注：经此步改写召回后，再由 P12/P15 的 Cross-Encoder Reranker 精排，形成“改写扩召回 $\to$ 混合双路 $\to$ 交叉精排”的高性能组合拳）。

---

### 4. RAG Evaluation 量化评测（第 4 个做，贯穿全周期的度量衡）
- **痛点**：没有量化指标，调 Chunk Size、换 Embedding 模型、改 Prompt、换分词规则全靠工程师的“玄学主观感觉”，一旦改动导致某些问题退化根本无法察觉。
- **工业实现**：构建包含真实用户高频提问、标注答案要点与标准文档 ID 的黄金评测集，通过自动化脚本在 CI/CD 中定期跑回归，自动计算 **Recall@K（召回率）、Precision@K（精确率）、Faithfulness（忠实度/防幻觉度）与 Response Relevancy（响应相关度）**。下一页（Page 17）将专门展开其指标与防退化方案。

---

### 生产落地优先级排序
按“改动最小、见效最快”的工程法则排序：
**Cache 语义缓存 → 真正的 Hybrid Search（向量+BM25 RRF） → Query Rewrite 改写扩展 → RAG Evaluation 量化评估**。
（注：Evaluation 的黄金基线集越早开始积累越好）。

## Key Emphasis
1. 牢记混合检索的工业标准：Dense + BM25 + RRF，坚决澄清元数据过滤不是混合检索。
2. 理解四项优化的协同价值：Cache 降本提速，Hybrid 保型号召回，Query Rewrite 拓宽口语召回，Evaluation 闭环防退化。
3. 四项优化均可在现有的 `v1.3` Demo `RetrievalService.java` 上以装饰器或责任链模式平滑扩展。

## Optional Extension
**学员问：如果在公司内部推行这四项优化，大概需要多少人力和周期？**
答：
- 第一步（Cache 语义缓存）：基于 Redis 现有实例，1~2 天即可上线，立即降低 30%~50% 重复提问的 API 账单；
- 第二步（Hybrid Search）：基于现有的 PostgreSQL 增加 `tsvector` 列和 GIN 索引，写一个 RRF 融合 SQL，2~3 天可搞定；
- 第三步（Rerank）：部署一个 `bge-reranker-v2-m3` 推理服务（或者调云端 Rerank API），接入只需增加一次 HTTP 调用，1 天搞定；
- 第四步（Evaluation）：关键在收集标注 30~50 条黄金测试用例，通常需要业务专家配合迭代 1 周。
整套优化可在两周内全量交付，让企业 RAG 系统的准确度发生质的飞跃。

## Classroom Interaction
让学员在 Demo 中连续问两次一模一样的"推荐一台写代码的笔记本"，观察两次耗时完全一样（都要 2~3 秒）——直观理解为什么语义缓存是生产落地的第一顺位；接着问学员如果输入“请问 ThinkPad X1 的 CPU 是什么型号”，为什么单靠向量检索容易搜出 MacBook，引导学员指出必须补齐 BM25 混合检索。

## Take Away
生产级 RAG 四件套：语义缓存降延迟、真正的 Hybrid 融双路、交叉精排保精度、量化评测防退化。

## Transition
四件套里的第四件——RAG Evaluation 是所有调优的“裁判”。黄金评测集怎么建？三大指标怎么算？间接提示词注入怎么防？我们翻到第 17 页细看。
