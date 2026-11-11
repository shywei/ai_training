# Page 7 — 上下文压缩 — 策略对比

## Slide Objective
讲透上下文压缩的完整知识体系——四种策略的原理、实现细节，以及多轮工具调用 Agent 中的 **Observation Masking（历史工具大返回折叠裁剪）** 工程实践。

## Suggested Presentation
对话越来越长，Token 成本越来越高。需要压缩。但压缩有四种基础策略，外加一项针对 Tool-using Agent 的关键裁剪机制，各有取舍。

**策略一：滑动窗口**

最简单。保留最近 N 条消息，裁掉旧的。N 的选择很关键——太小会丢失重要上下文，太大失去了压缩的意义。Demo 的 `SessionMemory` 里设了 20 条安全上限，超过就 LPOP 最旧的。

滑动窗口的问题：用户前面说过"我预算 8000"，被裁掉后 Agent 就忘了。这在短对话里不是问题，但在长对话里是致命的——用户聊了 15 轮，早期的关键信息全丢了。

**策略二：累积摘要（本课 Demo 选择）**

核心思想：不是直接丢弃旧消息，而是用 LLM 把旧消息压缩成摘要，摘要和最近的新消息共存。

具体实现：在本课 Demo（`SummaryPipeline.java`）中，当消息数超过阈值（`COMPRESS_THRESHOLD = 8`），取出前 N-4 条（保留最近 `KEEP_RECENT = 4` 条原文），把这些旧消息（含之前的旧摘要）拼成一段文本，让快模型（`agent.model.fast`）压缩成 300 字以内的更新摘要，写回 Redis（`session:{sid}:summary` / `messages`），与最近 4 条原文共存。

累积的关键：第二次压缩时，旧摘要也会被传给 LLM。Prompt 里明确告诉 LLM："之前已经有一份摘要，现在需要将旧摘要和新对话合并为一份更新的摘要"。这样摘要 B 包含摘要 A 的信息，不会丢失历史。

**策略三：关键词抽取**

从对话中提取关键实体存入结构化字段。比如用户说"我预算 8000，喜欢轻薄本"，提取出 `preferences` 存入 MySQL。优点是精确、可查询；缺点是丢失上下文语境。通常和画像系统配合使用——我们第五章讲的 `UserProfileExtractor` 本质上就是结构化抽取。

**策略四：Embedding 压缩（第六、七课演进方向）**

把历史对话或片段摘要转为向量，存入向量数据库（如 pgvector）。需要时按用户当前问题做语义检索，召回最相关的 top-k 记忆片段。下一课（第六课）引入 pgvector 后，我们就会把累积摘要进一步升级为向量化记忆沉淀。

**🛠️ 进阶工程实践：Observation Masking（历史工具大返回折叠裁剪）**

这一点在后续第七、八课（Tool Calling 与 Workflow）至关重要：在带工具调用的 Agent 中，真正把 Context Window 撑爆的往往不是用户的寥寥几句提问，而是历史轮次中工具返回的几 KB 甚至几十 KB 的 JSON 原始报文（即 `role=tool` 的 Observation，如商品列表 JSON、订单详情 JSON）。
生产级 Agent 的标准做法是 **Observation Masking**：保留最近 N 轮（如最近 1–2 轮）的完整工具返回，而对超过 N 轮之前的历史 `role=tool` 观测结果进行**折叠掩码或单行摘要替换**（例如替换为 `[Tool Observation Masked: search_products returned 15 items, see assistant reply for summary]`）。这样既保留了工具调用的轨迹（Agent 知道自己查过什么），又瞬间砍掉了 80% 以上的历史冗余 Token！

## Key Emphasis
四种基础策略的取舍 + **Observation Masking（裁历史 `role=tool` 大 JSON，保留近期轮次）** 是核心亮点。累积摘要的"合并旧摘要"机制为下一页 Demo 代码讲解做好铺垫。

## Optional Extension
**学员问：累积摘要压缩会不会把"不要黑色"这种关键约束在多轮缩写中压丢？**
答：会，多轮有损压缩存在"信息稀释效应"，所以关键约束不能只活在摘要里。
Demo 做法：本课 Demo 用双轨制兜底——`SummaryPipeline` 负责压缩对话过程摘要，而 `UserProfileExtractor` 将核心事实与约束（`facts / preferences / tasks / constraints`）独立抽取到 MySQL `user_profile` 表并全量注入。
生产做法：核心约束走结构化画像（结论层），过程对话走摘要或向量检索（过程层），历史工具大返回走 Observation Masking。

## Classroom Interaction
提问学员："如果 Agent 连续调用了 5 次商品搜索接口，每次返回 20 个商品的 JSON（约 4000 tokens），第 6 轮对话时输入 token 会发生什么？用刚才讲的哪个技术解决最直接？"（答案：输入暴涨 2 万 tokens；用 Observation Masking 折叠前 4 轮的 `role=tool` JSON 返回）。

## Take Away
上下文压缩是组合拳：**滑动窗口保近期 + 累积摘要保脉络 + 画像抽取保结论 + Observation Masking 砍工具大返回**。

## Why This Slide Matters
只懂压缩自然语言对话、不懂裁剪历史 `role=tool` 观测结果的工程师，到了第七、八课接入真实业务 API 时，3 轮工具调用就会把上下文窗口撑爆。

## Transition
看完了策略对比与 Observation Masking，本课 Demo 的 `SummaryPipeline` 累积压缩具体是怎么实现的？请看下一页（Page 8：上下文压缩 — Demo 实现）。
