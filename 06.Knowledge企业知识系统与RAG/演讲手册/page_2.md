## Slide Objective
展示课程全貌（8 个模块，覆盖 P3–P23），让学员明确今天的学习路径、每个知识模块对应 Demo 的落地点，以及核心技术架构的重构升级点（企业多元数据接入矩阵、P0 级纠正混合检索定义、完整 10 步 RAG Pipeline 与上下文组装强化、四件套优化）。

## Suggested Presentation
带学员浏览 8 个章节的主线：
- **01 为什么需要 Knowledge（P3-P6）：** 技术选型三角（Prompt vs RAG vs 微调）、Prompt 局限、Fine-tuning 成本与局限、RAG 核心本质；
- **02 企业多元数据矩阵与知识进库（P7-P9）：** 向量库绝非万能银弹、企业多元数据矩阵（SQL/RAG/图谱/API）、版面感知解析与 2026 Embedding 模型；
- **03 知识怎么存和查（P10-P12）：** pgvector 向量索引、语义检索（RetrievalService）、**【P0 纠正】元数据范围约束过滤 vs 真正混合检索（Dense+BM25+RRF）**；
- **04 知识怎么用（P13-P14）：** context 演进（v1.0 → v3.0）、Prompt + RAG 结构化注入、Token 预算配额控制、防间接提示词注入数据隔离标签；
- **05 完整 10 步 Pipeline 与企业优化（P15-P18）：** 完整 10 步 RAG Pipeline（深度强化组装去重/排序/拒答）、生产四件套（Cache/真正 Hybrid/Rerank/Eval）、量化评测三元组与 RAG 范式全景演进；
- **06 v3.0 Demo 全貌与记忆升级（P19-P21）：** 端到端数据流、Demo vs 生产六项差距、v1.3 项目结构、向量数据库赋能双层语义记忆；
- **07 Long-Context vs RAG 与 Demo 验证清单（P22）：** 剖析超长上下文的四个工程硬伤、可在 v1.3 Demo 亲手验证的核心实验清单；
- **08 第六课总结（P23）：** Agent 五大能力构建进度、第六课能力地图、课后实战练习与第七课 Tool Calling 预告。

## Key Emphasis
强调课程在纠正概念误区（混合检索 vs 元数据过滤）、完善数据接入矩阵、细化 10 步流水线与强化上下文组装上的工业级深度。
