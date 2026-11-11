## Slide Objective
讲清楚 Prompt 为什么装不下企业知识——这是 RAG 存在的根本原因。不是"技术限制"，是"架构设计的必然选择"。

## Suggested Presentation
先问一个问题：导购 Agent 现在能聊天、能记住用户，但用户问"有什么推荐的笔记本"，它答不上来。为什么？

因为它根本不知道公司卖什么。商品信息存在哪？ERP、商品目录、Wiki。这些地方的共同特点：大模型都读不到。

看左边的对比。Prompt 能做的四件事：定义角色（"你是导购助手"）、设约束（"只聊购物"）、给示例（Few-shot）、控格式（JSON 输出）。这些都是"告诉 LLM 怎么做事"。

Prompt 做不到的四件事：包含企业所有商品信息（商品有几千个，每个有配置、价格、适用场景，Prompt 塞不下）、包含内部文档和 FAQ（客服话术、售后政策、产品手册）、实时更新（商品上新了、价格变了，Prompt 不会自动更新）、处理大量结构化数据（库存表、订单表、用户表）。

核心矛盾：大模型的训练数据是公开互联网（截止到某个时间点），企业知识是内部私有的（实时更新）。两者之间有一道天然的鸿沟。Prompt 是"告诉 LLM 怎么做事"，但不能"告诉 LLM 知道什么"。

两个方向解决这个矛盾：把知识"烧"进模型（Fine-tuning），或者让模型自己"查资料"（RAG）。Fine-tuning 下一页讲，这里先建立认知——Prompt 的局限是架构层面的，不是技术层面的。就算你有无限长的 Context Window，把所有商品信息塞进 Prompt 也不现实——成本太高（token 计费）、更新太慢（每次改 Prompt 都要重新部署）。

## Key Emphasis
不要让学员觉得"Prompt 装不下是因为长度限制"。Prompt 装不下是因为架构设计——企业知识应该在外部，不应该硬塞进 Prompt。这是 RAG 存在的根本原因。

## Optional Extension
**学员问：Long Context 都 1M 了，把商品全塞进 Prompt 不就行了？**
答：能塞下不等于该塞——算三笔账：成本、延迟、注意力。
Demo 做法：做一次反例实验——把 `products.json` 全量塞进 `buildUserPrompt()` 的 context，看 token 消耗和回答质量，再切回 RAG 检索对比（page_22 有完整账）。
生产做法：默认 RAG 按需检索（几百 token），Long-Context 只留给"单文档深度分析"场景；用语义缓存和 Token 预算控制成本。取舍：全量塞入还丢了权限过滤——不同部门看到所有内容，这是安全问题不是成本问题。

## Classroom Interaction
让学员现场算一笔账：26 个商品约 8K token，每天 1 万次对话都带上 = 每天多烧 8000 万输入 token。数字一算，Prompt 路线自己就死。

## Take Away
Prompt 讲"怎么做"，不讲"知道什么"——企业知识放外部知识库按需检索注入，别硬塞。

## Why This Slide Matters
跳过这页，学员会把企业知识硬塞 Prompt，成本和"知识更新要重新部署"两个问题上线后才爆发。

## Transition
那把知识"烧"进模型呢？Fine-tuning 行不行？
