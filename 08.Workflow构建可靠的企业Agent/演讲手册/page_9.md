## Slide Objective
拆解 Agent Runtime 的六大组件——每个组件的职责和对应的 Demo 代码。

## Suggested Presentation
左边列出六个组件。这是提纲里明确要求的——Scheduler、State Manager、Tool Executor、Memory Manager、Checkpoint、Tracing。

Scheduler（调度器）控制 ReAct 循环的启停、最大轮次、超时。对应 AgentRuntime.java 的 while 循环——iteration < MAX_ITERATIONS。看起来简单，但它是最核心的组件——没有它，循环就没有边界。

State Manager（状态管理器）管理 WorkflowState 的转换，确保合法转换。对应 WorkflowState.java 枚举。注意"确保合法转换"——不能从 COMPLETED 跳回 EXECUTING，不能从 CONFIRMING 自动跳到 EXECUTING。这些约束是用代码硬编码的。

Tool Executor（工具执行器）执行工具调用，处理重试和熔断。对应 executeToolCall() 方法 + ErrorRecovery。它是唯一和 MCP Client 打交道的组件——隔离了工具调用的复杂性。

Memory Manager（记忆管理器）构建 context、管理对话历史。对应 MemoryManager.java——复用 v4.0 的代码。这个组件没有改，因为第七课已经做好了。

Checkpoint（检查点）保存/恢复执行状态。Demo 只做了 CONFIRMING 暂停——用户确认后从暂停点继续。持久化 Checkpoint 没做，那需要序列化整个 Agent 状态。

Tracing（追踪器）记录每步执行链路。对应 TraceService.java。它用 ConcurrentHashMap 存储 Trace，生产环境应该接 OpenTelemetry。

右边展示组件协作关系。Scheduler 是核心，控制整个循环。每轮循环中：MemoryManager 构建 context → StateManager 管理状态 → LLM 思考 → ToolExecutor 执行工具（带 ErrorRecovery 和 ConfirmationService）→ TraceService 记录。

底部对比第七课和第八课。第七课：一次性调用，LLM 决定调什么 → 执行 → 返回。第八课：循环调用，Runtime 控制循环 → 多步 → 追踪 → 确认。关键区别：Runtime 管 LLM，不是 LLM 管自己。

## Key Emphasis
六个组件各有职责，协作完成"可靠执行"。不是所有组件都需要从零写——MemoryManager 复用 v4.0，这就是"逐渐进化"的设计。

Scheduler 是最核心的组件——没有它，其他组件都没有意义。它是"操作系统"的"内核"。

## Optional Extension
**学员问：这六个组件是 Agent 框架的标准组件吗？**
答：是的。LangGraph 的 Graph + State + Node 对应 Scheduler + State Manager + Tool Executor。Spring AI 的 Advisor + ChatClient 对应 Memory Manager + LLM 调用。理解了这六个组件，就理解了所有 Agent 框架的底层设计。

**学员问：Checkpoint 和 Trace 的区别是什么？**
答：Checkpoint 是"存档"——保存 Agent 的完整状态，用于恢复执行。Trace 是"日志"——记录执行过程，用于调试和审计。Checkpoint 是为了"继续"，Trace 是为了"回溯"。

## Take Away
六个组件各有职责，协作完成可靠执行。理解了它们，就理解了所有 Agent 框架的底层设计。

## Why This Slide Matters
如果跳过这页，学员只知道"Agent Runtime 是什么"，但不知道"它由什么组成"。组件化思维是工程化的基础。

## Transition
接下来讲容错——工具调用失败怎么办。