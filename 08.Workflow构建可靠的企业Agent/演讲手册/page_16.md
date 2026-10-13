## Slide Objective
讲解可观测性——TraceService 如何记录 Agent 的每一步执行，以及 Tracing 的四个价值。

## Suggested Presentation
左边讲 TraceService 的职责。记录 Agent 每一步的完整执行链路：思考内容、工具调用、入参出参、耗时、重试次数、状态变更。Tracing 是调试、审计、优化的基础。

下面展示 Trace 输出示例。Trace #trace-a1b2c3，总耗时 4.2 秒。Plan: 3 步，耗时 0.8 秒。Step 1: search_knowledge("拯救者") → 成功，1.1 秒。Step 2: search_knowledge("MacBook") → 成功，0.9 秒。Step 3: LLM 综合分析 → 完成，1.4 秒。Status: COMPLETED。

注意 Trace 记录的粒度——每步都有 action（做了什么）、status（成功/失败）、durationMs（耗时）、retryCount（重试次数）。THINK 阶段的 LLM 思考内容也记录在 llmThought 字段里。这意味着你可以完整回溯 Agent 的执行过程——它为什么调了这个 Tool、参数是什么、返回了什么、花了多久。

右边列出 API 端点。GET /workflow/status 查状态——快速看一眼当前状态。GET /workflow/trace 查完整链路——调试时用这个，可以看到每步的思考过程。GET /workflow/traces 查最近 50 条 Trace 列表——浏览历史执行记录。

Tracing 的四个价值。调试：哪一步出了问题？工具返回了什么？LLM 为什么做了这个决定？审计：谁在什么时间做了什么操作？操作的结果是什么？优化：哪一步最慢？瓶颈在 LLM 还是工具调用？回放：出了 bug 可以完整复现执行过程，不需要用户再操作一次。

我在做 Demo 时，Trace 帮了大忙。有一次 Agent 突然不调工具了，我打开 Trace 一看——THINK 阶段的 llmThought 显示"用户问的是天气，不是购物问题，不需要调用工具"。原来是测试用例写错了。没有 Trace 的话，我根本不知道发生了什么。

## Key Emphasis
没有 Tracing，Agent 就是黑盒。有了 Tracing，每一步都透明。这是企业生产环境的基础设施。

Trace 的粒度很重要——不是记录"调了什么工具"，而是记录"为什么调、参数是什么、返回了什么、花了多久"。

## Optional Extension
**学员问：Trace 存在哪里？**
答：Demo 用 ConcurrentHashMap 存在内存里，重启丢失，多实例不共享。生产环境应该接 OpenTelemetry——Trace 数据写入 Jaeger 或 Zipkin，支持分布式链路追踪。

**学员问：Trace 会不会太大？**
答：会。每次执行记录完整的思考过程和工具调用，数据量很大。生产环境应该加采样——只记录 10% 的 Trace，或者只记录失败的 Trace。同时要加 TTL——7 天后自动清理。

**学员问：Trace 和日志（Log）有什么区别？**
答：日志是"发生了什么"，Trace 是"为什么发生"。日志记录事件（"调用了 search_knowledge"），Trace 记录链路（"用户问了 X → THINK 决定调 Y → 调用 Z → 返回 W → THINK 决定回答"）。Trace 是结构化的、可查询的、有因果关系的。

## Take Away
没有 Tracing，Agent 就是黑盒。Tracing 是调试、审计、优化的基础。

## Why This Slide Matters
如果跳过这页，学员以为"打日志"就够了。Tracing 和日志是两个层级的东西——日志记录事件，Tracing 记录链路。

## Transition
现在我们已经讲完了所有核心概念。接下来看 Demo 的完整架构和知识体系覆盖情况。