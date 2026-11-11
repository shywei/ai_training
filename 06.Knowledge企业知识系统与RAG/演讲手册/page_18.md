## Slide Objective
把本课学的 Advanced RAG 放进 2026 RAG 范式演进全景图中（Naive → Advanced → Adaptive → Self/Corrective → GraphRAG / RAPTOR / Multimodal / Agentic RAG / Deep Research），并明确 `v1.3` 代码中知识检索（`RetrievalService`）与记忆分层（`UserMemoryService` / `UserMemoryMapper` + MySQL `UserProfileMapper`）在 `agent-runtime` 单体内的协同关系。

## Suggested Presentation
先把坐标系立住：**本课的核心主干是 Advanced RAG**。前面讲的高阶分块（Parent-Child / Contextual）、Embedding、pgvector、Hybrid Search、Rerank、五步流水线（page_15）和三元组评测（page_17），全部是企业落地 Advanced RAG 的基石。

看上方的四阶演进链：
1. **① Naive RAG（检索即答）**：单路向量 Top-K 召回直接拼进 Prompt。简单 FAQ 能跑，但遇到专有名词、多轮指代或排序噪声就翻车。
2. **② Advanced RAG（检索工程化 —— ★本课主干）**：围绕检索前后做精细化工程——Pre-Retrieval（Query Rewrite）、Retrieval（Hybrid 稠密+稀疏双路）、Post-Retrieval（Cross-Encoder Rerank + `<retrieved_context>` 安全装配）。
3. **③ Adaptive RAG（先判再查）**：不是所有问题都走同一条检索链。先对 Query 做路由分类（闲聊直接答 / 简单问题单跳查 / 复杂问题多跳或调工具）。本课 `v1.3` 用 `RetrievalService.isProductRelated()` 演示了最轻量的门控，下一课（第七课）将升级为快模型路由。
4. **④ Self / Corrective RAG（质量门 + 自纠）**：检索回来先让轻量裁判评估"召回的内容能不能回答这个问题"——不合格就自动改写 Query 重查，再不合格就诚实拒答，绝不强行编造。

再看下方的五个前沿范式——按业务痛点选型，不必全上：
- **GraphRAG**：抽实体-关系建图 + 社区摘要，专治跨文档多跳与全局归纳问题（代价是建图Token高）。
- **RAPTOR**：递归聚类摘要成树，专治百页长文档里"既要局部细节又要跨章总结"。
- **Multimodal RAG**：结合 Docling/MinerU 表格提取与多模态大模型（VLM），让图文表统一检索。
- **Agentic RAG（★ 2026 最大主线）**：把检索变成 Agent 的自主工具——LLM 自主执行 **Plan → Search → Read → Reason** 多轮循环，一次没查全自己换词再查（第七、八课学完 Tool Calling 与 ReAct 循环后自然水到渠成）。
- **Deep Research**：多源多轮自主检索 + 交叉验证 + 输出结构化长篇研究报告。

最后看底部橙色说明栏，提前连接第六章的 `v1.3` 代码与记忆升级：
在本课 `v1.3`（`agent-runtime` 单体）中，PostgreSQL + pgvector 同时支撑了两条核心链路：
1. **企业知识检索**：`KnowledgeController` + `KnowledgeService` + `RetrievalService`（基于 `VectorStoreConfig` 的 `PgVectorStore`，操作 `vector_store` 表）；
2. **用户语义记忆**：`UserMemoryService.java` + `UserMemoryMapper.java` + `UserMemory.java`（操作 pgvector 的 `user_memory` 表），与 MySQL 的结构化画像（`UserProfileMapper` 操作 `user_profile` 表）及 Redis 短期会话（`SessionMemory`）一起，在 `MemoryManager.buildContext()` 中完成四路融合！（到了下一课第七课 `v4.0`，我们才会把知识与记忆进一步拆分为独立的 MCP Server 进程。）

## Key Emphasis
1) 先把 Advanced RAG 五步流水线 + 三元组评测做扎实，再按问题形态演进到 Adaptive / Self-RAG / GraphRAG / Agentic RAG；2) 认清 `v1.3` 的代码架构边界：`agent-runtime` 单体内由 `RetrievalService` 管知识、由 `UserMemoryService` / `UserMemoryMapper`（pgvector）+ `UserProfileMapper`（MySQL）管分层记忆。

## Optional Extension
**学员问：我们公司文档不多，要不要直接上 GraphRAG 或 Agentic RAG？**
答：千万别盲目追新！80% 的企业知识问答痛点，靠"好解析（Docling/MinerU）+ 好分块（Parent-Child/Contextual）+ Hybrid Search + Rerank"这套 Advanced RAG 就能解决，延迟低、成本可控。只有当评测集证明剩余 20% 是跨文档全局问题或多步搜集问题时，再局部引入 GraphRAG 或 Agentic RAG。

## Classroom Interaction
给出四个真实业务问题，让学员选范式：
1. `"ThinkPad X1 Carbon 支持雷电 4 吗？"`（Advanced RAG 单跳）；
2. `"汇总今年所有客诉工单里最集中的三类质量问题"`（GraphRAG / RAPTOR 全局归纳）；
3. `"对比我们公司三款轻薄本与竞品的优劣，写一份选购指南"`（Agentic RAG / Deep Research 多步检索）。

## Take Away
Advanced RAG 是立身之本，Agentic RAG 是 2026 演进主线；在 `v1.3` 中，`RetrievalService` 与 `UserMemoryService` 在 `agent-runtime` 内共同基于 pgvector 支撑起"懂商品 + 懂用户"的双翼。

## Why This Slide Matters
跳过这页，学员容易被漫天飞的前沿名词搞晕，既不知道本课所学在技术演进中的坐标，也不清楚何时该停在 Advanced RAG、何时该往 Agentic RAG 升级。

## Transition
理论体系与前沿坐标全部讲完了！接下来进入第六章：切回我们的 `v1.3`（v3.0）Demo，看这套架构在代码里做成了什么样、怎么一步步运行验证。（page_19）
