## Slide Objective
展示 THINK→ACT→OBSERVE 循环的实际执行过程和代码实现，澄清“为什么本课 Demo 用 `TOOL_CALL:` 文本协议，而生产级白盒 Runtime 严禁正则解析、必须用原生结构化 `tool_calls` + 手动控循环”。

## Suggested Presentation
左边是一个完整的执行例子。用户说"帮我对比拯救者和 MacBook 的价格"。

THINK 1："需要查拯救者价格" → ACT 1：调用 search_knowledge("拯救者笔记本") → OBSERVE 1：拯救者 Y9000P ¥8999, i7/16GB/512GB。

THINK 2："还需要查 MacBook" → ACT 2：调用 search_knowledge("MacBook Pro") → OBSERVE 2：MacBook Pro 14 ¥14999, M3/18GB/512GB。

THINK 3："信息足够，直接回答" → 最终回复："拯救者 ¥8999 更便宜..."。

注意——LLM 在 THINK 3 中决定"不需要再调工具了，直接回答"。这就是 ReAct 循环的退出条件：LLM 没有发起新的工具调用时，退出循环输出最终回复。

接下来必须给大家做一个**极其重要的工程架构澄清**——为什么本课 Demo 用 `TOOL_CALL:工具名(参数)` 文本协议，而生产级白盒 Runtime 到底怎么做？分三层讲透：
1. **为什么第七课看起来像"黑盒"？** 因为第七课开启了 Spring AI 默认的自动工具代理循环，框架在底层自动完成了 `tool_calls → 执行 → 回传`，外部看不到中间每一步状态，也没法在工具执行前插手做高危拦截、状态机流转或自定义重试熔断。
2. **为什么本课 Demo 用 `TOOL_CALL:工具名(参数)` 文本协议？** 纯粹为了**教学直观（零框架黑魔法）**，让你在控制台日志和代码里一眼看清 `THINK → ACT → OBSERVE` 的字符串流转。
3. **⚠️ 生产级白盒 Agent Runtime 标准做法**：生产环境**严禁使用文本正则解析工具调用**！一旦遇到嵌套 JSON 参数、换行符、特殊字符或者多工具并行调用（Parallel Tool Calls），正则解析必崩！正确做法是：**仍然使用 OpenAI 原生结构化 `tools / tool_calls` 协议，但关闭框架自动内部循环（例如 Spring AI 设置 `internalToolExecutionEnabled(false)`）**，由我们自己的 `AgentRuntime` `while` 状态机循环拦截模型返回的 `ToolCall` 结构化数组，依次经过 **状态机流转 → `ConfirmationService` 高危拦截 → `ErrorRecovery` 重试/熔断 → 执行并回传 `role: "tool"` 消息**！第九课用框架重构时，我们将正式切换为此生产级模式。

右边是 AgentRuntime.java 的核心代码。while 循环控制最大轮次。THINK 阶段调用 LLM。提取工具调用请求后，如果有工具调用 → 进入 ACT（串联 ConfirmationService 和 ErrorRecovery）+ OBSERVE；如果没有 → 直接回答，退出循环。

## Key Emphasis
必须帮学员把“工具调用协议（结构化 `tool_calls` vs 文本 `TOOL_CALL:`）”和“循环控制权（框架自动黑盒循环 vs 业务状态机白盒接管）”这两件事彻底解耦！

生产环境要的是：**原生结构化 `tool_calls` 协议的可靠性 + 关闭框架自动循环后的状态机白盒控制权**，绝不是在生产里手写正则解析 `TOOL_CALL:`！

## Optional Extension
**学员问：extractToolCall() 的正则在生产环境能用吗？**
答：绝对不能在生产环境用！LLM 输出稍有格式变体、嵌套 JSON 引号、多参数或者并行调用多个工具，正则就会解析失败甚至截断报错。**Demo 做法**：用 `TOOL_CALL:工具名(参数)` + 正则提取，纯粹是为了零依赖教学演示，让你看清字符串怎么流转。**生产做法**：开启原生 Function Calling（`tools` 参数），同时设置 `internalToolExecutionEnabled(false)` 关闭 Spring AI 的黑盒自动循环，在 `AgentRuntime` 的 `while` 循环里直接拿结构化的 `List<ToolCall>`，既保留 100% 协议级可靠性，又拥有 100% 白盒状态机控制权。

**学员问：THINK 阶段的输出会展示给用户吗？**
答：默认不把原始内部推理暴露给普通用户，但会记录在 `TraceService` 里用于调试与审计；在现代流式 UI（如第七课讲的 AG-UI / Agentic SSE）中，可以通过独立的 `thinking_delta` 事件渲染为前端可折叠的"思考过程"卡片。

## Take Away
生产级白盒 Agent Runtime = **OpenAI 原生结构化 `tools / tool_calls` 协议 + 关闭框架内部自动循环（`internalToolExecutionEnabled(false)`）+ 自己的状态机接管拦截/容错/回传**，严禁在生产环境用正则解析 `TOOL_CALL:` 文本。

## Why This Slide Matters
如果不把"教学用的文本协议"和"生产级的结构化 tool_calls 手动循环"讲清楚，学员回公司真用正则解析 `TOOL_CALL:` 上生产，遇到复杂 JSON 参数或多工具调用会直接引发线上解析崩溃。

## Transition
Runtime 的六个组件中，还有一个重要的能力——模型路由。