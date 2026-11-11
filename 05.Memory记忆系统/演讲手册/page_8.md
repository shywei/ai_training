# Page 8 — 上下文压缩 — Demo 实现

## Slide Objective
展示本课 Demo（`v1.2.1`）的上下文累积压缩实现（`SummaryPipeline.java`），讲解代码细节、双模选型踩坑经验与日志验证。

## Suggested Presentation
看左边的累积压缩流程（`SummaryPipeline.java`）。当 Redis 中的会话消息超过 8 条（`COMPRESS_THRESHOLD = 8`）时触发压缩：取出前 `N - 4` 条消息（包含可能已存在的旧摘要），调用快模型压缩为更新后的累积摘要，写回 Redis（`session:{sid}:summary` / `session:{sid}:messages`），并保留最近 4 条原文（`KEEP_RECENT = 4`）。

关键设计：第二次触发压缩时，旧摘要也会被传给 LLM。看 `SummaryPipeline.java` 的 `summarizeSession` 方法：它会检查待压缩消息中是否包含之前的 `[对话摘要]`——如果有，Prompt 会明确指示 LLM："之前已经有一份摘要，现在需要将旧摘要和新对话合并为一份更新的摘要。保留所有关键信息，不超过300字。"
- 第一次满 8 条：前 5 条 → **累积摘要 A** + 保留最近 4 条原文
- 第二次再满 8 条：累积摘要 A + 中间旧消息 → **累积摘要 B（含 A）** + 保留最新 4 条原文

踩坑经验 1（旧摘要合并）：一开始 Prompt 只写了"压缩以下对话"，没有告诉 LLM 合并旧摘要，结果每次压缩都把更早期的背景丢了。加上"合并旧摘要"指令后，实现了真正的累积摘要。

看左下角的避坑卡片：**🚨 压缩与抽取双模选型**。
文本摘要压缩只做段落缩写，不需要复杂推理能力，使用快模型（`agent.model.fast`，如 Qwen2.5-7B，temperature 0.1）即可胜任，速度快、成本低；但下一页要讲的用户画像抽取涉及严格 JSON Schema 输出，快模型极易发生引号缺失、多余逗号等语法残缺报错，因此画像抽取必须强制路由至推理模型（`agent.model.reasoning`，如 Qwen3-8B）。

看右边的关键设计：
1. **阈值**：`COMPRESS_THRESHOLD = 8`、`KEEP_RECENT = 4`。Demo 设为 8 条触发是为了方便课堂现场演示（聊 4–5 轮就能看到效果），生产环境通常设为 30–50 条。
2. **模型**：`agent.model.fast`（从 `application.yml` 读取，不硬编码）。
3. **独立日志**：`logs/llm-summary.log`。每次触发压缩时，都会在 `logs/llm-summary.log` 打印 `SUMMARY REQUEST`（含策略、完整 Prompt、旧摘要）和 `SUMMARY RESPONSE`（耗时与新摘要）。
4. **演进预告**：第六、七课引入 pgvector 与 MCP 后，这套累积摘要将进一步升级为满 20 条沉淀为 pgvector 向量化记忆。

## Key Emphasis
`SummaryPipeline` 的三个核心数字（阈值 `8`、保留 `4`、300 字以内累积合并）、双模选型（压缩用 `agent.model.fast`、画像 JSON 抽取用 `agent.model.reasoning`），以及用 `logs/llm-summary.log` 现场验证压缩前后对比。

## Optional Extension
**学员问：压缩用快模型（`agent.model.fast`）、画像抽取用推理模型（`agent.model.reasoning`），能不能统一用一个模型省事？**
答：不建议，两个任务的质量要求与延迟敏感度完全不同。
Demo 做法：在 `application.yml` 中按职责路由——摘要压缩走 `agent.model.fast`（Qwen2.5-7B），画像抽取走 `agent.model.reasoning`（Qwen3-8B）。打开 `logs/llm-summary.log` 和 `logs/llm-profile.log` 能直观看到两次调用使用了不同模型。
生产做法：按任务分级选模——结构化 JSON 输出任务留足质量预算，纯文本缩写任务用高吞吐便宜模型。

## Classroom Interaction
现场连聊 5 轮（超过 8 条消息），让学员打开 `logs/llm-summary.log` 查看 `SUMMARY REQUEST` 和 `SUMMARY RESPONSE`，再跑一次 `redis-cli LRANGE session:default:messages 0 -1`（或调用 `GET /api/agent/memory?sessionId=...`），亲眼看到前几轮对话变成了 1 条 `[对话摘要]` 加最近 4 条原文。

## Take Away
累积摘要的核心是"旧摘要 + 新对话 → 合并新摘要"；模型按任务选型（压缩用 `agent.model.fast`，结构化 JSON 抽取用 `agent.model.reasoning`），并通过 `logs/llm-summary.log` 可观测。

## Why This Slide Matters
一刀切只用一个模型的项目，要么成本和延迟爆炸（全用推理模型压历史），要么结构化输出天天解析报错（全用小模型抽 JSON）——这是 Demo 真实踩过又总结出来的工程准则。

## Transition
Session Memory 和累积压缩解决了"记住当前这轮对话"。但还有一个更重要的问题：怎么跨会话记住用户是谁？我们进入第五章（Page 9：用户画像 — 设计模式）。
