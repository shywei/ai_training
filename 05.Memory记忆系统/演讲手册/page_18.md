# Page 18 — 业界记忆系统一览 — 本课三层记忆的坐标系

## Slide Objective
给学员建立 2026 年业界主流记忆系统的全景坐标系：左边剖析四大代表性方案（**MemGPT/Letta、Mem0、Zep/Graphiti、A-MEM**），右边给出学界 **forms / functions / dynamics** 三分法以及 **2026 记忆工程两大核心算子（记忆冲突消解四态机 `ADD/UPDATE/DELETE/NOOP` + 三维记忆检索打分公式）**，并与我们课程的三层记忆（`Redis 短期缓冲 + MySQL user_profile 本课 v2.0 结论层 + pgvector user_memory 下节课 v3.0 过程层`）一一对齐。

## Suggested Presentation
前面讲完了我们自己的三层记忆架构与 Demo 实现。这一页我们抬眼看业界：2026 年开源界与学术界最火的记忆系统都在做什么？我们的设计与它们是什么关系？

**看左边：四大业界代表性记忆系统**
1. **MemGPT / Letta —— 把 LLM 当操作系统，上下文当内存**：
   核心思路是**虚拟内存分页（Paging）**——Context Window 是有限的"核心内存（Core Memory）"，装不下的历史放"外部存储（Archival/Recall Storage）"，由 Agent 通过函数调用换入换出。
   **对照本课**：它的"核心内存"就对应我们本课 v2.0 全量注入 Prompt 的 `MySQL user_profile`；它的"外部存储按需换入"就对应我们下节课 v3.0 引入的 `pgvector user_memory` 语义检索。
2. **Mem0 —— 生产级长期记忆抽取与冲突消解流水线**：
   社区落地最广的长期记忆组件。核心是**抽取-比对-更新流水线**。
   **对照本课**：我们本课 v2.0 的 `UserProfileExtractor`（读旧画像 + 新对话 → 推理模型合并更新）正是 Mem0 合并思想在用户画像层的最小精简实现。
3. **Zep（Graphiti 引擎）—— 时序知识图谱（Temporal Knowledge Graph）**：
   专门解决"用户偏好随时间变化、旧事实何时失效"的时序一致性问题——每条事实边上都带有生效时间与失效时间（双时态建模）。
   **对照本课**：我们用 `user_profile` 的"新信息覆盖旧信息 + `updated_at`"配合下节课 `user_memory` 的"只增不改"，实现了轻量化时序语义。
4. **A-MEM —— Agentic 记忆自演化（学界前沿）**：
   受卢曼卡片盒（Zettelkasten）启发，新记忆写入时自动与旧记忆建立双向链接并重写历史摘要，让记忆网络自我演化。生产上目前仍以可控性优先，演化方向留作高阶探索。

**看右边：学界三分法 + 2026 记忆工程两大核心算子**
1. **学界 `forms / functions / dynamics` 三分法**：
   评价任何记忆系统先问三句话：**forms（形态：存在参数、上下文还是外部库？）**、**functions（功能：短期工作缓冲还是长期沉淀？）**、**dynamics（动态：怎么写入、检索、更新、遗忘？）**。
   用三问定位我们课程的三层记忆：
   - `Redis (短期缓冲)` = 上下文内 / 短期工作（暂存、满 8 条累积摘要、24h TTL 淘汰）；
   - `MySQL user_profile (本课 v2.0 结论层)` = 外部长期存储（推理模型合并更新、可更正）；
   - `pgvector user_memory (下节课 v3.0 过程层)` = 外部长期存储（按需语义检索）。
2. **2026 记忆工程两大核心算子（面试与架构设计高频考点）**：
   - **① 记忆冲突消解四态机（Mem0 范式：`ADD / UPDATE / DELETE / NOOP`）**：
     当新对话抽取出候选记忆（如"预算从 5000 提高到 15000"或"不再考虑游戏本"）时，不能无脑 `INSERT` 到库里（否则下次检索会同时召回"预算 5000"和"预算 15000"，导致大模型精神分裂）。工程标准做法是将候选记忆与已召回的旧记忆一起送入 LLM 判定状态机：新增事实走 `ADD`、偏好变更走 `UPDATE`（原地更新或标记旧条目失效）、显式否定走 `DELETE`、重复废话走 `NOOP`。
   - **② 三维记忆检索打分公式**：
     单纯按向量相似度（Relevance）检索记忆，容易把三年前的过期偏好排在昨天刚说的话前面。生产级记忆检索采用三维加权打分（源自 Generative Agents 并成为 2026 工业界标配）：
     `Score = α · Relevance(语义相关度) + β · Recency(时间衰减，如指数衰减) + γ · Importance(重要度权重)`
     让"语义相关 + 最近发生 + 重要度高"的记忆优先进入 Context Window！

## Key Emphasis
讲清四个系统的核心范式（Letta 分页 / Mem0 抽取更新 / Zep 时序图谱 / A-MEM 卡片盒演化）+ **两大工程算子（`ADD/UPDATE/DELETE/NOOP` 冲突消解四态机 + `Relevance + Recency + Importance` 三维打分公式）**，并明确我们课程的三层定位（`Redis 短期缓冲 + MySQL 本课 v2.0 结论层 + pgvector 下节课 v3.0 过程层`）。

## Optional Extension
**学员问：既然有 Mem0、Zep 这些开源项目，企业该自建三层还是直接引入现成框架？**
答：先懂原理与三层边界，再做选型。
Demo 做法：课程手写 `SessionMemory + SummaryPipeline + UserProfileExtractor + MemoryManager`，每个 Prompt 和 SQL 都透明可调，让你把"累积压缩、合并消解、分层注入"吃透。
生产做法：如果业务偏好简单、合规要求高（需严格掌控级联删除与脱敏），自建三层最可控；如果需要细粒度自由文本记忆与图谱时序推理，可以复用 Mem0 的冲突消解四态机或接入 Zep。

## Classroom Interaction
提问学员："用户上周说'我买电脑预算 5000 元'，今天说'我年终奖发了，预算提到 15000 元'。如果放到 Mem0 的四态机里，今天这句话应该触发 `ADD / UPDATE / DELETE / NOOP` 中的哪一个动作？如果不做冲突消解直接 `ADD` 会有什么后果？"（答案：触发 `UPDATE`；如果直接 `ADD`，检索时会同时召回 5000 和 15000 两条矛盾记忆）。

## Take Away
掌握 `forms / functions / dynamics` 评价框架与两大核心算子（`ADD/UPDATE/DELETE/NOOP` 冲突消解 + `Relevance/Recency/Importance` 三维打分），无论自建还是选型业界框架都能一眼看穿本质。

## Why This Slide Matters
没有业界视野的工程师容易闭门造车（比如只做纯向量相似度检索而忽略时间衰减 Recency 和冲突消解 UPDATE），有了这张坐标图，就能站在 2026 工业界最佳实践的肩膀上迭代自己的记忆系统。

## Transition
业界坐标系和核心算子都装进脑子里了。下面进入最后一页（Page 19：第五课总结），为我们今天的 Memory 记忆系统收官。
