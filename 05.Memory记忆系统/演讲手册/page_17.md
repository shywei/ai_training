# Page 17 — Demo 架构 — 组件与日志

## Slide Objective
展示本课 Demo（Git Tag `v1.2.1`）的完整文件结构、三大日志文件、模型配置与 6 个课堂/课后可验证点，帮学员建立清晰的源码阅读路线图。

## Suggested Presentation
这是第五课（v2.0 记忆系统，对应仓库 Git Tag `v1.2.1`）的完整新增组件。

**第一步：读代码的五步路线图**
1. 先看 `core/ShoppingAgentController.java`：所有请求的入口。重点看 `/chat`（调用 `memoryManager.buildContext` 拼装上下文）、`/memory`（查询会话缓冲）、`/profile/extract`（触发画像抽取）、`/memory/clear`（清空会话并兜底抽取）。
2. 再看 `memory/MemoryManager.java`：记忆系统的总装枢纽。`buildContext(sessionId, userId)` 从 MySQL 查用户画像（`UserProfileMapper`），从 Redis 查会话历史与累积摘要（`SessionMemory`），拼成 `context` 字符串；并在消息超阈值时调用 `SummaryPipeline`。
3. 第三步看 `memory/SessionMemory.java`：封装 Redis List 操作（`session:{sid}:messages`，`MAX_MESSAGES = 20`，24h TTL，`role|content` 格式）。
4. 第四步看 `memory/SummaryPipeline.java`：累积摘要压缩流水线（`COMPRESS_THRESHOLD = 8`，`KEEP_RECENT = 4`，合并旧摘要与新对话，使用 `agent.model.fast`，日志写入 `logs/llm-summary.log`）。
5. 第五步看 `memory/UserProfileExtractor.java`：`@Async` 异步用户画像抽取器（读取 MySQL 旧画像 + Redis 新对话，调用 `agent.model.reasoning` 输出严格 JSON 并更新 `user_profile` 表，日志写入 `logs/llm-profile.log`）。

**第二步：三个独立日志文件（调试与演示的核心工具）**
- `logs/llm-interactions.log`（或 `logs/llm-chat.log`）：记录每次对话请求/响应以及 `[Memory Injection]` 实际注入的 `context` 内容。
- `logs/llm-summary.log`：独立记录 `SummaryPipeline` 的 `SUMMARY REQUEST`（含旧摘要）与 `SUMMARY RESPONSE`（新累积摘要）。
- `logs/llm-profile.log`：独立记录 `UserProfileExtractor` 的 `PROFILE EXTRACT REQUEST`（旧画像 JSON + 新对话）与 `PROFILE EXTRACT RESPONSE`（合并后的 JSON）。

**第三步：模型配置（`application.yml`）**
- `agent.model.fast`：快模型（如 `Qwen/Qwen2.5-7B-Instruct`），用于日常对话路由与 `SummaryPipeline` 摘要压缩；
- `agent.model.reasoning`：推理模型（如 `Qwen/Qwen3-8B`），用于复杂问题推理与 `UserProfileExtractor` 严格 JSON 画像抽取。

**第四步：可在 Demo (`v1.2.1`) 亲手验证的 6 个检查点**
1. **① 失忆基线**：新会话未注入历史时问"我叫什么" → 不知道；查看 `logs/llm-interactions.log` 确认初始 `context=null`。
2. **② Redis 缓冲验证**：发几条消息后执行 `redis-cli LRANGE session:{sid}:messages 0 -1`，查看 `role|content` 消息列表。
3. **③ TTL 滑动刷新**：执行 `redis-cli TTL session:{sid}:messages` 观察 86400 秒倒计时；再发一条新消息，TTL 重新刷新回接近 86400。
4. **④ 满 8 条触发累积摘要压缩**：连续对话超过 8 条消息（4 轮以上）→ 触发 `SummaryPipeline`，打开 `logs/llm-summary.log` 查看压缩 Prompt 与新摘要，再查 Redis 确认变成 `1 条累积摘要 + 4 条最新原文`。
5. **⑤ 画像抽取合并**：在对话中输入包含“记住/记一下”关键词的个人事实与偏好（或点击“清空会话”）→ 打开 `logs/llm-profile.log` 查看旧画像 JSON、新对话与推理模型合并后的 JSON。
6. **⑥ 画像落库与跨 Session 生效**：查 MySQL `SELECT * FROM user_profile WHERE user_id='zhangsan'`；点击"清空会话"或重新登录，问"根据我的偏好推荐笔记本" → 验证长期画像跨 Session 生效！

## Key Emphasis
"怎么按调用链读代码 + 怎么对照三大日志文件验证"是本页核心。务必提醒学员切到 `v1.2.1` 标签做实验，6 个验证点一步步走通。

## Optional Extension
**学员问：怎么最快读懂一个陌生的 Agent 记忆模块？**
答：不要按字母顺序读文件，要**按一次请求的数据流向读**，并且边跑边看 `logs/llm-interactions.log`、`llm-summary.log`、`llm-profile.log` 三个日志文件——日志里打印的 Prompt 就是代码逻辑最直观的投影。

## Classroom Interaction
带着学员现场核对一次 `logs/` 目录下的三个日志文件，指出哪个看 `context` 注入、哪个看 8 条摘要压缩、哪个看画像 JSON 抽取。

## Take Away
读 Demo 按调用链走：`ShoppingAgentController` → `MemoryManager` → `SessionMemory` / `SummaryPipeline` / `UserProfileExtractor`，每一步都在三个日志文件和 Redis/MySQL 里有迹可循。

## Why This Slide Matters
给清代码阅读路线和 6 个具体验证点，学员课后自己动手才不会迷路，也为第六课叠加 RAG 知识检索打下坚实的代码底座。

## Transition
Demo 的代码结构与 6 个验证点理清了。在全课收尾总结之前，我们抬眼看看 2026 年业界主流的记忆系统都在怎么做，给我们的三层记忆找准行业坐标系。请看下一页（Page 18：业界记忆系统一览）。
