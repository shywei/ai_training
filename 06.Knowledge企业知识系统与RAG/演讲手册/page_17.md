## Slide Objective
明确告诉学员 Demo 做到了什么、还差什么，以及代码怎么读。学员需要知道"Demo 是骨架，不是成品"。

## Suggested Presentation
左边是 Demo vs 生产差距。Demo 做到了七件事：JSON 加载、固定分块、pgvector 存储、关键词过滤、语义检索、context 注入、数据分层、自动沉淀。这些都是 RAG 的核心链路，学员可以动手验证。

生产还要补九件事。每个都是具体的工程问题：

PDF/Word/Excel 解析——扫描件要 OCR，表格要结构化提取，多栏排版要识别布局。语义分块——按段落/标题切分，保留语义完整性。Hybrid Search——向量 + BM25 融合，解决型号/SKU 匹配问题。Rerank——交叉编码器精排，提高排序精度。Embedding 模型选型与微调——不同领域的文档可能需要不同的模型。RAG Evaluation——Recall/Precision/Answer Relevancy，量化调优。语义缓存——相似问题共用结果，降低延迟和成本。多部门知识隔离——不同部门看到不同的知识。文档更新与版本管理——增量索引，不是每次全量重建。

右边是项目结构和读代码的顺序建议。这是学员课后自己读代码的路线图。

第一步：先看 `KnowledgeController.java`。这是知识库的入口。`POST /api/knowledge/index` 触发索引，`GET /api/knowledge/status` 查看状态。看 `loadProductDocuments()` 方法——它从 `products.json` 读取商品数据，构建 Document 对象。注意 content 不含价格/库存，只放描述性信息。这是"数据分层"的代码体现。

第二步：看 `RetrievalService.java`。这是检索的核心。看 `retrieve()` 方法——先调 `isProductRelated()` 做关键词过滤，再调 `vectorStore.similaritySearch()` 做向量搜索，最后 `formatResults()` 格式化结果。这个方法是 RAG 的"R"（Retrieval）的完整实现。

第三步：看 `MemoryManager.java` 的 `buildContext()` 方法。这是 context 构建的核心。v3.0 改动最大：四部分同时注入——MySQL 画像、pgvector 记忆、Redis 对话、pgvector 知识。看 `checkAndPrecipitate()` 方法——自动沉淀机制。

第四步：看 `UserMemoryService.java`。理解用户记忆的存储和检索。`store()` 把文本 Embedding 后存入 pgvector，`search()` 按语义相似度检索。

日志是调试的核心工具。`logs/llm-chat.log` 看 `[Memory Injection]` 部分，应该包含"用户画像"、"相关记忆"、"参考知识"三个标题。这是验证 RAG 有没有生效的最直接方式。

## Key Emphasis
"怎么读 Demo 代码"比"Demo 有什么文件"更重要。给学员一个明确的阅读路线图，让他们能自己读懂代码。

## Transition
接下来是本课对第五课的延伸——记忆体系升级。
