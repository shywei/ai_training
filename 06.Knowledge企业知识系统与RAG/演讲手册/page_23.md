## Slide Objective
作为第六课真正的收官总结页（`SUMMARY`），提炼全课核心金句，展示 Agent 五大能力构建进度图与第六课完整能力地图，并布置基于 `v1.3` 代码的可执行课后实战练习。

## Suggested Presentation
今天的一句话带走：**Knowledge 不是原始文档，而是经过治理、分块、向量化与安全隔离后能被 AI 可靠使用的企业知识。**

这句话的核心是"工程治理"。无论是数据库里的商品条目（Catalog RAG），还是几十页的 PDF 保修手册（Document RAG），只有经过解析（MinerU / Docling）、高阶分块（Parent-Child / Contextual Retrieval）、向量化（BGE-M3 / Qwen3-Embedding / text-embedding-3-large）、检索精排（pgvector + Hybrid + Rerank）、安全注入（`<retrieved_context>` 防间接注入）与三元组量化评测（Recall / Precision / Faithfulness），才能真正变成企业级 AI Agent 的可靠知识库。

看左边的 **Agent 构建进度图**：
导购 Agent 的五大核心能力，今天我们点亮了第三颗星——**Knowledge**。
- 第四课（Prompt）让它学会了怎么跟人规范对话；
- 第五课（Memory）让它记住了用户的偏好与历史；
- 第六课（Knowledge）让它掌握了公司的商品知识库，并顺手用 pgvector（`UserMemoryService`）把长期记忆也升级成了语义记忆！

看底部的阶段成果与下一课预告：
现在的导购 Agent 已经是一个"懂你、又懂商品"的专业顾问了。但如果用户问："ThinkPad X1 Carbon 现在还有几台现货？帮我下一单"，它还做不到——因为实时库存和下单交易不能放进静态知识库，必须实时调用企业 ERP/订单系统。它现在只能"说"，不能"做"。
**下一课（第七课）：Tool Calling 与 MCP 协议——我们将把知识库与商品交易能力封装为标准 MCP Server，让 Agent 真正连接企业系统，从"能说"迈向"能做"！**

### 课后实战练习（基于 Git 标签 `v1.3`）
1. **跑通索引管道**：启动 `agent-runtime`，执行 `POST http://localhost:8080/api/knowledge/index`（`KnowledgeController`），观察 `KnowledgeService` 将 26 个商品向量化写入 `vector_store` 表的完整日志。
2. **验证四路上下文注入**：提问`"推荐一台适合出差写代码的笔记本"`，在 `logs/llm-chat.log` 中检查 `MemoryManager.buildContext()` 注入的 `## 用户画像`、`## 相关记忆`、近期对话与 `## 参考知识`。
3. **验证检索门控与阈值旋钮**：分别测试闲聊（`"你好"`）与调高 `RetrievalService` 中的 `withSimilarityThreshold`，观察检索过滤效果。
4. **验证语义记忆跨会话召回**：输入个人剪辑需求触发 `checkAndPrecipitate()`，确认 `UserMemoryService` 将语义记忆写入 `user_memory` 表并在新会话中成功召回。
5. **走读核心源码**：按 `VectorStoreConfig` → `KnowledgeController` / `KnowledgeService` → `RetrievalService` → `UserMemoryService` → `MemoryManager` 顺序精读一遍 `v1.3` 源码。

## Key Emphasis
进度图让学员清楚自己在整套课程中的位置；能力地图把今天学的从"两大 RAG 形态"到"五步流水线 + 三元组评测"串成体系；五个课后练习全部严格对应 `v1.3` 源码。

## Optional Extension
**学员问：课后如果想自己动手给 `v1.3` 加一个生产级特性，最推荐先练哪个？**
答：推荐两个小步快跑的练习：
1. 在 `PromptManager` 和 `RetrievalService.formatResults()` 中给检索出的商品块加上 `<retrieved_context>` XML 隔离标签与防间接注入声明（10 分钟搞定）；
2. 写一个 JUnit 测试类，准备 10 条测试问题调用 `retrievalService.retrieve(query)`，算出你自己的第一个 `Recall@3` 分数！

## Classroom Interaction
请每位学员用一句话总结今天收获最大的一个工程认知（比如"静态描述放 pgvector、实时价格放 MySQL"、"长文档用 Parent-Child + Contextual 分块"、或"用 `<retrieved_context>` 防间接注入"）。

## Take Away
企业级 RAG = 高质量解析分块（Docling + Parent-Child/Contextual）+ 混合检索精排（pgvector + BM25 + Rerank）+ 安全受控装配（`<retrieved_context>`）+ 黄金集持续评测（Recall/Precision/Faithfulness）。

## Why This Slide Matters
收官页将全课 23 页知识点收敛为一张可带走、可落地的架构蓝图，并为第七课引入 Tool Calling 与 MCP 埋下自然伏笔。

## Transition
第六课到此圆满结束！休息一下，下一课我们进入《第七课：Tool Calling 连接企业系统》，带大家亲手用 Function Calling 与 MCP 协议让 Agent 真正"动手干活"！

## 课后扩展学习导读（深水区进阶指引）
对于希望在知识检索与 RAG 领域进一步深潜的学员，本课根目录下特别沉淀了一份高质量的进阶资料：[扩展学习指南.md](../扩展学习指南.md)。
- **核心定位**：第六课是第二阶段算法与数据密集度最高的攻坚课。课堂上大家掌握了 `v1.3` 的工程基线，而《扩展学习指南.md》遵循 3W 精选导读法则，专为解决千万级向量规模下的真实生产痛点而编写。
- **四大核心主题导读**：
  1. **HNSW 底层原理与 pgvector 调优**：从 Malkov 奠基论文（arXiv:1603.09320）与跳表图解出发，深入 `m` / `ef_construction` / `ef_search` 参数权衡，并详解 pgvector 现代版本的 Iterative Scan 机制，彻底解决带 `WHERE` 条件过滤时的“漏召回崩溃”。
  2. **混合检索与两阶段精排架构**：拆解 BM25 + Dense Vector 的 RRF（倒数排名融合）公式原理，对比 Bi-Encoder、Cross-Encoder（BGE-Reranker）与 ColBERT Late Interaction 的精度与延迟，建立生产级两阶段流水线。
  3. **高阶 RAG 范式演进**：对比 Self-RAG（arXiv:2310.11511）、CRAG（arXiv:2401.15884）与微软 GraphRAG（arXiv:2404.16130）的架构差异与 Token 成本，明确何时该用向量点对点查找、何时该用图谱全局汇总。
  4. **RAG 自动化量化评测**：详解 Ragas 论文（arXiv:2309.15217）四大核心指标（Context Precision, Context Recall, Faithfulness, Response Relevancy）与 NVIDIA RAG Triad 的工程映射，掌握如何用 Spring AI 手搓 LLM-as-a-Judge 自动化 CI/CD 质量门禁。
- **自测检验**：指南末尾提供了 3 道大厂架构级思考题（含高维向量内存估算、专有名词召回失效排查与医疗金融级防幻觉闭环），请学员课后精读指南并尝试独立思考作答。
