## Slide Objective
讲清 Demo 到生产的具体差距——每个差距的问题、后果、做法。诚实面对不完美。

## Suggested Presentation
这一页将 Demo 与生产环境的真实差距全面结构化为三大关键可靠性机制与配套工程能力：

**1. Durable Execution 与跨进程故障重建（Process Rehydration）**
- **问题**：Demo 中的执行状态全在单机内存 Map（`traces` 和 `pendingConfirmations`），一旦容器 OOM、Pod 发生驱逐重启，执行到一半的任务与审批全丢。
- **后果**：用户必须从头重新提问，大模型从头重新规划，且此前已经调用过的外部副作用工具（如扣库存、发邮件）可能被重复调用！
- **生产落地**：全量实施 **Durable Execution**。在每个 Node/Step 完成后，实时将状态机快照持久化到 `workflow_checkpoint` 表（PostgreSQL/Redis）；新实例接管时，读取快照**执行 Process Rehydration 重水化重建**，在内存中反序列化还原完整的状态机与上下文，**直接从断点继续推进，绝不重新规划，绝不重复调用副作用工具**。

**2. 概率规划与确定性 FSM 拓扑校验**
- **问题**：Demo 中的 `TaskPlanner` 仅依靠 Prompt 输出步骤 JSON，缺乏严密的程序级拓扑依赖校验。
- **后果**：大模型的规划本质是概率采样（Probabilistic Planning），生产环境中可能因幻觉输出步骤循环依赖死锁或颠倒因果顺序。
- **生产落地**：确立“概率规划 + 确定性 FSM 约束”架构，对模型输出的 DAG 强制执行**循环依赖检测（有向无环性）与前置条件满足度校验**，校验通过后才被允许注入状态机流转；校验失败安全降级为单步执行。

**3. 错误恢复四层协同矩阵（Error Recovery Synergy）**
- **问题**：Demo 仅实现了单点的重试与熔断，遇到商品缺货等业务报错时无法自愈。
- **生产落地**：建立四层协同矩阵：**① 重试（Retry 应对瞬态抖动）+ ② 熔断降级（Circuit Breaker / Fallback 应对级联雪崩）+ ③ 错误重规划（Replanning 动态自愈换路）+ ④ 人工介入接管（Human Escalation 终极安全底线）**。

**4. 规划触发混合路由**：规则快路径覆盖 85% 典型多步场景，未命中走快模型语义分类兜底。
**5. 审批超时与分布式并发锁**：Redis SETEX 5 分钟自动过期释放挂起状态，数据库乐观锁 version 杜绝超卖。
**6. 分布式链路追踪**：接入 OpenTelemetry 标准，Root Span 绑定 W3C traceparent，步骤转 Span/Event 上报 Jaeger。
**7. 真正多事件流式响应（Agentic SSE）**：推送类型化事件（`step_start`、`tool_call`、`confirm_required`、`answer_delta`），与第七课 AG-UI 前端交互规范无缝闭环。

## Key Emphasis
每个差距都有"问题→后果→做法"的结构。不要只说"缺什么"，要说"缺了会怎样"和"怎么补"。

这页是为架构师准备的——他们需要知道"Demo 到生产还要做什么"。

## Optional Extension
**学员问：这些差距里哪个最优先？**
答：按优先级排：① 确认超时（安全问题）② 持久化 Checkpoint（可靠性）③ 并发控制（正确性）④ SSE 流式（体验）⑤ 规划触发混合路由（回答质量）⑥ 成本控制（运营）⑦ 熔断探测（运维）。安全和可靠性优先。

**学员问：乐观锁怎么做？**
答：数据库表加一个 version 字段。更新库存时：UPDATE stock SET stock = stock - 1, version = version + 1 WHERE id = ? AND version = ?。如果 version 变了（被别人改了），更新失败，返回"库存不足"。这就是乐观锁——不需要加锁，靠版本号检测冲突。
**Demo 做法**：place_order 是模拟返回成功（AgentRuntime.callToolDirectly 里直接回一个 orderId），没有库存扣减，并发问题在 Demo 里看不到。
**生产做法**：上面这条带 version 的 UPDATE 就是并发扣减的最小实现；低冲突吞吐好，秒杀级高冲突换悲观锁或库存预扣。

## Take Away
把 7 项差距转成需求卡片排期（安全 → 可靠性 → 正确性 → 体验），每张卡片写清问题/后果/做法——这就是 Demo 转生产的施工图。

## Why This Slide Matters
如果跳过这页，学员以为 Demo 就是生产可用的——到企业里会踩坑。这页教的是"我知道差距在哪、怎么补"。

## Transition
差距清单列完了，空口无凭——下一页把这些断言逐条变成动手实验（8 个验证点），验完再看能力地图和总结。