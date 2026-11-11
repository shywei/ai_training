## Slide Objective
建立状态机的基本认知——什么是状态机，WorkflowState 的 7 个状态和转换关系。

## Suggested Presentation
有限状态机（FSM）是计算机科学里最基础的概念之一，但很多做 AI 的同学不熟悉。核心思想很简单：系统在任意时刻只能处于一个状态，状态之间的转换由明确的条件触发。

WorkflowState 有 7 个状态。PLANNING 是 TaskPlanner 在规划步骤——LLM 在想"这件事分几步做"。EXECUTING 是 Agent 在执行 ReAct 循环——调用工具、思考、返回结果。CONFIRMING 是等待用户确认高风险操作——工作流暂停了，Agent 只能等。RETRYING 是工具调用失败正在重试——不是立即失败，而是给它第二次机会。COMPLETED、FAILED、CANCELLED 是终态。

看右边的转换图。PLANNING → EXECUTING 是正常流转。EXECUTING → COMPLETED 是所有步骤完成。EXECUTING → CONFIRMING 是遇到高风险操作。CONFIRMING → EXECUTING 是用户确认后继续。EXECUTING → RETRYING → EXECUTING 是重试后继续。EXECUTING → FAILED 是重试耗尽。CONFIRMING → CANCELLED 是用户取消。

（Demo 现状注：`RETRYING` 在 `WorkflowState` 里声明了，但当前代码没有实际转入这个状态——重试只体现在 `WorkflowStep.retryCount` 和 trace 事件上，验证点⑧能现场自证。设计态与实现的已知差距：生产用状态机框架（如 Temporal）时再把重试状态补全。）

关键设计在底部：终态不可转换。COMPLETED、FAILED、CANCELLED 是终点，不能再变回 EXECUTING。这就像订单完成后不能再回到"待支付"。CONFIRMING 只能由用户操作触发，代码不能自动跳过——你不能让 Agent 自己决定"用户肯定同意了"。

状态机的价值是"确定性"——同样条件一定走同样路径。不像 LLM 那样"想到哪算哪"。状态机让 Agent 的行为变得可预测、可测试、可审计。

## Key Emphasis
1. **核心工程架构：概率规划 + 确定性有限状态机（FSM）约束**：
   大模型（TaskPlanner）生成的规划步骤本质上属于大模型的“认知/概率型规划（Probabilistic Planning）”，其输出天然存在不确定性、幻觉与因果倒置风险。企业级系统决不能让模型直接操控全局控制流，必须由确定性状态机（FSM）作为刚性控制骨架，负责校验拓扑依赖、前置拦截与强转移规则，将不可控的模型概率输出死死收敛到企业受控状态流中。
2. 7 个受控状态要逐一讲清楚每个的含义。特别是 CONFIRMING——这是"人在回路"的安全挂起点。
3. 终态不可逆（COMPLETED、FAILED、CANCELLED）和 CONFIRMING 只能由用户显式触发，这两个约束是状态机工程安全底线，一定要重点强调。

## Optional Extension
**学员问：为什么不直接用 Java 的 if-else，要搞状态机？**
答：if-else 能做，但状态机有三个优势。第一，状态转换可以独立测试——每个 Edge 可以单独验证。第二，状态可以持久化——存到数据库里，重启后能恢复。第三，状态变更可以记录在 Trace 里——出错时知道 Agent 在哪个状态。如果用 if-else，这些都要自己写。**Demo 做法**：WorkflowState 枚举 + 代码硬编码转换（isTerminal()/canConfirm() 约束合法转换）；**生产做法**：再加状态持久化——workflow_checkpoint 表按 traceId 存 state，重启可恢复（表结构在"Demo vs 生产"那页展开）。

**学员问：Spring StateMachine 框架能用吗？**
答：能，但 Demo 没用。7 个状态用枚举 + 代码判断更直观，学员容易理解。状态多了（20+）或者转换条件复杂了，再考虑用框架。

## Take Away
动手先画状态转换表（每个状态 × 谁触发 × 什么条件）——这张表既是代码骨架，也是测试用例清单。

## Why This Slide Matters
跳过这页，学员会用 while(true)+if-else 写循环——COMPLETED 能被改回 EXECUTING、确认能被代码跳过，出问题既没法测也没法审计。

## Transition
状态机定义了"有哪些状态"，但还有一个更重要的问题——状态之间的"边"和"条件"是什么。