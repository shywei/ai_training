## Slide Objective
对比状态机和纯 ReAct，让学员理解为什么需要 Runtime 来"管" LLM。

## Suggested Presentation
左边是第七课的纯 ReAct 模式。LLM 自主决定 THINK→ACT→OBSERVE，没有外部约束。没有停止条件，可能无限循环。工具失败直接报错。高风险操作无拦截。无执行记录。

右边是第八课的状态机 + Agent Runtime。Runtime 控制循环，最大 10 轮强制停止。每个状态有明确的转换条件。工具失败 → 重试 3 次 → 熔断 → 降级。高风险操作暂停，等待用户确认。每步记录 Trace。

注意底部的类比：LLM 是"大脑"，Runtime 是"操作系统"。大脑负责思考和决策，操作系统负责资源管理、任务调度、异常处理。你不会让员工自己决定"要不要走审批流程"——那是流程系统的事。

这个类比很重要。很多做 AI 的同学把 LLM 当成"万能的"——什么都让 LLM 决定。但在企业环境里，LLM 只是"执行单元"，决策权在 Workflow 层。就像公司里，员工做事，但有流程和审批。

## Key Emphasis
"LLM 是大脑，Runtime 是操作系统"——这个类比要讲透。它决定了后续所有设计的思路：LLM 负责"想"，Runtime 负责"管"。

不要在这页讲六大组件的细节——那是下一页的内容。

## Optional Extension
**学员问：Runtime 管得太多，会不会影响 LLM 的灵活性？**
答：Runtime 管的是"边界"，不是"内容"。LLM 在边界内自由发挥——想调什么工具、怎么推理、怎么回答。但边界是确定的：最多 10 轮、高风险要确认、失败要重试。就像公司里的员工手册——规定了流程，但不限制你的创造力。

**学员问：LangGraph 是不是就是这个？**
答：LangGraph 把这套模式做成了框架——状态图、节点、边、条件。第八课手搓一遍，第九课用 Spring AI Alibaba 重构，第十课对比 LangGraph。三步下来你就理解了 Workflow 的本质。

## Take Away
LLM 负责"想"，Runtime 负责"管"。用操作系统管理 CPU，用 Workflow 管理 LLM。

## Why This Slide Matters
如果跳过这页，学员会以为状态机和 Runtime 是两个独立的东西。其实它们是一体的——状态机是骨架，Runtime 是肌肉。

## Transition
接下来讲 Agent Runtime 的完整执行流程。