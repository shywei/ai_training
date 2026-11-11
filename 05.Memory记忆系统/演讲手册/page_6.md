# Page 6 — Session Memory — Demo 实现

## Slide Objective
展示 Demo 的 Session Memory 实现（`SessionMemory.java`），讲解关键代码和踩坑经验。

## Suggested Presentation
Demo 选了 Redis 方案。看左边的 Redis 结构。Key 是 `session:{sessionId}:messages`，List 结构。每条消息格式是 `role|content`，用 `|` 分隔角色和内容。为什么用 `|` 分隔而不是存 JSON？因为 List 的每个元素是字符串，用 `|` 分隔最简单，读取时 `split("\\|", 2)` 一下就行。踩坑经验：如果用户消息里包含 `|` 字符怎么办？`split("\\|", 2)` 限制只按第一个 `|` 切分，但生产项目中还是应该用标准 JSON 或 Protobuf 序列化。

看右边的 `SessionMemory.java` 代码。`addMessage` 方法做三件事：`rightPush`（RPUSH）追加、`expire`（EXPIRE）设 24 小时过期、超过 `MAX_MESSAGES = 20` 时 `leftPop`（LPOP）裁剪最旧的消息。

这里的 `MAX_MESSAGES = 20` 是滑动窗口的**安全兜底硬上限**。而在正常对话中，当消息超过 8 条时，我们的 `SummaryPipeline`（Page 8 详讲）就会先把旧消息压缩为 1 条累积摘要 + 4 条最新原文，因此消息数平时会維持在 5–8 条之间；只有当摘要压缩异常未生效时，20 条硬上限才会触发 `leftPop` 强行裁剪，防止 Redis 内存失控。

验证方法：用 `redis-cli LRANGE session:xxx:messages 0 -1` 或调用 `GET /api/agent/memory?sessionId=xxx` 可以直接查看当前对话内容。

## Key Emphasis
代码讲解要具体到方法名和参数。讲清楚 `SessionMemory` 的 20 条硬上限兜底与 `SummaryPipeline` 的 8 条摘要压缩阈值之间的配合关系。

## Optional Extension
**学员问：`role|content` 这种分隔符序列化，用户消息里带分隔符或换行怎么办？**
答：Demo 用两层小技巧兜住，生产必须换结构化序列化。
Demo 做法：`SessionMemory.addMessage` 把换行转义成 `\n`，读取时 `split("\\|", 2)` 只切第一个分隔符——用户内容里再出现 `|` 也不会误切。教学演示够用。
生产做法：每条消息存 JSON（`role/content/timestamp/tokenCount`），或用 Redis Stream 的结构化字段；并发写同一 Session 用原子操作或分布式锁保证顺序。

## Classroom Interaction
让学员连发两条消息后跑 `redis-cli LRANGE`——看顺序、看 `role|content` 格式。

## Take Away
滑动窗口的 `MAX = 20` 是"条数兜底上限"而非"token 上限"——单纯靠滑动窗口丢弃旧消息会丢上下文，所以下一章必须引入上下文压缩。

## Why This Slide Matters
把简单分隔符序列化和"无并发控制"照搬到生产，会出消息错切、顺序错乱两类 bug——特征是偶发、难复现，排查成本极高。

## Transition
单纯靠滑动窗口裁剪会丢失早期关键信息。怎么在控制长度的同时保留历史信息？我们进入第四章（Page 7：上下文压缩 — 策略对比）。
