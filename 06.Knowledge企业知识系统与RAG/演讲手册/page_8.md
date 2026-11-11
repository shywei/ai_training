## Slide Objective
讲透 Data Pipeline 与 Chunking——从基础分块（条目分块、Markdown 标题 + 递归分块）升级到 **2026 企业级两大高阶分块范式：Parent-Child Retrieval（父子文档索引）与 Anthropic Contextual Retrieval（上下文背景前缀注入）**，并对照展示 `v1.3` Demo 的 `KnowledgeService` 代码与长文档生产代码。

## Suggested Presentation
Data Pipeline 是把原始知识变成可检索向量的流水线：解析 ETL → Chunking 分块 → Metadata 打标 → Embedding 向量化 → 写入 `PgVectorStore`。

先解决一个核心矛盾——**Chunk Size 的两难悖论**：
- **Chunk 切太小（如 150~200 字）**：语义纯净，向量检索算余弦相似度时非常精准；但一旦切碎，上下文丢了、主语指代断了（比如只剩一句"该机型支持 7 天无理由退货"，不知道"该机型"是谁），LLM 拿半截话只能瞎编。
- **Chunk 切太大（如 1500 字）**：上下文完整了，但多段主题混在一个向量里，向量特征被稀释，检索命中率大幅下降。

怎么破这个两难？看左边分层的四种策略：
1. **实体/条目分块（Demo 采用）**：对于 `products.json` 这种商品目录或 FAQ，天然一个商品就是一个完整语义单元（200~300 字），直接"一商品一 Chunk"，无需切分。
2. **Markdown 标题 + 递归分块（长文档基础线）**：用 Docling / MinerU 把 PDF 转成带 `# / ## / ###` 标题树的 Markdown 后，先按章节标题切，超长章节再用 `RecursiveCharacterTextSplitter` 按段落/句子边界递归切分，保留 10%~20% 重叠（Overlap）。
3. **★ 2026 企业必选一：Parent-Child Retrieval（父子文档索引 / Small-to-Big）**
   把"用于检索的块"和"喂给 LLM 的块"解耦！长文档先按章节切成 1000~2000 字的**父块（Parent Chunk）**落库保存；再把父块切成 200 字的**子块（Child Chunk）**做 Embedding 进 pgvector。检索时用**子块高精度匹配**，命中后通过 `metadata.parent_id` **把完整的父块捞出来喂给 LLM**——既有小块的检索准度，又有大块的完整上下文！
4. **★ 2026 企业必选二：Anthropic Contextual Retrieval（上下文背景前缀注入）**
   为什么长文档子块常检索不到？因为指代丢失（Chunk 里只有"保修期为 2 年"，没写是哪款产品、哪份文件）。Anthropic 提出的标准做法是：在切片入库前，把整篇文档（利用 Prompt Caching 缓存全文前缀，成本极低）和当前切片发给轻量模型，生成一段 **50 字以内的背景说明前缀**（例如：`[背景：本文出自联想 ThinkPad X1 2026 保修手册第2章整机质保条款]`），拼在原始切片前面一起做 Embedding 和 BM25 索引。实测配合 Rerank 可将检索失败率降低 **67%**！

看右边两段代码对照：
- 上方是本课 `v1.3` Demo 的 `KnowledgeController` / `KnowledgeService`：读取 `products.json`，把商品的名称、品牌、类别、配置、亮点、适用场景、总结拼成 `content`，打上 `doc_id / name / brand / category` 四个 Metadata，构造 Spring AI 的 `Document` 对象调用 `vectorStore.add()` 入库。注意：**不含实时价格与库存**。
- 下方是生产级长文档入库范式：Docling 解析 PDF → 按 Markdown 标题切父块保存 → 切 200 字子块并注入章节/上下文背景前缀 → 带 `parent_id` 写入同一个 `vectorStore`。两条代码路径最终汇入同一个 `PgVectorStore`！

## Key Emphasis
必须讲清：① 条目型知识按业务实体边界一商品一 Chunk（`KnowledgeService`）；② 长文档绝不能固定字数裸切，生产标准组合是 **Markdown 标题切分 + Parent-Child 父子索引（小块检索、大块生成） + Contextual Retrieval（上下文前缀注入防指代丢失）**。

## Demo 与生产的差距（三格）
**✗ 仅实现条目级整块拼接，未内置长文档父子索引与上下文前缀注入**
问题：`v1.3` Demo 的 `KnowledgeService` 是"一个商品 = 一个 Document"，没有针对几十页 PDF 手册的章节切分、父子映射与背景前缀生成。
不解决会怎样：一旦把几十页的《退换货规定 PDF》按固定 500 字切块塞进库里，指代丢失 + 跨段截断会让检索召回率和回答准确率双双崩盘。
怎么解决：长文档走 **Docling/MinerU → MarkdownHeaderTextSplitter（父块） → TokenTextSplitter（子块） + Contextual 前缀生成**，检索命中子块后按 `parent_id` 回传父块。取舍：Contextual Retrieval 入库时多一次轻量 LLM 调用（开启 Prompt Caching 后每百万 Token 仅需几毛钱），但换来线上检索质量质的飞跃。

## Optional Extension
**学员问：Parent-Child 和 Contextual Retrieval 要同时上吗？**
答：看痛点在哪——"上下文残缺导致 LLM 答不全"上 Parent-Child；"子块脱离全文后缺乏主语搜不到"上 Contextual 前缀。两者完全正交，在企业制度/手册库里经常组合使用（即使只把章节路径 `[文档名 | 一级标题 > 二级标题]` 静态拼到子块头部，也是零成本的轻量级 Contextual Retrieval！）。

## Classroom Interaction
拿一句脱离上下文的切片——`"第四条：自签收之日起 7 日内若出现性能故障，可选择退货、换货或修理"`——问学员：如果用户搜`"ThinkPad 屏幕坏了能退吗"`，这句话为什么向量检索很难排到第一？（因为切片里根本没有"ThinkPad"和"屏幕"！）加上 `[文档：联想 ThinkPad 笔记本三包售后政策 | 章节：整机与显示屏退换规定]` 前缀后再看，瞬间豁然开朗。

## Take Away
商品条目按实体边界切（一商品一 Chunk）；长文档用 **Parent-Child（小块检索、大块回传）+ Contextual Retrieval（注入文档背景前缀）** 破解粒度与完整性的两难。

## Why This Slide Matters
跳过这页，学员回去处理公司 PDF 制度时只会用固定字数暴力切分，检索出没头没尾的半句话，后面换再好的大模型也救不回来。

## Transition
Chunk 切好并补齐上下文前缀了，下一步是：怎么把文本变成向量？2026 年主流 Embedding 模型怎么选？（page_9）
