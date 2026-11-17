# 第 10 页：同构提效（六）：AgentScope ReActAgent 接管多轮主循环

## Slide Objective
通过五步闭环讲透 `ReActAgent` 如何接管 V5（651 行）和 V6（300 行）仍在手写的多轮 ReAct 状态推进循环，展示 `AgentScopeConfig` 装配与 `AgentScopeWorkflowAdapter` 一行委托调用（Demo 已覆盖）。

## Suggested Presentation
各位同学，刚才看到 V6 里 `SpringAi2WorkflowAdapter` 依然要手写 300 行循环。现在我们切到 8084 端口的 V7 工程（`enterprise-ai-agent-agentscope`），看看引入 **AgentScope 2.0** 之后，同样的导购主循环是怎么写的！

同样按五步逻辑走一遍：
1. **遇到什么问题？** V5 的 `AgentRuntime`（651 行）和 V6 的 `SpringAi2WorkflowAdapter`（300 行）都把精力耗在了手写 `while` 循环、THINK-ACT-OBSERVE 推进与最大步数保护上；
2. **引入什么概念？** AgentScope 2.0 的核心智能体内核 —— **`ReActAgent`**；
3. **底层原理是什么？** `ReActAgent` 把“模型推理 ➔ 解析工具调用 ➔ 执行工具 ➔ 观察结果 ➔ 判断是否继续下一轮”封装成了内核内置的状态循环引擎，并内置 `maxIters` 迭代上限防死循环，外围挂载 `Toolkit` 工具集、五阶段 `Middleware` 钩子和 `AgentStateStore`；
4. **对应到框架怎么配、怎么调？**
   - **左栏配置期（`AgentScopeConfig.java`）**：先构建兼容 OpenAI 协议（直连 SiliconFlow）的 `OpenAIChatModel`，把 `@Tool` 工具类 `ShoppingAgentTools` 注册进 `Toolkit`，然后用 `ReActAgent.builder()` 一把组装起来，并挂上 `middlewares` 和 `stateStore`；
   - **右栏运行期（`AgentScopeWorkflowAdapter.java`）**：构造本次请求的隔离上下文 `RuntimeContext`，然后只需要一行代码：`Msg responseMsg = shoppingAgent.call(message, context).block();`——**原来 651 行 / 300 行的 `while` 状态循环全部交给了 `ReActAgent` 内核**，整个 Adapter 核心编排浓缩到约 110 行！

## Key Emphasis
1. **对比明确**：`ChatClient` 是单次请求客户端；`ReActAgent` 是把“循环引擎 + 工具路由 + 中间件 + 状态存储”打包的高阶运行时。
2. **同构定性**：这里实现的依然是导购 Agent 的 ReAct 推理与工具调用循环（08 课已有原理），属于框架托管循环带来的效率跃迁。

## Optional Extension
### Q1: 在 `AgentScopeConfig.java` 中，`OpenAIChatModel` 可以直接接入本地 Ollama 或 DeepSeek 吗？
**讲师解析**：完全可以。只要目标推理服务暴露标准 OpenAI 兼容协议接口（`/v1/chat/completions`），只需修改 `application.yml` 中的 `agentscope.model.base-url`、`api-key` 和 `model-name`，无需改动任何 Java 代码即可无缝切换模型后端。

## Classroom Interaction
“对比 V5 的 651 行 `AgentRuntime`、V6 的 300 行 `SpringAi2WorkflowAdapter` 和 V7 的 110 行 `AgentScopeWorkflowAdapter`，多出来的这几百行代码去哪儿了？”（答案：被 `ReActAgent` 内核吸收了）。

## Take Away
`ReActAgent` 将多轮 THINK-ACT-OBSERVE 循环、工具调度与迭代上限收敛为内核能力，业务 Adapter 只需一行 `call()` 委托执行。

## Why This Slide Matters
完成了 `V5 ➔ V6 ➔ V7` 同构能力演进的最后一跳，让学员亲眼看到高阶应用框架如何消灭循环编排胶水。

## Transition
到这里，我们第一部分“同构功能提效（V5 ➔ V6 ➔ V7）”已经全部看清。下面用 P11 一张表做总收口，同时回答一个关键问题：除了写得更短，企业级 Agent 还有哪些“V5 根本做不好、必须靠框架提供”的能力？请看 P11！
