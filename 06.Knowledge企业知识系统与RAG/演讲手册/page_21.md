## Slide Objective
展示引入向量数据库后，第五课的记忆系统如何在 `v1.3` 中通过 `UserMemoryService.java`、`UserMemoryMapper.java` 与 `UserMemory.java` 升级为 **MySQL 结构化画像 + pgvector 非结构化语义记忆** 的双层长期记忆架构。

## Suggested Presentation
这一页把第五课（Memory）和第六课（Knowledge/pgvector）彻底融会贯通。

回顾第五课（v2.0）的局限：当时的长期记忆只有 MySQL 的 `user_profile` 表——存的是高度浓缩的结构化标签（比如 `preferences: "轻薄本"`）。但用户在长对话里随口提到的具体语境（比如"记住我平时还做 4K 视频剪辑，希望显卡性能强一些，最好接口齐全"），很难塞进固定的几个标签字段里，大量宝贵的语义细节就这样丢失了。

到了第六课（`v1.3`），既然我们已经通过 `VectorStoreConfig.java` 引入了 PostgreSQL + pgvector，记忆系统立刻就能借力升级！
看右边的 `v1.3` 核心代码：`UserMemoryService.java` 与 `UserMemoryMapper.java`（位于 `agent-runtime` 的 `memory/` 包下）：
- **沉淀写入（`saveMemory`）**：当 Redis 中的短期会话达到沉淀阈值时，`MemoryManager.checkAndPrecipitate()` 会将对话片段交由 `UserMemoryService`，通过 `embeddingModel.embed(content)` 转为 1024 维向量，经 `UserMemoryMapper` 写入 pgvector 的 `user_memory` 表。
- **语义召回（`search`）**：当用户在新会话中提问"给我推荐几款电脑"时，`UserMemoryService.search(userId, query)` 按 `userId` 过滤并执行余弦相似度搜索，把历史对话中相关的语义记忆片段（"平时做视频剪辑，希望显卡强"）精准召回，拼入 `## 相关记忆`！

看右下角的数据分层全景——同一个 Agent 系统，三大存储引擎各司其职：
- **pgvector（语义检索层）**：一张表存事（`vector_store` 商品描述，由 `RetrievalService` 检索），一张表存人（`user_memory` 用户语义记忆，由 `UserMemoryService` 检索）。
- **MySQL（精确查询层）**：一张表存实时商品价格库存（`product_info`，第七课 Tool 查询），一张表存用户结构化画像结论（`user_profile`，由 `UserProfileMapper` 查询）。
- **Redis（高速缓冲层）**：`SessionMemory` 存当前会话近期对话。

（注：在本课 `v1.3` 中，`UserMemoryService` 和 `UserMemoryMapper` 直接在 `agent-runtime` 内访问数据库；到了第七课 `v4.0`，我们会把它们抽取到独立的 MCP Server 后面，供多个 Agent 共享。）

## Key Emphasis
向量数据库不仅服务于企业知识（Knowledge），也赋能了用户记忆（Memory）。记住 `v1.3` 记忆三层架构：MySQL `user_profile` 存结论标签、pgvector `user_memory`（`UserMemoryService`）存语义过程片段、Redis `SessionMemory` 做短期缓冲。

## Demo 与生产的差距（三格）
**✗ 记忆沉淀未做去重冲突消解，且在请求线程内触发**
问题：`v1.3` 的 `MemoryManager.checkAndPrecipitate()` 在对话过程中触发，且多次沉淀相似表述（或前后矛盾的表述，如预算从 5000 变 10000）时只追加不合并。
不解决会怎样：触发沉淀的那一轮响应变慢；时间久了 `user_memory` 表里堆积重复或过期的记忆片段，干扰 LLM 判断。
怎么解决：沉淀全异步化（MQ / 线程池）；写入 `user_memory` 前先检索相似旧记忆，走 `ADD / UPDATE / DELETE / NOOP` 冲突消解状态机，并在检索打分时引入时间衰减（Recency Decay）。

## Optional Extension
**学员问：既然 `vector_store` 和 `user_memory` 都在 pgvector 里，为什么不干脆合成一张表？**
答：千万不能混表！用户记忆属于个人隐私数据（必须按 `user_id` 严格隔离，且受 GDPR/个人信息保护法的"被遗忘权/一键注销删除"约束）；而企业商品知识是公共或按部门隔离的企业资产，两者的生命周期、权限模型和评测方式完全不同。

## Classroom Interaction
让学员执行演示步骤 4：对 Agent 说`"记住我平时还做视频剪辑，希望显卡性能强一些"`，清空 Redis 会话并重新登录后问`"给我推荐几款电脑"`，在日志里找出 `UserMemoryService` 从 `user_memory` 表召回的记录。

## Take Away
同一个 pgvector，分表支撑起 Knowledge（`vector_store` + `RetrievalService`）与 Memory（`user_memory` + `UserMemoryService`）；结论归 MySQL、语义归 pgvector、缓冲归 Redis。

## Why This Slide Matters
跳过这页，学员不知道如何保存非结构化的历史对话细节，或者反过来把用户隐私记忆和公共商品知识混进同一张向量表。

## Transition
学到这里，企业决策者常会提出一个灵魂拷问："现在大模型上下文窗口都到 100 万 Token 了，还需要做 RAG 吗？"而且我们在 `v1.3` Demo 里到底能亲手验证哪些核心机制？我们翻到倒数第二页。（page_22）
