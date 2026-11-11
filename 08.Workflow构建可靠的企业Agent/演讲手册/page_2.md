## Slide Objective
展示课程结构，让学员建立预期。八个章节围绕一个核心：怎么让 Agent 可靠地运行。

## Suggested Presentation
今天的课程分八个部分。前半部分讲"为什么需要 Workflow"——LLM 的缺陷、状态机的概念、Runtime 的设计。后半部分讲"怎么实现"——容错、人在回路、可观测性。

注意第三章"状态机"里的 Node/Edge/Condition/Checkpoint/Resume。很多人以为状态机就是一堆状态标签，其实状态机的核心是"边"和"条件"——怎么从一个状态走到另一个状态，由什么条件触发。这是本课的重点概念之一。

第四章 Agent Runtime 是今天最核心的内容。六大组件、TaskPlanner、THINK→ACT→OBSERVE 循环——这是所有 Agent 框架的底层模式。LangChain、LangGraph 底下都是这一套。手搓一遍，你才能理解框架帮了什么。

第七章专门讲 Demo 的知识体系覆盖情况——哪些是 Demo 已覆盖的（学员可动手验证），哪些是仅 PPT 讲解的（知道概念和方向即可）。这个区分很重要，不要学员以为所有内容都在 Demo 里实现了。

最后有演示环节——多步对比查询、容错降级、人在回路。演示在课程结束后集中进行。

## Key Emphasis
八章内容不是独立的，围绕同一个问题：Agent 从"玩具"变成"工具"需要什么。先讲问题（为什么不能信 LLM），再讲解法（状态机、Runtime、容错）。

不要在目录页就展开细节——学员只需要建立框架感。

## Optional Extension
**学员问：为什么叫"Workflow"不叫"Agent Loop"？**
答：Workflow 强调的是"可控"——状态机、条件判断、人工确认。Agent Loop 强调的是"循环"——THINK→ACT→OBSERVE。第八课两者都有，但核心价值是"可控"。

**学员问：和 LangGraph 的 Workflow 有什么关系？**
答：LangGraph 是把这套模式做成了框架——状态图、节点、边。**Demo 做法**：本课手搓 WorkflowState + AgentRuntime 循环，7 个状态和转换条件全在代码里。**生产做法**：第九课用 AgentScope Java 2.0 重构同等功能，第十课对比 LangGraph——看清框架接管什么、你还要写什么，你就理解了 Workflow 的本质。

## Take Away
判断要不要上 Workflow 看三条硬指标：流程有多步、有高风险操作、要可追溯——占一条就需要状态机 + Runtime，只是单轮问答就别上。

## Why This Slide Matters
跳过这页，学员容易把 Workflow 理解成画流程图——项目里只画图不建状态机/容错/确认，落地时整套返工。

## Transition
先回答一个关键问题——为什么需要 Workflow。