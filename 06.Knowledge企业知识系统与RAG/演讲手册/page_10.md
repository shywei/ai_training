## Slide Objective
讲透向量数据库选型和 pgvector 的实现——知识存在哪、怎么建索引、为什么这样选。

## Suggested Presentation
四个方案各有优劣。Chroma 最简单，pip install 就能用，但企业不用它。Milvus 性能最强，但要独立部署，运维成本高。ES 企业已有，但向量只是它的附加功能，资源重。

pgvector 不是技术最先进的，但最贴合企业现状。PostgreSQL 大部分企业都有，加一个扩展就能做向量存储，不引入新基础设施。SQL 生态复用，DBA 会管。这就是工程选型的智慧——不是追新，是务实。

看右边的代码。`VectorStoreConfig.java`：因为项目主数据库是 MySQL，pgvector 需要单独的 PostgreSQL 数据源。手动创建 `DataSource` + `JdbcTemplate`，注入 `PgVectorStore`。这是"数据源隔离"的设计——知识库和业务数据分开存，DBA 各管各的。

为什么需要独立数据源？Spring AI 的 pgvector 自动配置使用主数据源，但我们的主数据源是 MySQL。如果不做隔离，pgvector 会尝试往 MySQL 写向量数据，直接报错。手动创建 PostgreSQL 数据源，注入 `PgVectorStore`，这是踩过的坑。

再看 `schema-knowledge.sql`：`embedding vector(1024)` 定义了 1024 维的向量列。这里有个坑——Embedding 维度必须和表定义匹配。BGE-large-zh-v1.5 输出 1024 维，所以表定义是 `vector(1024)`。如果换模型（比如 ada-002 是 1536 维），需要 DROP TABLE 重建。维度写错了不报错，查不出来——这是最隐蔽的坑。

索引策略：Demo 用 HNSW（`USING hnsw`）。HNSW 精度高但吃内存，适合中小规模。IVFFlat 速度快但精度低，适合大规模。Demo 数据量小（26 个商品），HNSW 够了。生产数据量大时再考虑 IVFFlat 或混合方案。

## Key Emphasis
选型不是技术竞赛。学员应该学会"根据企业现状做决策"，而不是"哪个最新用哪个"。pgvector 的优势是"不引入新基础设施"——这句话在企业里很有说服力。

## Demo 与生产的差距
Demo 没有对比不同 Embedding 模型的效果。生产中应该用评测集测试 BGE vs Jina vs ada-002 的 Recall/Precision，选最优的。Demo 也没有做 Embedding 模型微调——领域专有名词（如"X1 Carbon"）可能需要微调才能正确理解。Demo 的 HNSW 索引在数据量大时可能不够，需要评估 IVFFlat 或混合方案。

## Transition
向量库有了，数据也进去了。用户问"推荐一台写代码的笔记本"，系统怎么找到最相关的商品？
