# Page 13 — 存储选型 — 什么时候用什么

## Slide Objective
讲清楚三种存储（Redis / MySQL / Vector DB）的适用场景与选型边界。

## Suggested Presentation
Redis、MySQL、Vector DB——三种存储解决三种不同维度的问题：

1. **Redis 存临时的（Session 短期记忆与累积摘要）**：因为当前对话历史需要毫秒级高频读写、自动过期（24h TTL）。Redis 的 List 结构天然适合消息队列。验证：`redis-cli LRANGE session:xxx:messages 0 -1`。
2. **MySQL 存持久的（用户画像结论层）**：用户画像需要跨 Session 永久生效、需要结构化字段级更新与精确主键查询。建表脚本在 `docs/schema.sql`，由 MyBatis Plus 操作。
3. **Vector DB 存语义检索的（企业知识库 & 下一课演进的过程记忆）**：企业文档与海量历史片段需要按语义相似度召回，将在下一课（第六课）正式引入 pgvector。

记住底线原则：**不要把长期画像存 Redis（易失），不要把高频会话缓冲存 MySQL（读写慢、无自动滑窗过期）。**

## Key Emphasis
三种存储对应三种读写模式，一句话锚定：Redis 存临时的、MySQL 存持久结构化的、Vector DB 存语义检索的。下一页我们将在此基础上进一步预演：当下一课引入 Vector DB 后，长期记忆内部如何分化为"结论层（MySQL）"与"过程层（pgvector）"。

## Optional Extension
**学员问：一段"半结构化"的用户数据（既有明确偏好标签，又有长段聊天背景）该放哪？**
答：拆成"结论"与"过程"分层存放，别硬塞进一张表。
Demo 做法：本课 Demo（`v1.2.1`）先把结构化结论（`facts / preferences / tasks / constraints`）抽进 MySQL `user_profile` 表；从下一课（`v1.3`）开始，再把自然语言历史过程沉淀进 pgvector。
生产做法：判断标准就两条——要不要按字段精确读写/合并更新？要不要按问题做语义检索？前者进关系库，后者进向量库。

## Classroom Interaction
给五条数据让学员现场贴到三个存储上：当前第 3 轮对话原文（Redis）、用户预算与职业画像（MySQL）、退换货政策文档（Vector DB）、商品实时库存价格（MySQL）、商品长篇评测描述（Vector DB）。当场对答案。

## Take Away
存储选型按"读写模式 + 生命周期"决定，而不是按数据量大小；拿不准时先问一句"它是要按主键全量读改，还是要按语义相似度检索"。

## Why This Slide Matters
把所有用户数据混在一个存储里的人，要么在关系库里做不出语义检索，要么在向量库里做不了画像字段更新——选型一步错，后面全是返工。

## Transition
存储选型的大原则讲完了。既然下一课（第六课）就要引入 pgvector 向量库，很多同学会问：到时候能不能干脆把 MySQL 画像也搬进 pgvector，只维护一个库？下一页（Page 14）我们通过"架构演进预告"，讲透为什么记忆必须分为结论层（MySQL）和过程层（pgvector）。
