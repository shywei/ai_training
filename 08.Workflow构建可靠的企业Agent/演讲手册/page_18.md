## Slide Objective
展示 Demo 知识体系覆盖情况——哪些学员可以动手验证，哪些只需知道概念和方向。

## Suggested Presentation
左边是 Demo 已覆盖的 6 个核心能力。状态机——7 个状态 + 转换条件，WorkflowState.java。ReAct 循环引擎——THINK→ACT→OBSERVE，最多 10 轮，AgentRuntime.java。TaskPlanner——LLM 自动拆步骤，TaskPlanner.java。三级容错——重试 3 次指数退避 + 熔断 + 降级，ErrorRecovery.java。人在回路——高风险操作确认/取消，ConfirmationService.java。执行追踪——Trace + 链路回溯，TraceService.java。这 6 个能力学员都可以本地跑起来、验证、修改、扩展。

右边是仅 PPT 讲解的 5 个能力。持久化 Checkpoint——状态存数据库，重启后恢复。Demo 只做了 CONFIRMING 暂停 + 确认后 Resume，没有序列化完整状态。分布式 Tracing——接 OpenTelemetry，支持多实例链路追踪。Demo 用内存 Map 存 Trace。条件编排——分支、并行、子 Workflow。Demo 只有顺序编排。Multi-Agent——多 Agent 协作。Demo 是单 Agent。分级模型路由——按任务类型选模型。Demo 用统一模型。

底部是关键认知：知识体系不能有缺口。Demo 覆盖核心路径，PPT 讲清楚"如果要落地，大概怎么做"。学员学完后知道四件事：① 原理 ② 能看懂代码 ③ 知道差距 ④ 知道从哪里开始补。

为什么要分"已覆盖"和"仅 PPT"？因为如果不分，学员会以为所有内容都在 Demo 里实现了——然后发现做不到，产生挫败感。分了之后，学员知道哪些可以动手验证，哪些只需要理解概念——学习目标更清晰。

## Key Emphasis
"Demo 已覆盖"和"仅 PPT 讲解"的区分是本页的核心。不要让学员产生误解——以为所有内容都是可以跑的代码。

底部四个学习目标要逐一讲清楚——学员需要知道自己学完后能达到什么水平。

## Optional Extension
**学员问：为什么 Multi-Agent 不做在 Demo 里？**
答：Multi-Agent 需要多个 Agent 实例 + Agent 间通信 + 任务分配，工程量太大。第八课聚焦"单 Agent 可靠运行"，Multi-Agent 是第十一课的内容。先让一个 Agent 可靠运行，再考虑多个 Agent 协作。

**学员问：条件编排自己实现难吗？**
答：不难，但需要理解 Workflow Engine 的概念。简单版：在 WorkflowStep 里加 condition 字段（如 "stock > 0"），AgentRuntime 根据条件决定下一步走哪条 Edge。但复杂的条件编排（并行、子图、动态边）建议用 LangGraph 或 Temporal。

## Take Away
Demo 覆盖核心路径，PPT 补齐知识缺口。学员知道：原理、代码、差距、从哪里开始补。

## Why This Slide Matters
如果跳过这页，学员会把"仅 PPT 讲解"的内容也当成 Demo 可验证的内容——导致学习目标混乱。

## Transition
接下来是项目架构和代码阅读路线图。