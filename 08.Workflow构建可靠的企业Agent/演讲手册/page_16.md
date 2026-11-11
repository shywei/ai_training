# page_16 · 三级容错② 熔断 — 触发条件 · 参数取值 · 失败模式

## Slide Objective
讲透熔断的三格：触发条件（按工具名连续失败 5 次，`ErrorRecovery.recordFailure()` 计数 + `isCircuitBroken()` 判定）、参数取值（阈值 5、恢复窗口 `CIRCUIT_BREAKER_RECOVERY_MS=60_000`、简化半开的恢复动作）、失败模式（到点放行无探测、进程内状态不共享、连续计数被一次成功清零）。对应 Demo 的 `ErrorRecovery.java` 与 `AgentRuntime.executeToolCall()` 入口的熔断检查。

## Suggested Presentation
先讲熔断在链路上的位置：`executeToolCall()` 的**第一件事**就是 `isCircuitBroken(toolName)`——熔断打开时根本不发请求，`step.skip("工具已熔断，降级为跳过")`，直接返回降级消息。这和重试的关系要讲清：重试的每次失败都调 `recordFailure()`，所以熔断计数是重试失败的累积；熔断一旦打开，重试循环压根没机会跑——**两级是串联短路，不是并联**。

触发条件拆三个细节。第一，**计数维度是工具名**：`ConcurrentHashMap<String, ToolFailureInfo>`，`search_knowledge` 挂了不影响 `queryProductInfo`。第二，**阈值是"连续"失败 5 次**：`recordSuccess()` 一次就把整桶清零，所以 9 次失败夹 1 次成功的慢故障永远熔不掉——这是"连续计数"这个简化设计的固有盲区。第三，**每次重试失败都计数**，一次工具调用带 3 次失败就能把计数推到 3，两次调用就足以触发熔断。

参数取值讲两个数。**阈值 5**：低于它，网络抖动会造成误熔断；高于它，宕机的下游已经被多打了好几轮。**恢复窗口 60 秒**：从最近一次失败起算（`lastFailureTime`），给下游重启留时间（实测大多数服务重启在 30 秒内）。恢复动作在 `isCircuitBroken()` 里做——这是 Demo 的**简化半开**：elapsed ≥ 60s 就把 `failureCount` 清零、放行，日志打"熔断恢复，重新尝试"。

失败模式重点讲半开的简化代价。**没有探测请求**：恢复点一到，全部流量直接放过去，下游没恢复就立刻再攒 5 次失败、再次熔断——开/关震荡。**状态在进程内存**：起两个 agent-runtime 实例，各熔断各的，宕机的下游仍被另一半流量打死。这就是 page_23 差距里"半开状态探测"那一格的来处——生产用显式三态（closed/open/half-open），半开只放 N 个探测请求，成功才闭合，Resilience4j CircuitBreaker / Sentinel 都是现成实现。

设计弯路：Demo 选择"到点清零放行"不是不知道半开，而是**课堂可复现优先**——等 60 秒就能看到恢复日志，比引入探测请求的状态分支好演示。但要把简化和正解分清楚：简化只该活在 Demo 里。

## Key Emphasis
必须讲清：熔断检查在调用入口、短路整个调用（含重试）；计数按工具名、连续失败语义、成功清零；恢复是"到点清零放行"的简化半开。

不要展开：Resilience4j/Sentinel 的配置细节（滑动窗口大小、慢调用比例）——点名工具和三态模型即可，展开留生产培训。

## Optional Extension
**学员问：熔断后直接放行全部请求不是更简单吗，为什么还要半开探测？**
答：差别在恢复瞬间的流量形态——放行全部等于拿生产流量当探测。
Demo 做法：60 秒到点 `isCircuitBroken()` 清零放行，日志"熔断恢复，重新尝试"，课堂一条命令能演；`POST /api/agent/workflow/circuit-breaker/reset` 还能手动复位，不用等 60 秒。
生产做法：显式半开态，只放 N 个探测请求（或按比例放 5%），探测成功才闭合、失败立刻回到 open 且窗口翻倍；多实例用 Redis 共享熔断状态，或每实例独立熔断 + 网关全局限流兜底。取舍：探测期少量请求仍会失败，这是恢复的必要成本；分布式状态多一次 Redis 往返和一致性问题，但换来全局限流语义。

**学员问：为什么熔断按工具名分桶，不按用户或按下游服务分？**
答：分桶维度决定保护粒度——Demo 按工具名，是"一个 MCP Tool 一个下游依赖"的最小模型。
Demo 做法：`failureCounters` 的 key 就是 toolName，`GET /api/agent/workflow/circuit-breaker` 按工具列出 `failureCount / lastError / circuitBroken`，停掉 knowledge-server 只看到 `search_knowledge` 一个桶亮红。
生产做法：至少两层——按下游服务（保护依赖）+ 按调用方（防单个租户打爆全局）；错误分类计数（超时/限流/业务错分开），业务错不该触发熔断。取舍：桶越多状态越多、误熔断面越大，从"依赖级"起步，被哪类问题咬了再细分。

## Classroom Interaction
让学员在 Demo 里验一点：不停服务，直接 `GET /api/agent/workflow/circuit-breaker` 看空状态；然后停掉 mcp-knowledge-server，连发 3~4 个查询触发失败累积，边发边刷这个端点，看 `failureCount` 逼近 5、`circuitBroken` 翻成 true、下一次请求的 trace 里出现"降级: 工具 … 已熔断"。最后 `POST /api/agent/workflow/circuit-breaker/reset` 复位。

## Take Away
熔断的工程判断就三条：按什么维度计数、连续失败还是滑动窗口、恢复是放行全部还是先放探测——Demo 三个答案都是简化版，上生产前逐条换成显式三态 + 分布式状态。

## Why This Slide Matters
跳过这页，学员不知道熔断在调用链的哪一层短路（入口，连重试都不给跑），也不知道 Demo 的"60 秒恢复"是简化半开——直接照抄到生产，恢复瞬间就是一次流量惊群。

## Transition
熔断打开后工具被跳过，但用户的问题还在——不能回一句"系统错误"了事。第三级降级接管：怎么让 LLM 在工具缺席的情况下给出不编造的回答。
