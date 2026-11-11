# page_15 · 三级容错① 重试 — 触发条件 · 参数取值 · 失败模式

## Slide Objective
把"重试"从 page_13 的一句"最多 3 次、指数退避"拆到工程粒度：什么时候触发（`AgentRuntime.executeToolCall()` 的 while(true) 循环 + `ErrorRecovery.shouldRetry()`）、参数为什么是这些数（MAX_RETRY=3、2^retryCount×1000 = 1s/2s/4s）、重试自己会怎么坏（耗尽降级、非幂等重试放大、退避惊群）。对应 Demo 的 `ErrorRecovery.java` 与 `AgentRuntime.executeToolCall()` 的重试循环。

## Suggested Presentation
先说清重试在调用链上的位置。`AgentRuntime.executeToolCall()` 是工具调用的唯一入口：入口先查熔断（下一页），再查确认（page_18），最后进 `while(true)` 重试循环。循环里 `callToolDirectly()` 抛异常、或者返回 null（Demo 把 null 也当失败），就进 catch——这是重试的**触发点**。catch 里三件事按顺序做：`step.incrementRetry()` 记账、`errorRecovery.recordFailure()` 把失败记进熔断计数（注意：重试的每次失败都喂给熔断，两级是串联的）、然后 `shouldRetry(step)` 判 `retryCount < MAX_RETRY` 决定睡一觉再来还是收工。

参数取值讲三个决定。第一，**MAX_RETRY=3，计数挂在 `WorkflowStep.retryCount` 上，每一步独立**——Step 1 重试完了不影响 Step 2 的预算。第二，**退避是指数不是线性**：`getRetryDelayMs() = 2^retryCount × 1000`，1s、2s、4s。为什么不固定 1s？固定间隔在下游真宕机时等于匀速打它；指数退避把总等待拉到 7 秒，给下游重启窗口，又不至于让用户等太久——这是查询类工具能接受的上限。第三，**成功即 `recordSuccess()` 清零**，但注意清的是熔断计数，不是这一步的 retryCount。

再讲失败模式，这是这页真正的价值。**耗尽不是终点**：`shouldRetry` 返回 false 就 `step.fail("重试耗尽")`、`executeToolCall()` 返回 null，上层把这个 null 读作"降级信号"——所以重试和降级是硬连线的。**重试会放大副作用**：`search_knowledge` 是只读的，重试安全；`place_order` 重试一次就是两次下单，而 Demo 的下单是 `ORD-` + 时间戳模拟、没有幂等键，两次重试返回两个不同订单号——这个现场在验证点页（page_24 ⑧）让学员亲手点出来。**退避没有抖动**：并发请求同时睡、同时醒，下游刚恢复就被齐射打回去。

设计弯路讲一条：Demo 当初的取舍是"重试逻辑必须让学员看见，不能藏进框架"——`ErrorRecovery` 全部 151 行，重试就 3 个方法。生产上这不是自己造的理由，Resilience4j 的 Retry + CircuitBreaker 组合是标配；但看懂这 151 行，你才知道框架的 Retry 配置项背后在调什么。

## Key Emphasis
必须讲清：触发点在 catch、判定在 `shouldRetry`、每次失败同时喂熔断计数；退避公式是 `2^retryCount × 1000`；耗尽返回 null 是给降级的信号，不是异常往外抛。

不要展开：MCP Client 的重连冷却（`ensureConnected()` 的 10s 冷却）那是连接层自愈，和工具级重试是两层，提一句就行，别混讲。

## Optional Extension
**学员问：为什么重试次数是 3 次，不是 5 次或者无限重试到成功？**
答：先分清这是"预算"不是"目标"——重试次数乘以每次耗时就是用户可感知的等待。
Demo 做法：3 次 + 1s/2s/4s，最坏 7 秒等待后降级，`shouldRetry()` 一个比较就完事，课堂上能完整演一遍（停掉 mcp-knowledge-server 看日志）。
生产做法：重试预算跟着链路 deadline 走（上游剩余 2s 就只许再试 1 次），次数从"常量"变成"剩余预算的函数"；读写分开——只读自动重试，写操作等幂等键（第七课幂等页）就位才重试。取舍：无限重试在分布式系统里就是雪崩的种子，宁可降级。

**学员问：重试会不会把下单重复执行？**
答：会，这是重试最经典的坑，Demo 现在就没防。
Demo 做法：`place_order` 是模拟调用（`callToolDirectly()` 的下单分支返回 `ORD-` + `System.currentTimeMillis()`），连续两次"下单"拿两个不同订单号，学员自己就能验证"没有幂等键"。
生产做法：先有幂等键（第七课），再谈重试——同一业务动作带同一 requestId，服务端查重表，重复请求直接返回上次结果；对账兜底（第七课补偿页）。取舍：幂等键要全链路传递和存储，改造成本在写操作一侧，但这是重试能安全存在的前提。

## Classroom Interaction
让学员在 Demo 里验一点：停掉 mcp-knowledge-server，发"查询华为MateBook价格"，对照两处——agent-runtime 日志的 `[ErrorRecovery] 重试等待 …ms` 序列，和 `GET /api/agent/workflow/trace?traceId=` 里 `steps[].retryCount` 与事件"Step N 重试第 N 次"。要求学员指出这几次失败同时被记进了哪个熔断计数器（`recordFailure` 的 warn 日志带 `(n/5)`）。

## Take Away
重试不是一个 try-catch，是一套预算机制：每步独立计数、指数退避、成功清零、耗尽转降级；不配幂等与链路预算的重试，比不重试更危险。

## Why This Slide Matters
跳过这页，学员会把重试理解成"失败了再来一次"，然后在写操作上加自动重试——重复下单、重复扣款都从这里来；也不知道耗尽之后系统该走降级而不是把异常抛给用户。

## Transition
重试管的是一次调用失败了再试。但如果下游真的挂了，每个请求都陪跑 3 次重试就是集体浪费——这时该第二级出场：熔断。
