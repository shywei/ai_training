# Page 15 — 完整 10 步企业级 RAG Pipeline — 离线加工与在线服务双链路

## Slide Objective
将此前零散的知识组件串联收敛为**完整 10 步企业级 RAG Pipeline 标准体系**（涵盖离线数据链路 01~05 与在线服务链路 06~10）。**深度强化与细化第 09 步“上下文组装与压缩（Context Assembly）”的四大关键工程机制**：去重过滤、Token 预算配额控制、防“迷失在中间（Lost in the Middle）”的最佳布局排序、置信度不足时的拒答与兜底机制。讲清每一步的职责、Demo 代码落点与排障定位坐标系。

## Suggested Presentation

### 一、流水线全景：为什么必须拆为完整的 10 步？
很多低质量教程把 RAG 简化为“切块、Embedding、查向量、塞进 Prompt”，这在企业生产中根本无法运转。真实的工业级 RAG 必须严格拆解为两半：
- 上半部：**离线数据加工链路（Offline Ingestion Pipeline, 01~05）**，决定知识库的“数据质量下限”；
- 下半部：**在线检索生成链路（Online Serving Pipeline, 06~10）**，决定线上问答的“实时体验与精度上限”。

---

### 二、离线数据加工链路（Steps 01~05）
看第一行卡片：
- **Step 01 · 数据接入 (Data Ingestion)：**
  从多样化企业源头采集数据：文件手动上传、MySQL/PG 数据库基于 Debezium 的 CDC 增量监听、OSS/S3 对象存储变更触发、内部知识库 API 抓取。（**Demo**：`KnowledgeService` 批量加载 `products.json`）。
- **Step 02 · 解析清洗 (Parsing & Cleaning)：**
  **2026 生产核心升级：** 坚决淘汰只提取纯文本、彻底破坏表格排版的传统 Apache Tika！采用版面感知解析工具（IBM Docling / MinerU），精准保留多级标题树状层级、跨页表格与 OCR 图像坐标。
- **Step 03 · 分块切分 (Chunking)：**
  根据业务形态选择高阶分块策略：半结构化条目采用“一记录一 Chunk”，长文档采用 Parent-Child（父子块切分）或 Anthropic Contextual Retrieval（注入 50~100 字篇章背景前缀，防止切片断章取义）。
- **Step 04 · 元数据提取 (Metadata Extraction)：**
  从文本与源数据中提取核心元数据（文档版本、所属部门、租户 ID、商品品类/品牌、最后更新时间），存入 JSONB 字段供在线阶段做**硬性范围约束过滤（Metadata Filtering）**。
- **Step 05 · 向量化与索引 (Embedding & Indexing)：**
  调用多语言强模型（BGE-M3 / Qwen3-Embedding）生成 1024 维稠密向量，写入 PostgreSQL 构建 `HNSW` 索引；**同时构建 `tsvector` 全文检索倒排索引**，为下一步的双路混合检索打下基石。

---

### 三、在线检索生成链路（Steps 06~10）
看第二行卡片：
- **Step 06 · 查询改写与门控 (Query Rewrite & Gating)：**
  - **前置门控：** 闲聊过滤（Demo 的 `isProductRelated` 关键词白名单）；
  - **多轮指代消解：** 将用户第二轮口语“那它支持快充吗？”还原为“ThinkPad X1 Carbon Gen12 是否支持快充？”；
  - **生成扩展：** 利用 HyDE（假设文档嵌入）或 Multi-Query 生成多个子查询，扩大召回覆盖。
- **Step 07 · 真正混合检索 (Hybrid Search)：**
  **绝不是单一的 SQL 过滤！** 必须是 `PgVectorStore` 稠密向量检索（召回语义相关 Top-20）+ PostgreSQL `tsvector` 稀疏全文检索（精准命中型号/专有名词 Top-20），双路并行召回后通过 **RRF 倒数排名融合算法** 合并去重。
- **Step 08 · 交叉编码精排 (Reranking)：**
  双编码器粗排的向量余弦相似度 Top-K 顺序未必最优。引入 Cross-Encoder 交叉编码器精排模型（如 `bge-reranker-v2-m3`），对 `(Query, Document)` 成对进行深层注意力交互打分，截取真正高相关的 Top-3~5。

---

### 四、🔥 重点强化：Step 09 · 上下文组装与压缩（Context Assembly）
重点展开第 09 步！很多系统前面检索打满分，最后回答却一团糟，问题全出在“上下文组装粗制滥造”上。生产级组装必须包含四大刚性工程机制：

1. **去重与语义压缩（Deduplication & Compression）：**
   - 多路召回和精排后的文档切片可能存在高度重复的内容（如多个商品页面都包含相同的“7天无理由退货声明”）；
   - 必须通过文本哈希或轻量语义去重算法剔除冗余段落，精简文本，节省宝贵的 Context Window。
2. **Token 预算严格配额（Token Budget Control）：**
   - 严禁把所有检索结果无脑全塞进去！必须在 Java 端设立硬性预算配额：
     `参考知识配额(40%) + 历史对话(30%) + 用户画像(10%) + 生成预留(20%)`；
   - 组装时按 Tokenizer 计算累计 Token，一旦超标，按优先级严格截断末尾低权重切片。
3. **防“迷失在中间（Lost in the Middle）”的最佳布局排序：**
   - 认知科学与斯坦福论文明确证实：**大模型对长上下文头部的 Token 和尾部的 Token 注意力最强，而对位于正文中间（Middle）的切片极易发生“注意力塌陷/迷失”！**
   - **生产级最佳重排布局（U-shaped Attention Ordering）：**
     如果精排出了 5 个相关切片，绝对不要按得分 1, 2, 3, 4, 5 从头排到尾！最佳实践是：
     - 最强证据（Rank 1）置顶在最上方；
     - 次强证据（Rank 2）置底在最下方（紧挨着用户问题）；
     - 较弱证据（Rank 3, 4, 5）放在正文中间。
     迎合模型的 U 型注意力曲线，让模型以最高概率采纳最关键的事实证据！
4. **置信度不足时的拒答与兜底机制（Confidence Fallback & Rejection）：**
   - 如果检索和精排返回的所有候选切片，其最高相似度/相关度得分均低于系统置信阈值（例如 Rerank 分数 &lt; 0.4），说明知识库根本没有该问题的答案！
   - **此时严禁强行将低分切片拼进 Prompt！** 强行拼接是模型产生幻觉的最核心根源。系统应在 Java 组装层直接截断，触发兜底拒答：“抱歉，商品知识库中暂未找到与您需求匹配的信息”，安全优雅兜底。

- **Step 10 · 受控生成与溯源 (Generation & Evaluation)：**
  基于 `<retrieved_context>` 标签严格生成回答，生成结果附带段落级角标溯源引用（Citation `[1]`），并在后台接入 RAG Evaluation 量化评估体系。

---

### 五、端到端质量反馈闭环（Quality Feedback Loop）
一个真正成熟的企业级 RAG 系统绝不仅是一条单向开环流水线，而必须具备**端到端质量反馈闭环**：
1. **检索阶段评估（Retrieval Evaluation）：** 持续监控离线黄金集与在线采样的 Recall@K、MRR、nDCG 指标，评估多路召回与 Rerank 的纯检索能力；
2. **生成阶段评估（Generation Evaluation）：** 基于 RAGAS 评估忠实度（Faithfulness）、答案相关性（Answer Relevance）与上下文相关性；
3. **线上失败样本收集（Bad Case Ingestion）：** 收集线上用户点踩（Thumbs-down）、客服改写、拒答错误的 Bad Cases，自动打标归类并回流评测数据集；
4. **索引与切分配置持续优化（Pipeline Continuous Tuning）：** 根据失败样本归因，反哺驱动离线索引管线（调整 Chunking 粒度、更新 Embedding/Rerank 模型、修补元数据）与在线查询管线（调优 Query Rewrite 策略与置信阈值），形成持续迭代的工程飞轮。

---

### 六、排障定位口诀
底部排障口诀帮学员将理论落到实战：
- 多轮追问搜不到 → 动 **Step 06 Rewrite**；
- 型号专有名词漏召回 → 动 **Step 07 Hybrid**（上 BM25 双路，别拿元数据过滤充数！）；
- 召回了但排第 5 被截断 → 动 **Step 08 Rerank**；
- 超窗爆 Token、注意力迷失或胡乱幻觉 → 动 **Step 09 Assemble**；
- 瞎编参数或口吻违规 → 动 **Step 10 Generate**。

## Key Emphasis
1. 建立端到端双管线全貌：离线索引加工链路保质量，在线检索生成链路出精度，配套质量反馈闭环持续迭代。
2. 讲透第 9 步上下文组装的四件套：去重压缩、Token 预算、防 Lost in the Middle 排序、低置信度拒答。
3. 牢记排障口诀与反馈飞轮：遇到 RAG 问题按 10 步定位，线上 bad case 闭环驱动管线优化。

## Take Away
RAG 是一条双管线闭环系统工程：离线加工保质量，在线检索出精度，组装排序防迷失，质量闭环促演进。
