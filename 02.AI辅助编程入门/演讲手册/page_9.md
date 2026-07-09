# Harness概念体系

## Slide Objective
拆解 Agent 的驾驭层（Harness）架构，让学员理解大模型是如何被约束和扩展，从而变成“能干活的 Agent”的。

## Suggested Presentation
我们一直在说 Agent，那大模型到底是怎么变成 Agent 的呢？
核心就在于这一层：Harness，也就是“驾驭层”。它就像是大脑外面的四肢和规矩。
我们来看它包含哪些核心组件。
首先是 System Prompt，它决定了 Agent 的“人格”，就像员工入职时的企业文化培训。
然后是 Rules（规则），它规定了 Agent 的行为边界，比如你的项目必须用 TypeScript，不能用 JS。
接下来是 Skills（技能）。它就像给 AI 装备了不同的工具箱，你可以让它学会怎么生成 PPT，怎么分析数据。
如果 AI 还需要操作外部系统怎么办？这就是 MCP（模型上下文协议），它是一个标准接口，让 AI 能够连接你的数据库、文件系统、甚至服务器。
最后是上下文管理，决定了在有限的 Token 窗口里，AI 能“记”住哪些最重要的信息。
有了这套体系，AI 就不再只是个聊天机器人了。

## Key Emphasis
重点讲清楚 Harness 的五大组件（System Prompt, Rules, Skills, MCP, Context）及其作用。
必须强调它们之间的协作关系：人格+规则+技能+工具+记忆 = Agent。

## Optional Extension
如果学员对 Harness 觉得陌生，可以强调：这是一个概念集合，不同的工具（如 Cursor, Claude Code）都在底层实现了这些机制，只是叫法可能略有不同，但本质一致。

## Classroom Interaction
在座的各位，有谁之前听说过 MCP 这个概念的？（不管有没有，借此引出下一页对 MCP 的详细解释）

## Take Away
Harness 体系（规则、技能、工具、上下文）是将通用大模型转化为专用编程 Agent 的核心架构。

## Why This Slide Matters
这是整门课程最核心的架构认知。如果不理解 Harness，学员在使用各种 AI 编程工具时只会知其然而不知其所以然。

## Transition
在整个 Harness 体系中，有两个组件对我们开发者扩展 Agent 能力最重要，那就是 Skills 和 MCP。我们来专门拆解一下这两个武器。
