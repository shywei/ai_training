# Page 11 — context 注入 — 贯穿后续课程的主线

## Slide Objective
展示 `context` 参数的演进——贯穿后续所有课程的主线（`MemoryManager.buildContext()`）。

## Suggested Presentation
这一页既是架构主线也是 Demo 核验页。看左边的 `context` 演进路线：
- **v1.0 第四课**：`context = null`（只有 System Prompt + 用户当前问题）
- **v2.0 第五课（本课 `v1.2.1`）**：`context = 用户画像（MySQL）+ 对话历史与累积摘要（Redis）`
- **v3.0 第六课**：`context = 画像 + 对话 + RAG 知识检索结果`
- **v4.0 第七课**：`context = 画像 + 对话 + RAG + Tool 调用结果（MCP）`

关键设计决策：`PromptManager.buildUserPrompt(message, context)` 的方法签名始终不变，变化的只是 `context` 字符串的装配内容！这就是为什么第四课要把 Prompt 构建独立成组件并预留 `context` 参数——为后续每一课的能力叠加留好扩展点。

看右边本课 v2.0（`MemoryManager.buildContext`）实际生成的 `context` 内容：
第一块是来自 MySQL `user_profile` 的 `## 用户画像`（事实、偏好、任务、约束）；
第二块是来自 Redis `SessionMemory` 与 `SummaryPipeline` 的 `## 对话历史`（含累积摘要与最近对话）；
最后在 `PromptManager` 中拼上 `## 用户问题`。

验证方法：打开 `logs/llm-interactions.log`（或 `logs/llm-chat.log`），查看 `[Memory Injection]` 和 `[User Prompt]`，能直观看到 `context` 是怎么一步步装配并注入给 LLM 的。

## Key Emphasis
一个设计点讲透：`PromptManager.buildUserPrompt(message, context)` 接口不变，变的只是 `context` 的内容——v1.0 `null` → v2.0 `画像+对话` → v3.0 `+RAG` → v4.0 `+Tool`。右边的 `context` 实物和 `logs/llm-interactions.log` 要对照着讲。

## Optional Extension
**学员问：`context` 一直往里加东西，早晚塞爆，怎么办？**
答：分块 + 配额，不是无脑拼接。
Demo 做法：本课 Demo 每块都有天然边界——用户画像是合并后的四行结构化字段，对话历史有 `SummaryPipeline`（满 8 条压成 300 字内累积摘要 + 保留 4 条原文）和 `SessionMemory`（MAX 20）双重限流。
生产做法：给 `context` 做显式 token 预算分配（系统提示 / 画像 / 知识检索 / 对话历史各有配额），超预算按优先级裁剪（用户当前问题 > 画像结论 > 检索结果 > 早期历史）。

## Classroom Interaction
打开 `logs/llm-interactions.log`，让学员在请求日志里圈出 `## 用户画像`、`## 对话历史`、`用户问题` 三块——亲眼看到 `context` 是分块装配出来的。

## Take Away
把 `context` 写成分块装配器（`MemoryManager.buildContext`）：每块有明确存储来源与长度上限——接口不变，Agent 能力随课程渐进叠加。

## Why This Slide Matters
把 Prompt 拼接硬编码在 Controller 里的项目，到了第六课加 RAG、第七课加 Tool 时就要推倒重写——预留 `context` 分块装配接口是 Agent 工程化的基本功。

## Transition
`context` 注入把短期记忆和长期画像串联起来了。但在把这套系统推向企业生产之前，有一个红线问题必须先回答：记忆安全与隐私。请看第七章（Page 12）。
