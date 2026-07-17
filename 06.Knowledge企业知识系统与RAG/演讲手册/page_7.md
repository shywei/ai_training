## Slide Objective
讲透 Data Pipeline 和 Chunking——原始数据怎么变成可检索的知识块。重点讲清楚"从数据库来的数据怎么走 ETL 管道"。

## Suggested Presentation
Data Pipeline 是 RAG 的第一个环节。原始数据进来，经过解析、分块、标注、向量化，最后存到向量数据库。

看上面的流程图。文档解析 → Chunking → Metadata → Embedding → Vector DB。Demo 用的是 JSON，所以文档解析这一步最简单——直接读 JSON 文件。但生产中要处理 PDF（Apache Tika + OCR）、Word（POI）、Excel（EasyExcel）。PDF 是最难的——扫描件要 OCR，表格要结构化提取，多栏排版要识别布局。

但学员要记住：**知识不一定是文件**。商品导购场景下，商品信息在 MySQL 里。ETL 管道是：MySQL product 表 → 提取描述字段（名称、配置、亮点、适用场景）→ 拼成文本 → Embedding → 存入 vector_store。`products.json` 就是这个 ETL 过程的产物，Demo 里用 JSON 文件模拟。

看右边的代码。Demo 的做法是一个商品一个 Chunk。每个商品的描述、配置、亮点、适用场景拼成一个文本。因为 JSON 天然有结构边界，不需要复杂的切分逻辑。

Metadata 标注很重要。Demo 给每个 Chunk 打了 doc_id、name、brand、category 标签。这些标签存在 vector_store 的 metadata JSONB 列里，后面可以用来做过滤——比如"只在手机品类里检索"。

**多种知识共享一张表**：如果以后有 FAQ、内部文档，也存同一张 vector_store 表，metadata 加 `type` 字段区分（`type: product`、`type: faq`、`type: report`）。查询时按类型过滤，或者跨类型搜索。这是企业常见的做法——一张表管所有知识，比每个类型一张表简单得多。

Chunking 策略：固定大小最简单，按字数切。语义分块按段落/标题切，保留完整性。递归分块先大后小，兼顾两者。Demo 用固定分块（一个商品一个 Chunk），生产中处理长文档时需要语义分块或递归分块。

核心权衡：Chunk 太大，检索结果里混入无关信息。Chunk 太小，丢失上下文。需要根据实际数据调优，一般 500-1000 token，overlap 10-20%。

注意 content 不含价格和库存。价格会变，向量库里存旧价格会误导检索。实时数据从 MySQL 查。

## Key Emphasis
学员应该理解两个关键点：1) 知识可以从数据库来，不一定从文件来，ETL 管道把结构化数据变成 RAG 可用的文本；2) 多种知识共享一张向量表，用 metadata 区分，比每个类型一张表简单。

## Demo 与生产的差距
Demo 用 JSON 文件模拟 ETL。生产中应该是：MySQL 数据变更 → 触发 ETL 管道 → 提取描述字段 → 生成 Embedding → 更新向量库。Demo 的固定分块在长文档上效果不好，生产中应该用语义分块或递归分块。Chunk Size 需要根据实际数据调优。

## Transition
Chunk 切好了，下一步是：怎么让 AI "理解"这些文本的"意思"？
