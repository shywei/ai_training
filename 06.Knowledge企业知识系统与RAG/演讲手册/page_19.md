## Slide Objective
给学员一个 v3.0（Git 标签 `v1.3`）Demo 的全景视图——相比第五课 v2.0 新增了什么、端到端数据流怎么在 `agent-runtime` 单体里串起来、以及四个关键架构设计决策背后的思考。

## Suggested Presentation
前面我们讲完了 RAG 的完整理论体系与优化进阶，现在切回代码，看看这些设计在第六课 `v1.3` Demo 里是怎么落地的。

看左上的对比：第五课（v2.0）的导购 Agent 能聊天、能用 MySQL 记用户画像、用 Redis 存短期对话，但它不知道公司卖什么商品。第六课（v3.0 / Git tag `v1.3`）新增了四大核心能力：
1. **商品知识检索**（`KnowledgeController` + `KnowledgeService` + `RetrievalService`，基于 `PgVectorStore`）；
2. **用户语义记忆检索**（`UserMemoryService` + `UserMemoryMapper`，操作 pgvector `user_memory` 表）；
3. **短期会话向长期向量库自动沉淀**（`MemoryManager.checkAndPrecipitate()`）；
4. **四路 Context 组装**（MySQL 画像 + pgvector 语义记忆 + Redis 对话历史 + pgvector 商品知识）。

看左下的端到端数据流（全部运行在 `agent-runtime` 单体进程内）：
当用户提问"推荐一台写代码的笔记本"时，`MemoryManager.buildContext()` 依次拉取：① MySQL `UserProfileMapper` 的用户画像；② pgvector `UserMemoryService.search()` 的相关历史记忆片段；③ Redis `SessionMemory` 的当前对话缓冲；④ pgvector `RetrievalService.retrieve()` 检索出的 Top-K 商品知识。四块拼好后交给 `PromptManager`，带着知识使用规则发给大模型生成推荐回复。

看右边的四个关键设计决策：
1. **数据分层**：为什么不把商品价格和库存也塞进向量库？因为价格天天变、库存秒秒变，向量库里存旧价格会直接误导用户！所以我们严格分层：不变的商品描述进 pgvector 供 RAG"理解"，实时价格库存留 MySQL 供第七课 Tool"查询"。
2. **手动索引触发（`POST /api/knowledge/index`）**：为什么不在 Spring Boot 启动时无脑全量重算 Embedding？因为重启一次就调 26 次 Embedding API 既烧钱又拖慢启动，而且学员看不见过程。做成显式 API 接口（`KnowledgeController.index()`），调用一次、控制台逐条打印 Embedding 进度，数据管道一目了然。
3. **自动沉淀机制**：Redis 只做短期工作缓冲，对话满阈值后自动提要并经 `UserMemoryService` 写入 pgvector `user_memory` 表。
4. **结论与过程记忆不重叠**：MySQL `user_profile` 存结构化结论标签，pgvector `user_memory` 存非结构化对话语义片段，各司其职。

## Key Emphasis
这页是从理论到 `v1.3` 代码实践的桥梁。重点掌握端到端四路数据流（`UserProfileMapper` + `UserMemoryService` + `SessionMemory` + `RetrievalService`）以及"数据分层"和"显式索引接口 `POST /api/knowledge/index`"的设计意图。

## Optional Extension
**学员问：生产环境里商品上新了，也是靠人工调 `POST /api/knowledge/index` 吗？**
答：教学期用手动 API 是为了让流水线透明可观测；生产环境里，这个接口背后的方法（`KnowledgeService`）会绑定到商品管理后台的"上架/变更事件"（MQ 或 MySQL CDC），按商品 `doc_id` 做增量 Upsert。

## Classroom Interaction
在终端现场执行一次 `curl -X POST http://localhost:8080/api/knowledge/index`，让全班看一眼 Spring Boot 控制台逐条打印 26 个商品 Embedding 的实时日志。

## Take Away
`v1.3` 在 `agent-runtime` 内用同一个 PostgreSQL + pgvector 同时打通了"商品知识检索（`RetrievalService`）"和"用户语义记忆（`UserMemoryService`）"，并与 MySQL、Redis 形成清晰的数据分层。

## Why This Slide Matters
跳过这页，学员进代码仓库后容易迷失在各个类之间，看不清 `MemoryManager` 是如何把四路数据串成闭环的。

## Transition
清楚了 `v1.3` Demo 做成了什么，接下来我们坦诚对比：Demo 骨架和真正的企业生产系统还有哪六项差距？`v1.3` 的代码目录应该按什么顺序读？（page_20）
