# Page 1 — 封面：Memory 记忆系统（让 Agent 记住用户）

## Slide Objective
开场定调：从"理解 AI"进入"让 AI 记住用户"。

## Suggested Presentation
上节课完成了导购 Agent 第一版，但它每次对话都是陌生人。今天解决这个问题。

今天的主题是 Memory——记忆系统。不是"聊天记录"，而是"企业为 AI 建立的记忆系统"。聊天记录只是存消息，Memory 是结构化的、可压缩的、跨 Session 生效的用户记忆体系。

## Key Emphasis
定调词要准：今天讲的是"记忆系统"，不是"聊天记录怎么存"。开场就点明本课（Tag `v1.2.1`）核心落地的是 `enterprise-ai-agent` 的 `memory/` 包和两个存储（Redis 会话缓冲与累积摘要、MySQL 用户画像），下一课（第六课）再引入第三个（pgvector 向量库）。不要在开场展开架构细节——本段只负责把问题立起来。

## Optional Extension
**学员问：记忆系统跟"把历史塞进 messages 数组"有什么本质区别？**
答：区别在"塞多少、塞什么、谁来选"。
Demo 做法：本课 Demo（`v1.2.1`）里，`SessionMemory`（Redis）存最近对话，`SummaryPipeline` 在消息超 8 条时将早期消息压缩为累积摘要，`MemoryManager.buildContext()` 负责把 MySQL 用户画像 + Redis 摘要与近期对话分块拼装。打开 `logs/llm-interactions.log`（或 `logs/llm-chat.log`）能看到每次真正注入了多少。
生产做法：历史全量塞 messages 只适合短会话；长会话按 token 预算装配 context（系统提示 + 画像 + 摘要/检索记忆 + 最近消息），超预算先裁旧消息、再走后台压缩。第六课看 context 加入 RAG 结果后怎么继续扩展，第八课 Workflow 讲多轮任务的上下文预算。

## Classroom Interaction
开场 30 秒做个实验：让学员在前端新会话问一句"我叫什么"，然后打开 `logs/llm-interactions.log` 看这次请求的上下文——没有任何历史记忆。本课要解决的问题现场成立。

## Take Away
Memory 是服务端的、结构化的、跨 Session 的用户状态——它是你的代码和存储选型，不是模型自带的能力。

## Why This Slide Matters
跳过这页的定调，学员会把今天当成"聊天记录存哪儿"来听，最后把对话原文全塞 MySQL 或把長期画像全塞 Redis——三层各司其职的判断从第一页就丢了。

## Transition
在解决这个问题之前，我们先来看一下今天的学习地图（Page 2：目录）。
