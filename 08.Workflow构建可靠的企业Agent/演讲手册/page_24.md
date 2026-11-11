# page_24 · 🧪 可在 Demo 验证哪些点 — 状态机 · 三级容错 · 人在回路 · 追踪

## Slide Objective
把第八课的关键断言逐条变成动手实验：Runtime 自动分档、计划先行、重试退避、熔断触发、熔断恢复、降级留痕、人在回路、现状自证。每条都给真实入口——`POST /api/agent/workflow`、`GET /api/agent/workflow/trace|status|traces|circuit-breaker`、`POST /api/agent/workflow/circuit-breaker/reset|confirm`、`ErrorRecovery` / `TraceService` / `ConfirmationService` 的日志与方法——这页是"听完"和"会做"的分界线。

## Suggested Presentation
八个实验按顺序做，每个先说验证什么、再说看哪里。**① 简单/复杂自动分档**：`POST /api/agent/workflow?message=&sessionId=&userId=` 发"推荐一台笔记本"→ 响应 `mode=simple`、`stepCount=1`；发"帮我对比游戏本和轻薄本的区别"→ `mode=workflow`、`stepCount≥2`——触发条件是 `TaskPlanner.needsPlanning()` 的确定性关键词（对比/比较/区别/分析…或 ≥2 个商品名），前端不判断复杂度。**② 计划先行**：复杂任务执行前，规划出的步骤就全部登记进 trace（`traceService.recordStepCreated`），`GET /api/agent/workflow/trace?traceId=` 里每步带 `llmThought / toolName / toolInput / toolOutput / durationMs`，出问题可逐步回溯。**③ 重试与指数退避**：停掉 mcp-knowledge-server 再提问，日志 `[ErrorRecovery] 重试等待 …ms (第 N 次重试)`（`getRetryDelayMs()` 的 2^n×1000），trace 事件"Step N 重试第 N 次: …"、`steps[].retryCount` 递增。

**④ 熔断触发**：连续触发 5 次失败，日志 `⚡ 工具 search_knowledge 触发熔断！暂停 60 秒`（`CIRCUIT_BREAKER_THRESHOLD=5`）；`GET /api/agent/workflow/circuit-breaker` 看到 `{failureCount:5, circuitBroken:true, lastError:…}`，之后的调用直接跳过。**⑤ 熔断恢复 + 手动复位**：60 秒后再请求，日志 `[ErrorRecovery] 工具 search_knowledge 熔断恢复，重新尝试`，计数清零放行——注意这是简化半开（无探测请求）；不想等就 `POST /api/agent/workflow/circuit-breaker/reset`。**⑥ 降级有回答 + 留痕**：熔断后页面仍给回答（不是报错）；trace events 有"…降级…"，对应 step 为 `SKIPPED / FAILED`；回答里能看到 `getDegradationMessage()` 的系统提示"工具 X 当前不可用…请基于已有信息回答用户"。

**⑦ 人在回路**：说"我要买拯救者"，响应 `status=confirming / confirmRequired=true / pendingTool=place_order`（`ConfirmationService.HIGH_RISK_KEYWORDS` 命中）；`POST /api/agent/workflow/confirm?traceId=&confirmed=true|false` 才继续/取消（`WorkflowStep.Status.WAITING_CONFIRM`，trace 事件"Step N 等待用户确认"）。**⑧ 现状自证**：重启 agent-runtime 再查 `GET /api/agent/workflow/trace` → `not_found`——`TraceService` 是内存 `ConcurrentHashMap`，审计只到日志级；确认后执行的下单**不再走**重试/熔断（`resumeAfterConfirm()` 直接 `callToolDirectly()`）；连点两次下单两个 `ORD-` 时间戳（`callToolDirectly()` 的模拟分支）= 无幂等键；trace 的 state 里找不到 `RETRYING`——`WorkflowState` 声明了但链路没用，重试只体现在 `retryCount` 与事件。

讲授要点：每个实验先让学员猜会发生什么再动手，猜错的点就是认知缺口。③④⑤⑥ 是三级容错的完整闭环，⑦⑧ 把"做了什么"和"没做什么"一次摊开。

## Key Emphasis
必须讲清：⑧ 是本页的灵魂——同一套操作既验证了 Demo 做了什么（分档/计划/重试/熔断/降级/确认/追踪），也自证了 Demo 没做什么（持久化审计、幂等、确认路径的容错覆盖）。不要展开环境搭建细节（端口、启动顺序）——那是 `Demo演示步骤.md` 的事，照着做就行。

## Optional Extension
**学员问：这些验证点跑完都过了，是不是说明 Demo 可以上生产？**
答：恰恰相反，⑧ 跑完你就拿到了"不能上生产"的证据清单。
Demo 做法：⑧ 的四个现场（Trace 重启即丢、下单无幂等、确认路径绕过容错、RETRYING 声明未用）各自可复现，让学员把这四条写进自己的整改清单。
生产做法：审计落库（接 OpenTelemetry/Jaeger，page_23 差距第一、二格）、写操作幂等键（第七课）、确认路径统一走 `executeToolCall()` 的容错链 + 确认超时 5 分钟自动取消（page_23"确认超时 + 并发控制"格）；补完用同一组实验回归。取舍：Demo 的价值是把缺口变成可复现的实验，不是把缺口留着上生产。

**学员问：这八个点怎么变成可以反复跑的验收，而不是课堂手点一遍？**
答：把实验升级成断言——每条验证点就是一个验收标准。
Demo 做法：手点 + 记录预期日志/响应字段，作为课程验收清单（学员交实验记录）。
生产做法：写成自动化——集成测试断言 `mode`/`stepCount`/`retryCount`，故障注入（停依赖容器）跑重试/熔断/降级的混沌用例，`circuit-breaker` 端点的指标接进监控；第九课用 AgentScope Java 2.0 重构后，同一组断言再跑一遍，对比框架接管了哪些容错能力、哪些还得自己写。取舍：自动化前期慢，但这八个点就是这套 Agent 的行为契约，改一处代码跑一遍才知道有没有改坏。

## Classroom Interaction
分组认领实验：每组挑一个（建议 ③④⑤ 分给基础好的组，⑧ 全班一起做），先预测现象、再动手、最后对着代码指认"是哪几行让这个现象发生"（如 ③ 的 `ErrorRecovery.getRetryDelayMs()`、⑦ 的 `HIGH_RISK_KEYWORDS`、⑧ 的 `resumeAfterConfirm()`）。每组用一句话回答"这个实验验证的工程结论是什么"。

## Take Away
八个验证点跑完，Demo 的能力边界和治理缺口都是亲手确认的：能力来自确定性分档 + 计划先行 + 三级容错 + 人在回路 + 全链路 trace，缺口是审计持久化、幂等、确认路径容错覆盖、确认超时四格——下次评估任何 Agent 项目，先跑这八个实验再听汇报。

## Why This Slide Matters
跳过这页，学员对 Demo 的认知停在"课件说它能"：既不会用可复现的实验验证一个 Agent 系统的真实行为（停服务看降级、重启看持久性、连点看幂等），也拿不出治理缺口的现场证据——上生产评审时只能被评审拿证据问倒。

## Transition
验证点跑通了，代码也按阅读路线读完了。拉远看全局——第八课的能力地图和 Agent 构建进度，确认自己走到哪、还差什么。
