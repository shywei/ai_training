## Slide Objective
讲透 Embedding 的原理、2026 企业级主流模型选型（`BGE-M3`、`Qwen3-Embedding`、`OpenAI text-embedding-3-large`，淘汰过时的 2022 `ada-002`）、**Late Chunking（延迟分块）** 以及维度对齐与换模型重建的工程踩坑经验。

## Suggested Presentation
Embedding 就是把文本变成一组高维浮点数（向量）。语义相近的文本，在向量空间中的距离也相近。

看右上的向量空间示意。"ThinkPad X1 Carbon"、"商务轻薄笔记本"、"MacBook Pro 14"聚在左上角；"AirPods Pro"、"无线降噪耳机"聚在右下角。"写代码"和"编程开发"字面完全不同，但在向量空间中距离极近——这就是语义检索能跨越同义词鸿沟的原因。

重点看左下角的 **2026 企业级 Embedding 模型选型**（注意：2022 年的 `text-embedding-ada-002` 早已淘汰，不要再在新项目里选型）：
1. **BGE 系列（`BGE-M3` / `BGE-large-zh-v1.5`）**：智源出品，中文开源标杆。本课 `v1.3` Demo 使用 `BAAI/bge-large-zh-v1.5`（1024 维，通过 SiliconFlow OpenAI 兼容接口调用，配置在 `agent-runtime` 的 `application.yml` 中，由 Spring AI `EmbeddingModel` 自动封装）。生产升级推荐 **`BGE-M3`**（支持 8K 上下文，且一次推理可同时输出 Dense 稠密向量 + Sparse 稀疏词权 + ColBERT 多向量，天然适配 Hybrid Search）。
2. **`Qwen3-Embedding`（阿里千问）**：2025–2026 多语言与中英双语 MTEB 榜单霸榜模型，支持 **32K 超长上下文**、**Instruction-aware（指令感知检索）** 以及灵活维度输出，国内企业私有化与云 API 首选之一。
3. **`OpenAI text-embedding-3-large`**：全面取代旧版 `ada-002` 的海外主力模型。原生 3072 维，最大的工程亮点是支持 **Matryoshka Representation Learning（俄罗斯套娃可变维度）**——可以在 API 请求时传 `dimensions: 1024` 直接取前 1024 维，向量库存储和计算成本直降 2/3，而检索评测精度几乎不掉！

再看左下的前沿机制——**Late Chunking（延迟分块）**：
上一页我们讲了 Contextual Retrieval（用轻量 LLM 给每个切片生成背景前缀）。如果不想花 LLM 生成前缀的钱，还有没有办法保住全文指代？有，就是 **Late Chunking**！传统做法是"先切块、再对每个短块独立算 Embedding"；Late Chunking 反过来——利用 `BGE-M3` / `Qwen3-Embedding` / `Jina-v3` 的 8K~32K 长窗口，**先把整篇文档输入 Transformer 编码层，让每个 Token 的表征都融合全文自注意力上下文，最后在输出池化层（Mean Pooling）再按 Chunk 边界切分池化**。零额外 LLM 调用成本，就能让切片向量自带全文语境！

最后强调右下角的踩坑经验：
- **维度必须严格对齐**：Demo 的 `bge-large-zh-v1.5` 是 1024 维，`docs/schema-knowledge.sql` 里定义的就是 `embedding vector(1024)`。
- **换模型必须重建索引**：哪怕从 `bge-large-zh-v1.5`（1024 维）换成 `Qwen3-Embedding`（截断到 1024 维），虽然维度数字相同，但两个模型的向量空间坐标系完全不同，旧向量和新 Query 算余弦相似度毫无意义，必须全量重算向量入库！

## Key Emphasis
Embedding 是语义检索的基石。选型记住 2026 三大主力（`BGE-M3`、`Qwen3-Embedding`、`text-embedding-3-large`），理解 Matryoshka 动态降维与 Late Chunking 延迟分块；但也要清楚稠密向量对精确型号/SKU（如"X1 Carbon"）不够敏感，这正是后面要上 Hybrid Search 的原因。

## Demo 与生产的差距（三格）
**✗ 单模型固定维度，未做多模型评测对比与领域术语适配**
问题：`v1.3` Demo 固定使用 `BAAI/bge-large-zh-v1.5`（Spring AI `EmbeddingModel`），未在评测集上对比 `BGE-M3` / `Qwen3-Embedding`，且单路稠密向量对具体型号串（如 `NB001`）区分度有限。
不解决会怎样：专有名词/型号检索召回率低；未来切换更强 Embedding 模型时若直接改配置不重建表数据，会导致线上检索静默失效（查不出任何结果或结果完全错乱）。
怎么解决：用黄金评测集（page_17）对比选型 `BGE-M3` / `Qwen3-Embedding` / `text-embedding-3-large`；利用 `BGE-M3` 或 `pgvector + BM25` 做混合检索；建立索引版本管理（换模型 = 建新表/双写重算 → 评测通过后切流）。

## Optional Extension
**学员问：换了 Embedding 模型，如果新老模型都是 1024 维，为什么也查不到东西了？**
答：维度相同不代表坐标系相同！就像摄氏度和华氏度都是一个数字，但刻度完全不同。
Demo 做法：对照 `agent-runtime/src/main/resources/application.yml` 中的 `spring.ai.openai.embedding.options.model` 与 `docs/schema-knowledge.sql` 的 `vector(1024)`，理解 `PgVectorStore` 是如何调用 `EmbeddingModel` 写入和查询的。
生产做法：给向量表或 metadata 加上 `embedding_model_version` 字段；一旦更换模型，触发全量重新索引（`POST /api/knowledge/index`），绝不能新老向量混查。

## Classroom Interaction
提问学员：如果公司决定把 `bge-large-zh-v1.5`（1024 维）升级为 `Qwen3-Embedding`（同样设为 1024 维），数据库表结构 `vector(1024)` 不用改，那上线前必须执行什么操作？（答：全量重跑索引，重新计算所有文档的 Embedding！）

## Take Away
2026 选型认准 `BGE-M3`、`Qwen3-Embedding`、`text-embedding-3-large`；善用套娃降维（Matryoshka）与延迟分块（Late Chunking）；牢记"换模型必重算全量向量"。

## Why This Slide Matters
跳过这页，学员还在拿几年前的 `ada-002` 立项，或者在换模型时不重算向量，导致线上检索静默崩盘。

## Transition
文本变成了 1024 维向量，接下来进入第三章：向量存在哪？为什么企业首选 pgvector？在 `v1.3` 代码里怎么配置独立数据源？（page_10）
