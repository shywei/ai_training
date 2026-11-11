## Slide Objective
正面回应"1M 超长上下文能否替代 RAG"的架构质疑（剖析四个工程硬伤），并给出 **`v1.3` 单体架构下可在 Demo 亲手验证的 5 个核心实验点**（涵盖 `POST /api/knowledge/index`、`RetrievalService`、`isProductRelated`、`MemoryManager` 四路注入与 `UserMemoryService` 语义记忆沉淀）。

## Suggested Presentation
这一页解决两个核心问题：第一，在架构评审会上回答老板"有了百万长上下文还要不要建 RAG"；第二，给出今天下课前大家在 `v1.3` 代码上亲手验证的 5 个实验清单。

看左栏：**Long-Context vs RAG 架构对决**。
现在主流模型上下文窗口都到了 128K~1M tokens，很多管理层会问："为什么不把全套产品目录和手册每次全塞进 Prompt，省掉切块和 pgvector？"
你可以直接拿这四个硬伤回答：
1. **成本差距（100 倍以上）**：每次对话全量塞入 10 万 tokens，而 RAG 只检索最相关的 800 tokens；每天 1 万次对话，成本相差两个数量级。
2. **首字延迟（TTFT 飙升）**：10 万 tokens 输入做 Prefill 计算，首字返回延迟从几百毫秒飙升到十几秒，交互式导购根本等不起。
3. **注意力迷失（Lost in the Middle）**：上下文过长时，模型对首尾敏感、对中间段落命中率大幅衰减。
4. **无法做细粒度权限过滤（ACL）**：全量塞入等于所有员工/用户看到同一份全量数据；而 RAG 可以在 `pgvector` 检索层按 `tenant_id / dept_id` 精确过滤。
所以结论非常明确：**默认用 RAG 做海量知识的低成本精准召回，再用 Long-Context 窗口对召回的多篇长章节做深度综合分析——两者互补，绝非替代！**

再看右栏：**🧪 可在 Demo 验证哪些点（`v1.3` 实操清单）**。
大家切到 Git 标签 `v1.3`（单体 `agent-runtime`，端口 `8080`），按这 5 步逐一验证：
1. **验证手动索引（`KnowledgeController`）**：执行 `curl -X POST http://localhost:8080/api/knowledge/index`，看控制台逐条将 `products.json` 的 26 个商品向量化写入 PostgreSQL `vector_store` 表。
2. **验证语义检索与参数旋钮（`RetrievalService`）**：在前端提问“写代码的笔记本”，断点观察 `SearchRequest.query(query).withTopK(3).withSimilarityThreshold(0.3)` 返回的 `List<Document>`；试着把阈值改成 `0.85` 验证阈值拦截效果。
3. **验证关键词前置过滤（`RetrievalService.isProductRelated()`）**：发一句`"你好呀"`，打开 `logs/llm-chat.log`，确认未触发向量检索、Prompt 中没有 `## 参考知识`。
4. **验证四路 Context 注入（`MemoryManager.buildContext()`）**：问`"推荐一台适合出差写代码的笔记本"`，在 `logs/llm-chat.log` 中同时看到 `## 用户画像`（MySQL `user_profile`）、`## 相关记忆`（pgvector `user_memory`）、近期对话（Redis）与 `## 参考知识`（pgvector `vector_store`）。
5. **验证语义记忆沉淀与跨会话召回（`UserMemoryService` + `UserMemoryMapper`）**：告诉 Agent `"记住我平时还做视频剪辑，希望显卡性能强一些"`，触发沉淀进 pgvector `user_memory` 表；清空 Redis 会话后重新提问`"给我推荐几款电脑"`，验证 `UserMemoryService.search()` 跨会话召回成功！

## Key Emphasis
1) Long-Context 解决"单次读得下"，RAG 解决"海量查得准、花得少、控得住权限"；2) 5 个 Demo 验证点全部锚定在 `v1.3` 的真实类与接口（`POST /api/knowledge/index`、`RetrievalService`、`UserMemoryService`、`MemoryManager`）。

## Optional Extension
**学员问：能在 `v1.3` Demo 里亲手测一下 Long-Context 全量塞入 vs RAG 的 Token 差距吗？**
答：完全可以！把 `products.json` 全部 26 个商品拼成一个大字符串直接塞进 `PromptManager.buildUserPrompt()`，对比走 `RetrievalService.retrieve()` 检索 Top-3 的输入 Token 数——哪怕只有 26 个商品，输入 Token 也差了近 10 倍；如果是 2600 个商品，全量塞入当场就会超窗报错。

## Classroom Interaction
让学员现场执行右栏的第 ① 步（`POST /api/knowledge/index`）和第 ② 步，并在 PostgreSQL 中执行 `SELECT count(*) FROM vector_store;` 與 `SELECT count(*) FROM user_memory;` 核对表数据。

## Take Away
架构上认准"RAG 检索 + Long-Context 精读"的黄金组合；代码上亲手跑通 `v1.3` 的索引、检索、过滤、四路注入与记忆沉淀五步验证。

## Why This Slide Matters
跳过这页，学员既容易被"百万上下文淘汰 RAG"的营销话术带偏，又缺少一份清晰的动手实验清单去检验自己对 `v1.3` 代码的掌握程度。

## Transition
实验清单明确了，最后我们翻到今天课程的收官总结页，回顾 Agent 的能力进化版图。（page_23）
