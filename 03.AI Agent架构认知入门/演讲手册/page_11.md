## Slide Objective
介绍Multi-Agent的概念和行业主流方案，让学员理解多智能体协作的架构模式。

## Suggested Presentation
在真实世界里，造一辆汽车不是一个人能干完的，需要不同工种的配合。AI也一样。

当任务复杂到一定程度，把所有Prompt、所有Tool都塞给一个Agent，它会"精神分裂"——上下文太长、决策太复杂、出错率飙升。

解决方案是Multi-Agent：创建多个专精Agent，由一个调度员（Orchestrator）统一协调。
比如一个"代码审查"任务：Orchestrator把它拆成三个子任务，分配给"代码分析Agent"、"安全检查Agent"、"规范审查Agent"各自执行，最后汇总结果。

Multi-Agent和Workflow有什么区别？
Workflow编排的是"步骤"——第一步做什么、第二步做什么，是确定性的流程。
Multi-Agent编排的是"角色"——每个Agent有独立的大脑（LLM）、独立的Prompt、独立的工具，是带智能的协作。
简单任务用单Agent加Workflow就够了，复杂任务才需要Multi-Agent。

目前行业里有哪些主流的Multi-Agent方案？
CrewAI是目前最流行的Multi-Agent框架，它的核心概念是"角色（Role）+ 任务（Task）+ 流程（Process）"。你定义几个角色，每个角色有自己的目标和工具，CrewAI负责调度和协作。
AutoGen是微软开源的多Agent框架，更侧重于Agent之间的对话式协作——多个Agent像开会一样讨论问题，逐步收敛到解决方案。
LangGraph也能做Multi-Agent，因为它本身就是一张图，可以把每个Agent当作图中的一个节点，用边定义它们之间的通信关系。
在实际产品中，Claude Code的Sub-Agent机制就是Multi-Agent的实践——主Agent派生子Agent处理子任务，完成后汇报结果。

选型建议：如果需要角色分工明确的协作，选CrewAI。如果需要Agent之间对话式讨论，选AutoGen。如果已经在用LangGraph做Workflow，直接在图里加Agent节点就行。

## Key Emphasis
区分Multi-Agent和Workflow：编排步骤 vs 编排角色。
展开三个主流方案（CrewAI/AutoGen/LangGraph）的定位和差异。
给出选型建议，帮学员建立实际的技术判断力。

## Optional Extension
如果学员问"Agent之间怎么通信"，回答：主要有两种方式。一种是消息传递——Agent之间通过共享的消息队列交换信息。另一种是共享内存——所有Agent读写同一个上下文空间。CrewAI用的是前者，LangGraph用的是后者。

## Classroom Interaction
无。

## Take Away
Multi-Agent通过专业分工和团队协作解决超复杂任务。CrewAI做角色协作，AutoGen做对话协作，LangGraph做图编排。

## Why This Slide Matters
Multi-Agent代表了Agent架构的最前沿方向。让学员知道这不只是概念，已经有成熟的框架可以落地。

## Transition
到这里，认知层和执行层的六个核心组件全部讲完了。但我们还没有把这些零件组装起来。接下来，我们看一个完整的实战案例：导购Agent，看看这些组件在真实系统里是怎么协作的。
