## Slide Objective
明确告诉学员 `v1.3` Demo 做到了什么、生产级知识管道还要补哪六项差距（每项按"问题 / 不解决会怎样 / 2026 生产方案"三格展开，重点将文档解析从传统 `Apache Tika` 升级为 **2026 版面感知解析 MinerU / IBM Docling + VLM 原生解析 Qwen-VL / OlmOCR**），并给出 `v1.3` 单体 `agent-runtime` 的完整项目结构与代码阅读路线图。

## Suggested Presentation
左边是 Demo vs 生产级知识管道的六项差距，每项都按"问题 → 不解决的后果 → 2026 生产级解决方案"三格拆透：

1. **✗ 1 复杂文档版面感知解析（淘汰破坏表格的传统 Apache Tika）**
   - **问题**：`v1.3` Demo 只通过 `KnowledgeService` 读取结构化的 `products.json`。而在企业里，退换货政策、保修手册、财报大量是复杂排版的 PDF。如果还用传统的 `Apache Tika` 或 `PDFBox` 裸抽纯文本，多栏排版会串行，跨页参数对比表会丢失行列对应关系变成乱码。
   - **不解决会怎样**：合同、手册、表格类知识一旦抽错，后面分块、Embedding、大模型再强也是"垃圾进、垃圾出（Garbage In, Garbage Out）"，涉及表格数据的回答必出幻觉。
   - **2026 生产方案**：全面升级为 **2026 版面感知解析（MinerU / IBM Docling）+ VLM 原生多模态解析（Qwen-VL / OlmOCR）**！
     - 对于常规图文/多栏/跨页表格 PDF：用 **IBM Docling** 或 **MinerU** 识别文档阅读顺序、保留 `# / ##` 标题层级树，并将复杂表格完整转换为 **结构化 Markdown 或 HTML `<table>`**（入库时用 LLM 生成表格摘要做向量检索，命中后把完整 HTML/Markdown 原表回传给生成模型）；
     - 对于扫描件、手写批注、复杂架构图：直接走 **VLM 原生解析（如 Qwen2.5-VL / Qwen3-VL / OlmOCR）** 端到端转结构化 Markdown。

2. **✗ 2 高阶分块（Parent-Child + Contextual Retrieval）**
   - 长文档按 Markdown 章节切 1000 字父块、200 字子块建向量索引（Parent-Child），并在入库前注入 50 字文档背景前缀（Anthropic Contextual Retrieval）消除指代丢失。

3. **✗ 3 Hybrid Search（BM25 + 向量 RRF 融合）**
   - 在 `RetrievalService` 的 `PgVectorStore` 稠密检索基础上，增加 PostgreSQL `tsvector` / BM25 全文检索路，通过 RRF 融合解决型号/SKU 精确匹配问题。

4. **✗ 4 Rerank 精排（Cross-Encoder）**
   - 双路召回 Top-20 后，接入 `bge-reranker-v2-m3` 或 `Qwen3-Reranker` 交叉编码精排取 Top-3~5。

5. **✗ 5 RAG Evaluation 与 CI 门禁**
   - 建立黄金评测集，每次调参自动跑 Recall@K / Precision@K / Faithfulness 回归测试。

6. **✗ 6 语义缓存、多部门权限隔离与安全防护**
   - 高频问题进 Redis 语义缓存；`vector_store` 按 `tenant_id / dept_id` 在 SQL 检索层做强制预过滤（PostgreSQL RLS）；用 `<retrieved_context>` 标签防御间接提示词注入。

再看右边——**`v1.3` 项目结构与代码阅读路线图**（全部在 `agent-runtime` 单体模块下）：
- 第一步：看 `knowledge/VectorStoreConfig.java`——理解如何隔离 MySQL 主数据源，手动配置 PostgreSQL 数据源并组装 1024 维 `PgVectorStore` Bean。
- 第二步：看 `knowledge/KnowledgeController.java` 与 `KnowledgeService.java`——`POST /api/knowledge/index` 如何读取 `products.json`、拼接描述文本与 metadata、调用 `vectorStore.add()` 入库。
- 第三步：看 `knowledge/RetrievalService.java`——`isProductRelated()` 关键词过滤与 `SearchRequest.query(query).withTopK(...).withSimilarityThreshold(...)` 语义检索。
- 第四步：看 `memory/UserMemoryService.java`、`UserMemoryMapper.java` 与 `MemoryManager.java`——理解语义记忆如何写入 pgvector `user_memory` 表，以及 `buildContext()` 如何组装四路上下文。

## Key Emphasis
1) 文档解析质量决定 RAG 上限：2026 年处理复杂 PDF 与跨页表格必须用 **MinerU / IBM Docling + VLM（Qwen-VL / OlmOCR）**，摒弃破坏表格结构的传统 Tika 裸抽；2) 熟记 `v1.3` 单体架构下的核心类阅读顺序：`VectorStoreConfig` → `KnowledgeController/KnowledgeService` → `RetrievalService` → `UserMemoryService` → `MemoryManager`。

## Optional Extension
**学员问：PDF 里的超大表格（比如一页有 50 行配置参数对比）转成 Markdown 后还是太长，怎么进向量库？**
答：工业界标准做法是**"摘要索引 + 原表回传"**（其实也是 Parent-Child 的变体）：用 LLM 为这张表格生成一段自然语言摘要（包含表名、列名、涉及的机型名称）作为 Child 块做 Embedding 建索引；把完整的 Markdown/HTML 原表存为 Parent 块。用户搜机型配置命中摘要后，把完整原表喂给 LLM 做精确对比回答！

## Classroom Interaction
让学员在 IDE 中切到 Git 标签 `v1.3`，对照右边的目录树依次打开 `VectorStoreConfig.java`、`KnowledgeController.java`、`RetrievalService.java`、`UserMemoryService.java`，确认每个类的职责。

## Take Away
Demo 给出了最小闭环骨架；生产落地重点补齐 **MinerU/Docling + VLM 版面感知解析、Parent-Child/Contextual 分块、Hybrid + Rerank、三元组评测与多租户安全隔离**。

## Why This Slide Matters
跳过这页，学员会把 Demo 直接搬上生产，用传统 Tika 抽乱 PDF 表格、缺少权限隔离与评测门禁，上线即翻车。

## Transition
看完了项目整体架构，我们专门聚焦 `memory/` 包下的一个重要变化：引入 pgvector 后，第五课的记忆体系在 `v1.3` 中是如何升级的？（page_21）
