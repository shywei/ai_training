## Slide Objective
展示项目架构、代码阅读路线图和 v5.0 新增/修改文件，帮助学员快速上手代码。

## Suggested Presentation
左边是代码阅读路线图。按这个顺序读，10 分钟就能理解整个 Demo 的架构。

① WorkflowController.java — API 入口。看它有哪些端点：POST /workflow 执行工作流，POST /workflow/stream SSE 流式，POST /workflow/confirm 确认/取消。响应里有 mode 字段：simple 或 workflow——简单任务不走完整工作流。

② AgentRuntime.java — ReAct 循环引擎（核心）。这是最重要的文件，651 行。先看 execute() 方法的整体流程——构建 context → TaskPlanner → ReAct 循环 → 输出结果。再看 executeAgentLoop() 的循环逻辑——THINK→ACT→OBSERVE。最后看 handleConfirmation() 的暂停/恢复逻辑。

③ TaskPlanner.java — 任务规划。看 plan() 方法的 Prompt 设计——怎么让 LLM 输出 JSON 格式的步骤（注意解析要多兕底：LLM 常丢外层数组）。看 needsPlanning() 的判断逻辑——关键词 + 商品名数量；生产做法是规则快路径 + 快模型兜底分类。

④ WorkflowState.java + WorkflowStep.java — 状态机。看枚举定义和转换规则。WorkflowStep 是一个步骤的完整描述——序号、描述、工具、参数、状态、重试次数。

⑤ ErrorRecovery.java — 容错。看 shouldRetry()、getRetryDelayMs()、isCircuitBroken() 三个方法——对应三级容错。

⑥ ConfirmationService.java — 人在回路。看 isHighRisk() 的风险判断逻辑，以及 requestConfirmation() / confirm() 的暂停/恢复流程。

⑦ TraceService.java — 执行追踪。看 addStep() 和 complete() 怎么记录执行链路。

右边列出 v5.0 新增/修改的文件。新增 7 个文件，共 1761 行新代码。AgentRuntime.java 651 行是核心——ReAct 循环 + 状态机 + Tool 执行。WorkflowController.java 275 行——API 入口 + SSE。TaskPlanner.java 189 行——LLM 任务规划。TraceService.java 197 行——执行追踪。ConfirmationService.java 159 行——人在回路。ErrorRecovery.java 151 行——三级容错。WorkflowState.java + WorkflowStep.java——状态机。

修改了 McpProductClient.java——加了 callToolDirectly() 方法。这是为了解决一个技术问题：Spring AI 的 Function Calling 是框架自动处理的，AgentRuntime 需要自己控制工具调用，所以加了一个直接调用 MCP Server 的方法。

MCP Server 层完全没有变——chat-mcp-service 和 business-mcp-service 的代码零修改。/chat 已删除（被 /api/agent/workflow 替代）。

## Key Emphasis
代码阅读路线图是本页的核心价值——学员课后上手代码的"地图"。按顺序读，从入口到核心到组件。

1761 行新代码听起来很多，但拆成 7 个文件每个都不大。最大的 AgentRuntime 651 行，是因为它包含了完整的 ReAct 循环 + 状态机 + Tool 执行逻辑。

## Optional Extension
**学员问：为什么 MCP Server 层没变？**
答：因为第七课的 MCP 架构设计是正确的——统一数据访问层。第八课只改 Agent 层，MCP Server 层保持稳定。这就是分层架构的好处——下层稳定，上层迭代。

**学员问：callToolDirectly() 和 Spring AI 的 Function Calling 有什么区别？**
答：Function Calling 是"LLM 说要调什么，框架自动执行"——你控制不了执行过程。callToolDirectly() 是"AgentRuntime 决定调什么，自己执行"——你能控制重试、熔断、确认。手搓 ReAct 需要后者。

## Take Away
1761 行新代码，7 个文件，10 分钟阅读路线。MCP Server 层不变，分层架构的好处。

## Why This Slide Matters
如果跳过这页，学员拿到代码不知道从哪里看起。代码阅读路线图是"上手"的桥梁。

## Transition
接下来讲 Demo vs 生产的具体差距。