## Slide Objective
讲透向量数据库选型和 `v1.3` 中 `pgvector` 的实现（`VectorStoreConfig.java` + `PgVectorStore` + `schema-knowledge.sql`）——知识存在哪、怎么建索引、为什么这样选。

## Suggested Presentation
主流向量存储方案各有优劣。Milvus / Qdrant 是专用向量库，亿级规模性能最强，但要独立部署集群，运维成本高。Elasticsearch 企业常有，但向量是附加功能，资源占用重。

pgvector 不是纯跑分最高的，但最贴合企业现状。PostgreSQL 大部分企业都有，装一个 `vector` 扩展就能做向量存储，**不引入新基础设施**。SQL 生态直接复用，DBA 会管，还能和 JSONB 元数据过滤、关系表事务天然结合。这就是工程选型的智慧——不是追新，是务实。

看右边的 `v1.3` 代码：`VectorStoreConfig.java`。因为项目主业务数据库是 MySQL（存用户画像与实时商品价格库存），pgvector 需要单独的 PostgreSQL 数据源。我们在 `VectorStoreConfig` 里手动创建 `pgvectorDataSource()` 和专属 `JdbcTemplate`，再通过 `PgVectorStore.builder(pgJdbcTemplate, embeddingModel).withDimensions(1024).build()` 构建 `PgVectorStore` Bean。这是**多数据源显式隔离**的设计。

为什么必须独立配置？Spring AI 的 pgvector 自动配置默认会绑定到 Spring Boot 主数据源（`@Primary` MySQL）。如果不做隔离，`PgVectorStore` 会尝试往 MySQL 里执行 PostgreSQL 的 `vector` 语法，启动或写入时直接崩溃。

再看 `docs/schema-knowledge.sql`：`embedding vector(1024)` 定义了 1024 维的向量列，配合 `USING hnsw (embedding vector_cosine_ops)` 建立 HNSW 余弦相似度索引。注意：Embedding 模型输出是 1024 维，表定义也必须是 `vector(1024)`。

索引策略上，Demo 用 **HNSW**（Hierarchical Navigable Small World）。HNSW 图索引精度高、查询快，但比较吃内存，是百万级向量以内的首选；IVFFlat 建索引快、省内存，但精度略低。

## Key Emphasis
选型不是技术竞赛。学员应该学会"根据企业现状做决策"。pgvector 的核心优势是"不引入新基础设施 + SQL/JSONB 生态复用"；在 Spring Boot + MySQL 架构下接入 pgvector，第一件事就是写好 `VectorStoreConfig.java` 做多数据源隔离。

## Demo 与生产的差距（三格）
**✗ HNSW 默认参数，未做大规模向量参数调优与连接池隔离压测**
问题：`v1.3` Demo 仅 26 条商品数据，使用默认 HNSW 索引参数（`m=16, ef_construction=64`），连接池最大连接数设为 5。
不解决会怎样：当企业文档切片增长到数十万至百万级时，默认参数下建索引变慢或召回率下降，高并发检索时连接池排队导致接口 P99 延迟超标。
怎么解决：十万~百万级内调优 HNSW 参数（`m`、`ef_construction` 及查询期 `hnsw.ef_search`），读写分离；千万级以上再评估迁移至 Qdrant / Milvus。取舍：百万级以内 pgvector 性价比最高，无需过早引入独立向量集群。

## Optional Extension
**学员问：数据量到多大 pgvector 扛不住？**
答：先看量级——百万级 Chunk 以内，配好 HNSW 索引和内存的 PostgreSQL 完全扛得住（P95 延迟在毫秒到十几毫秒级）。
Demo 做法：在 psql 中执行 `EXPLAIN SELECT * FROM vector_store ORDER BY embedding <=> '[...]' LIMIT 3;`，亲眼确认 `idx_vs_embedding` HNSW 索引生效。
生产做法：十万~百万级调 `hnsw.ef_search` 平衡精度与延迟；千万级以上再通过 Spring AI 的 `VectorStore` 抽象接口无缝切换底层实现。

## Classroom Interaction
让学员打开 `VectorStoreConfig.java` 和 `docs/schema-knowledge.sql`，找出哪三处数字/配置必须严格对齐（`application.yml` 的模型、`VectorStoreConfig` 的 1024 维、`schema-knowledge.sql` 的 `vector(1024)`）。

## Take Away
选向量库先看企业现状（已有 PG 首选 pgvector）；主库是 MySQL 时必须通过 `VectorStoreConfig` 隔离独立 PG 数据源并构建 `PgVectorStore`。

## Why This Slide Matters
跳过这页，学员在已有 MySQL 的 Spring Boot 项目里直接引入 `spring-ai-pgvector-store-spring-boot-starter` 会因数据源冲突直接启动崩溃。

## Transition
向量库和 `PgVectorStore` 配置好了，数据也写入了。当用户问"推荐一台写代码的笔记本"时，`RetrievalService` 怎么检索出最相关的商品？（page_11）
