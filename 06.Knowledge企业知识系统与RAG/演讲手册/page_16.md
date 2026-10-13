## Slide Objective
给学员一个 v3.0 Demo 的完整视角——做成了什么、怎么串起来的、演示时怎么看。这页是 PPT 讲解结束后的 Demo 演示指南。

## Suggested Presentation
前面 14 页讲了 RAG 的完整知识体系。现在我们看看这些知识在 Demo 里是怎么落地的。

看左边的 v3.0 vs v2.0 对比。v2.0 的导购 Agent 能聊天、能记住用户，但不知道商品信息。v3.0 新增了四件事：商品知识检索（pgvector）、语义检索用户记忆（pgvector）、自动沉淀（Redis → pgvector）、context 扩展（画像+记忆+对话+知识）。

看端到端的数据流。用户问"推荐一台写代码的笔记本"，`MemoryManager.buildContext()` 组装四部分上下文：① MySQL 查用户画像（偏好、事实）。② pgvector 语义检索用户记忆（历史对话中相关的片段）。③ Redis 读当前会话历史（沉淀后这部分为空）。④ pgvector 关键词过滤后语义检索商品知识。四部分组装成 context，发给 LLM，生成推荐回答。

右边是四个关键设计决策，每个都是这节课的核心成果。

数据分层：商品描述放 pgvector（语义检索），价格库存放 MySQL（第七课 Tool 查询）。不是所有数据都适合放向量库——价格会变，向量库存旧价格会误导。

手动索引：不自动加载，前端按钮触发 `POST /api/knowledge/index`。Embedding 是一个显式动作，学员应该看到这个过程。

自动沉淀：每 10 条消息（5 轮对话），Redis 消息沉淀到 pgvector，然后清空。Redis 只存"还没处理的"，pgvector 存"已经处理过的"。

记忆不重叠：MySQL 存结构化画像（LLM 提取的结论），pgvector 存对话原文片段（原始记忆）。两者互补，不冲突。

底部是演示步骤建议。PPT 讲完后，按这个顺序演示 Demo，每个步骤验证一个核心概念。学员可以跟着做，也可以课后自己验证。

## Key Emphasis
这页是"从知识到实践"的桥梁。前面讲了 RAG 的原理、组件、优化，这页展示它们在 Demo 里是怎么组合的。演示步骤对应课后练习，学员课后可以自己跑一遍。

## Transition
Demo 做成了什么清楚了。接下来看 Demo 和生产环境的差距，以及代码怎么读。
