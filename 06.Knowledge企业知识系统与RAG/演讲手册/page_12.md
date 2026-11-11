# Page 12 — 【P0 纠正】检索过滤 vs 混合检索 — 约束边界与真正的 Hybrid Search

## Slide Objective
**【P0 级核心知识正本清源】** 彻底纠正行业与课堂中极易出现的混淆概念——**严正声明：元数据过滤（Metadata Filtering，如 SQL WHERE 过滤）只是对候选范围的硬性裁剪，绝对不是混合检索！真正的混合检索必须是“稠密向量检索（Dense Vector）+ 稀疏词法检索（Sparse / BM25）+ 倒数排名融合（RRF）”**。对照 Demo 中的 `isProductRelated()` 门控与 `withFilterExpression`，建立清晰的检索技术进阶路线。

## Suggested Presentation

### 一、开场定调：一场必须纠正的技术概念混淆
很多工程师在面试或做架构设计时，经常脱口而出：“我们系统做了混合检索，就是向量检索的时候带上 `WHERE category = '笔记本'` 过滤一下分类。”
听到这种回答，资深架构师立刻能识破其对 RAG 原理一知半解。
**必须严正澄清：SQL 的 WHERE 过滤叫元数据过滤（Metadata Filtering），属于检索范围的硬性约束，它绝不是混合检索！**

---

### 二、元数据过滤 vs 真正混合检索的本质区别

#### 1. 元数据过滤（Metadata Filtering）— 硬性范围约束
- **底层原理：** 本质就是数据库的标量条件过滤（如 SQL `WHERE tenant_id = 'T01' AND status = 1`）。
- **执行时机：** 
  - **前过滤（Pre-filtering）：** 先通过 WHERE 筛出符合条件的记录，再在剩余子集中做向量相似度计算（pgvector 原生支持通过 HNSW 与标量索引联合查询）；
  - **后过滤（Post-filtering）：** 向量检索 Top-100，然后在内存中剔除不符合元数据条件的记录（易发生召回不足）。
- **核心特征：** 它非黑即白，**根本没有改变任何相关性打分算法**。对于“搜精确型号 NB001 搜不到”、“专业代码错误码被语义稀释”的痛点，元数据过滤完全无能为力。

#### 2. 真正的混合检索（Hybrid Search）— 完整四阶段工程范式
- **标准四阶段工程范式：**
  - **第一阶段 · 双路召回（Dual-Path Retrieval）：**
    - **稠密向量（Dense）：** 基于 Embedding 向量空间，擅长捕获**抽象概念、近义词表达、跨语言语义**，但对精确型号容易稀释；
    - **稀疏词法（Sparse / BM25）：** 基于倒排索引，擅长**精准匹配字面专有名词、产品型号（如 X1 Carbon Gen12）、报错代码**；
  - **第二阶段 · 结果融合（Fusion）：**
    - **RRF 倒数排名融合（Reciprocal Rank Fusion）或加权评分：**
      $$\text{RRF\_Score}(d) = \sum_{m \in \{\text{dense}, \text{sparse}\}} \frac{1}{k + \text{rank}_m(d)} \quad (k=60)$$
      RRF 抛弃原始分数量纲，纯看候选列表排位，公平合并双路候选集；
  - **第三阶段 · 候选结果重排序（Reranking）：**
    - 采用 **Cross-Encoder 交叉编码模型（如 bge-reranker-v2-m3）** 对 Query 与候选文档计算深度交互注意力得分，重新打分和排序。**特别强调：RRF 是排名融合算法，Cross-Encoder 是相关性重排模型，两者层次不同、分工明确！**
  - **第四阶段 · 上下文组装（Context Assembly）：**
    - 针对重排后的 Top-K 结果进行去重、Token 预算配额截断与防断章组装。

---

### 三、Demo 现状与企业检索演进三级跳
看右侧 Demo 代码与进阶路径：
- **Stage 1（Demo 现状）：** 
  `RetrievalService.isProductRelated()` 维护关键词白名单，闲聊直接返回 `null`，命中则走单路 pgvector 稠密检索。这是教学演示的最小闭环，做到了**“前置门控零延迟、零额外 Token 浪费”**；
- **Stage 2（范围约束）：** 
  在 Spring AI `SearchRequest` 中调用 `withFilterExpression("category == '笔记本'")`，落地标准的 **Metadata Filtering**；
- **Stage 3（生产终局）：** 
  落地真正的 **Hybrid Search**：利用 PostgreSQL 自带的 `tsvector / tsquery`（或者外挂 Elasticsearch）构建 BM25 倒排索引，与 `pgvector` 稠密检索双路召回各 Top-20，通过 RRF 合并去重后取 Top-5。

## Key Emphasis
1. 牢记概念边界：元数据过滤是 WHERE 约束，混合检索是 Dense + BM25 + RRF。
2. 理解 RRF 的数学价值：不看绝对分、只看相对位，消除量纲差异。
3. 明确 Demo 范围：Demo 演示了关键词门控与元数据预留，生产必须双路 RRF。

## Optional Extension
**学员问：PostgreSQL 一个数据库真的能同时搞定混合检索吗？需不需要额外部署 Elasticsearch？**
答：完全不需要！现代 PostgreSQL 是真正的多模数据库：
- **向量检索：** 挂载 `pgvector` 扩展，建 `HNSW` 索引；
- **全文检索（BM25）：** 原生支持 `tsvector` 全文索引与 `ts_rank_cd` 词法打分，配合 `jieba` 中文分词插件；
- 在同一个 SQL 查询中利用 CTE（公用表表达式）分别查出向量 Top-20 和全文 Top-20，在 SQL 内部一行代码算完 RRF 融合！单库搞定，运维成本比维护 ES 集群低 10 倍。

## Classroom Interaction
现场提问学员：“如果用户在搜索框里输入‘笔记本 ThinkPad X1 报错 ERR_502’，在这个查询中，哪些词靠 Dense 召回？哪些词靠 BM25 召回？”
引导学员分析：“笔记本”走语义泛化（Dense），“ThinkPad X1”与“ERR_502”走精确倒排命中（BM25）。双路融合才不会搜出一堆无关的报错文档。

## Take Away
元数据过滤只做范围裁剪，混合检索方显多路威力。稠密懂语义，稀疏扣型号，RRF 排名化解量纲差。

## Transition
澄清了混合检索的核心概念后，我们进入第五章（Page 15），把整个 RAG 知识处理链路拉长为完整的“企业级 10 步 RAG Pipeline”，重点看第 9 步上下文组装的独门绝技。
