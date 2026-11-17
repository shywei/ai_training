# 第 09 页：V6 的能力天花板：API 优雅了，但多步状态机依然要手搓

## Slide Objective
通过解剖 `SpringAi2WorkflowAdapter.java`（300 行真实代码），让学员直观看到 Spring AI 2.0 作为“模型通信基础设施层（类比 Servlet API）”的能力天花板，引出引入应用级智能体运行时框架（AgentScope 2.0）的必要性。

## Suggested Presentation
各位同学，请大家直接看 8083 工程里最发人深省的一个类——`SpringAi2WorkflowAdapter.java`，整整 **300 行**！

在刚才的 P04~P08 中，Spring AI 2.0 帮我们把 Prompt 拼接、正则解析工具、MCP 握手和安全切面全接管了。按理说代码应该只剩几十行才对，为什么这个 Adapter 还是写了 300 行？请看左侧这段真实源码：
1. **步骤规划还要自己写**：`List<WorkflowStep> steps = planTask(message);`
2. **多轮状态推进循环还要自己写**：`while (cursor < steps.size() && cursor < MAX_ITERATIONS)`，每走一步手动 `cursor++`；
3. **人在回路审批中断还要自己判断、手动返回**：在 `driveWorkflowLoop` 里调完模型，还得写 `if (checkNeedsHumanConfirmation(...))` 去手动改状态为 `CONFIRMING`、手动存快照、手动提前 `return`；
4. **每步快照持久化与 SSE 事件推送还要自己穿插调用**：`checkpointStore.saveSnapshot(...)` 和 `sink.tryEmitNext(...)` 全得硬编码在循环体里。

为什么会这样？请看右侧的核心类比：**Spring AI = Servlet API**！
Servlet API 把 HTTP 请求、响应和 Filter 链抽象得极其优雅，但它从来不负责替代 Spring MVC 或工作流引擎。同理，Spring AI 2.0 刻意定位在“模型通信基础设施层”，它帮你优雅地完成**单次模型请求和单步工具循环**，但**不内置多步认知状态机、不内置执行栈快照、也不内置工具前钩子挂起**。

如果团队继续只在 Spring AI 之上做复杂长链路 Agent，每个开发都会自己写一套“土味状态机 `while` 循环”。这就是我们接下来要走上第二级阶梯——**AgentScope 2.0（Spring MVC 级应用框架）**的根本原因！

## Key Emphasis
1. **用事实说话**：指给学员看 `SpringAi2WorkflowAdapter` 真实留存的 4 块手搓编排代码。
2. **定位清晰**：Spring AI 不是“不行”，而是它刻意聚焦在基础设施层（Servlet API 定位），多步智能体运行时本就不在它的职责内。

## Optional Extension
### Q1: 既然 Spring AI `ToolCallingAdvisor` 自己也能循环调工具，为什么 `SpringAi2WorkflowAdapter` 还要在外面再包一层 `while` 循环？
**讲师解析**：因为 `ToolCallingAdvisor` 内部的循环是封闭且无状态的——一旦把工具全交给它，它就会在一次同步调用里把查商品、查库存、下单全部连续跑完，你没法在“查完库存准备下单”前暂停下来存断点、也没法把每一步的执行轨迹拆解成 `WorkflowStep` 记录下来等待次日人工审批。所以只要需要多步可控状态流转，在 Spring AI 里就只能自己在外面再包一层状态机循环。

## Classroom Interaction
“对照 P08 和 P09：在什么业务下，你根本不需要写 `SpringAi2WorkflowAdapter` 里这 300 行状态循环，直接调一行 `ChatClient` 就够了？”（答案：不需要多步规划、不需要中途人工审批挂起、不需要断点恢复的问答/RAG/简单查询场景）。

## Take Away
Spring AI 2.0 消除了“模型通信胶水”，但没有消除“多步状态编排胶水”——这就是通信底座与智能体运行时的界线。

## Why This Slide Matters
本页是第一部分从 V6 迈向 V7 的核心转折点，让学员从源码证据中彻底明白“为什么不能只停留在基础设施层”。

## Transition
既然 V6 里这 300 行编排循环依然要手搓，那换上专门的智能体应用框架 AgentScope 2.0（V7）之后，同样的导购主循环会变成什么样？请看 P10！
