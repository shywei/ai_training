## Slide Objective
讲解可观测性——TraceService 如何记录 Agent 的每一步执行、分布式 OpenTelemetry 接入，以及基于 Trace 的 Agent 系统级三维评估指标（Agent Evals，与第六课 RAGAS 呼应）。

## Suggested Presentation
左边讲 TraceService 的职责。记录 Agent 每一步的完整执行链路：思考内容、工具调用、入参出参、耗时、重试次数、状态变更。Tracing 是调试、审计、优化与量化评估（Agent Evals）的基础。

下面展示 Trace 输出示例。Trace #trace-a1b2c3，总耗时 4.2 秒。Plan: 3 步，耗时 0.8 秒。Step 1: search_knowledge("拯救者") → 成功，1.1 秒。Step 2: search_knowledge("MacBook") → 成功，0.9 秒。Step 3: LLM 综合分析 → 完成，1.4 秒。Status: COMPLETED。

注意 Trace 记录的粒度——每步都有 action（做了什么）、status（成功/失败）、durationMs（耗时）、retryCount（重试次数）。THINK 阶段的 LLM 思考内容也记录在 llmThought 字段里。这意味着你可以完整回溯 Agent 的执行过程——它为什么调了这个 Tool、参数是什么、返回了什么、花了多久。

右边列出 API 端点和 Tracing 的四个价值。GET /workflow/status 查状态；GET /workflow/trace 查完整链路；GET /workflow/traces 查最近 50 条 Trace 列表。四个价值：调试、审计、优化、回放。

分布式 Trace 的接入，按三格讲透：
- 是什么问题：TraceService 用 ConcurrentHashMap 存 Trace，traceId 是自己生成的 "trace-xxx"，没接任何标准协议。
- 不解决会怎样：重启全丢；多实例部署时 A 实例的链路在 B 实例查不到；跨服务调用（Agent→MCP Server）的链路拼不起来，线上事故只能靠猜。
- 怎么解决（含取舍）：四步——① 引 opentelemetry-api/sdk + otlp-exporter（要零代码就挂 javaagent，但埋点只到方法级）；② 在 TraceService.createTrace() 处建 root span，把 traceId 对齐 W3C traceparent，跨服务自动透传；③ 现有埋点直接映射——recordStepStart/recordStepEnd 变每步一个 span，recordRetry/recordEvent/recordThought 变 span event，recordStateChange 变 span attribute；④ OTLP 导出到 collector → Jaeger，头部采样 10% + 失败链路全量。取舍：手动埋点要改 TraceService 但精确到 step；采样降存储成本，偶发问题靠"失败全量"兜底；Jaeger 保留 7 天，审计级留存落对象存储。

更进一步，有了结构化 Trace，我们就能建立**Agent 系统级三维评估指标（Agent Evals，与第六课讲的 RAG 评估指标 RAGAS 形成完整闭环）**：
1. **① 任务完成率（Task Success / Pass@k）**：评估 Agent 端到端是否完成了用户的业务目标，以及在同一任务跑 k 次时的稳定性（`Pass@k` 与 `Pass^k`）。
2. **② 工具调用准确率（Tool Selection & Argument Accuracy）**：评估 Agent 是否选对了工具（Tool Selection），以及传入的参数是否符合 Schema 与业务意图（Argument Accuracy）。
3. **③ 轨迹效率（Trajectory Efficiency）**：评估 Agent 解决问题的路径质量——包括**步数冗余度**（实际执行步数 vs 黄金标准最优步数，有没有原地打转或无效调用）、**Token 消耗**与**端到端延迟**。

我在做 Demo 时，Trace 帮了大忙。有一次 Agent 突然不调工具了，我打开 Trace 一看——THINK 阶段的 llmThought 显示"用户问的是天气，不是购物问题，不需要调用工具"。原来是测试用例写错了。没有 Trace 的话，我根本不知道发生了什么。

## Key Emphasis
没有 Tracing，Agent 就是黑盒。有了 Tracing，每一步都透明，不仅能支撑线上排障，还能沉淀为 **Agent Evals 三维评估体系（任务完成率 Pass@k、工具调用准确率、轨迹效率）**，与第六课的 RAGAS 检索生成评估首尾呼应。

Trace 的粒度很重要——不是记录"调了什么工具"，而是记录"为什么调、参数是什么、返回了什么、花了多久"。

## Optional Extension
**学员问：Trace 存在哪里？分布式 Trace 怎么接？**
答：拆"存哪"和"怎么接"两层。
**Demo 做法**：TraceService 用 ConcurrentHashMap 存内存，traceId 是自造的 "trace-xxx"，重启丢失、多实例不共享——单实例教学够用。
**生产做法**：四步接 OpenTelemetry——① 引 opentelemetry-sdk + OTLP exporter（或挂 javaagent，零代码但埋点只到方法级）；② 在 TraceService.createTrace() 处建 root span，traceId 对齐 W3C traceparent，跨服务（Agent→MCP Server）透传；③ 现有埋点映射：recordStepStart/recordStepEnd 变每步一个 span，recordRetry/recordEvent 变 span event，recordStateChange 变 span attribute；④ OTLP → collector → Jaeger，采样 10% + 失败链路全量。取舍：手动埋点改 TraceService 精确到 step；采样省存储但偶发问题靠失败全量兜底。

**学员问：第六课讲了用 RAGAS 评估 RAG，那多步 Agent 怎么自动化评估？**
答：就靠这页讲的 **Agent Evals 三维评估指标**！把线上收集的高价值 `WorkflowTrace` 转化为评测基准集（Golden Dataset），每次升级 Prompt 或切换模型时，离线跑评测集统计三项核心指标：① **任务完成率（Task Success / Pass@k）**；② **工具调用准确率（Tool Selection & Argument Accuracy）**；③ **轨迹效率（Trajectory Efficiency：步数冗余度、Token 消耗与端到端延迟）**。

**学员问：Trace 和日志（Log）有什么区别？**
答：日志是"发生了什么"，Trace 是"为什么发生"。日志记录事件（"调用了 search_knowledge"），Trace 记录链路（"用户问了 X → THINK 决定调 Y → 调用 Z → 返回 W → THINK 决定回答"）。Trace 是结构化的、可查询的、有因果关系的。

## Take Away
Trace 埋点的最低标准：每次 LLM 调用、每次工具调用、每次状态变更都有带 traceId 的记录——在此之上构建 **任务完成率（Pass@k）、工具调用准确率、轨迹效率** 三维 Agent Evals 评估体系。

## Why This Slide Matters
跳过这页，学员会拿散落的 log 当追踪，事故时按文本搜日志拼链路——多实例部署下根本拼不出来，更无法量化评估 Agent 升级到底是变好了还是退化了。

## Transition
现在我们已经讲完了状态机、Runtime、容错、人在回路与可观测性。接下来进入第七章，先看贯穿全课的 context 在 v5.0 多轮循环中如何演进。