# 第 15 页：企业级增量（二）：为什么 Advisor 拦不住高危工具执行前挂起？

## Slide Objective
通过对比 Spring AI `Advisor` 的单次请求边界与 AgentScope `MiddlewareBase` 的五阶段认知生命周期钩子（严格对齐 `agentscope-core:2.0.0` 真实接口方法），讲透为什么“高危工具执行前 HITL 无损挂起”必须依赖认知生命周期拦截器。

## Suggested Presentation
各位同学，刚才我们看到了如何保存快照与复水。现在来解决一个紧接着的问题：**到底在代码执行流程的哪个位置，触发“挂起并保存快照”？**

假设用户说“帮我买一台 ¥8999 的拯救者”。模型经过思考后，决定调用 `createOrder` 工具。在企业生产里，我们必须在**“模型刚发出 `createOrder` 意图、但工具方法还没真正执行扣款”**的那一瞬间拦下来，保存快照并挂起等待人类确认。

在 V5 里我们怎么做的？靠硬编码的 `ConfirmationService` 字符串匹配且内存阻塞等待，非常脆弱。
那用 V6 的 Spring AI `Advisor` 能拦住吗？请看左栏——**拦不住！为什么？**
因为 Spring AI `Advisor` 围绕的是单次 `ChatModel` 请求/响应（类似 Web 里的 `Servlet Filter`）：
1. 它只看得见一次 HTTP 发出去的 Prompt 和收到的响应，根本不知道当前处于多轮 ReAct 循环的哪一步；
2. 内置 `ToolCallingAdvisor` 一收到 `tool_calls` 就会在内部循环里直接反射调工具，没留出“检查每笔工具入参并把整个智能体挂起等待次日审批”的认知切面；如果硬要在 `Advisor` 里拦，只能靠自己抛自定义异常砸断调用链，再在外部手写状态恢复。

再看右栏——**这就是 AgentScope 2.0 引入 `MiddlewareBase` 认知生命周期钩子的原因！**
我们核对过 `agentscope-core:2.0.0` 的真实源码，`MiddlewareBase` 直接在 `ReActAgent` 内部认知拓扑的 **5 个核心关节** 暴露了标准 Hook：
1. `onAgent`：智能体任务入站启动与最终收官（做租户校验、总耗时审计）；
2. `onSystemPrompt`：动态增强或注入安全系统提示词；
3. `onModelCall`：底层模型网络调用切面（这一个阶段就对应了 Spring AI Advisor 的能力范围）；
4. `onReasoning`：进入模型思考推演阶段拦截；
5. **`onActing`（最关键！）**：在工具真正被调用执行**之前**精准拦截 `ActingInput`（里面直接带着本次要调的所有 `ToolUseBlock` 和参数！），并能结合 Reactor `.onErrorResume()` 处理异常自愈。

## Key Emphasis
1. **严格对齐真实 API**：`MiddlewareBase` 的 5 个方法是 `onAgent`、`onSystemPrompt`、`onModelCall`、`onReasoning`、`onActing`（异常处理在返回的 `Flux<AgentEvent>` 上用 `.onErrorResume()` 实现）。
2. **本质差异**：`Advisor` 管“应用到模型”的通信边界；`MiddlewareBase` 管“智能体内部思考与行动”的认知拓扑。

## Optional Extension
### Q1: 在 `MiddlewareBase` 里既有 `onReasoning` 又有 `onModelCall`，两者有什么区别？
**讲师解析**：`onReasoning` 处于智能体认知循环的“思考阶段”外围（可以观察和干预整个推理上下文、思考流事件和推理轮次状态）；而 `onModelCall` 更贴近底层模型网络请求的实际发出与返回（适合做模型超时重试、备用模型路由降级与单次 Token 计量）。

## Classroom Interaction
“如果我只希望在发给大模型之前把用户手机号脱敏，应该用 `onActing` 还是 `onModelCall`（或 Spring AI `CallAdvisor`）？”（用 `onModelCall` / `CallAdvisor`，因为这是模型通信层的需求）。

## Take Away
Spring AI Advisor 是面向单次请求的 HTTP 级拦截；AgentScope MiddlewareBase 则把钩子打进了 ReAct 认知循环，让工具执行前拦截成为可能。

## Why This Slide Matters
帮学员彻底搞懂“请求拦截链”与“认知生命周期钩子”的本质区别，而不是把两个名字混为一谈。

## Transition
理解了 `MiddlewareBase` 的 5 个阶段后，我们马上看 P16 的真实代码：在 `LifecycleMiddleware.onActing()` 里，究竟是怎么几行代码实现无损挂起 HITL 的？
