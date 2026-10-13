## Slide Objective
展示第八课的能力地图和 Agent 构建进度——导购 Agent 正式上线，以及 Multi-Agent 预告。

## Suggested Presentation
左边是第八课的能力地图，四个板块。

第一个：为什么不能相信 LLM。不确定性、不可控、无限循环、无审计。这是第八课的"问题意识"——所有设计都从这里出发。

第二个：状态机。Node/Edge/Condition、Checkpoint/Resume。状态机是 Workflow 的骨架——让 Agent 的行为可预测。

第三个：Agent Runtime。六大组件、TaskPlanner、THINK→ACT→OBSERVE。Runtime 是 Workflow 的肌肉——执行、调度、管理。

第四个：容错 + 人在回路 + 可观测性。重试→熔断→降级、确认/取消、Trace。这三件事让 Agent 可靠、安全、透明。

右边是 Agent 构建进度。Prompt（第四课）、Memory（第五课）、Knowledge（第六课）、Tool（第七课）、Workflow（第八课）——全部完成。第九课、第十课、第十一课待做。

🎉 导购 Agent 正式上线！Prompt + Memory + Knowledge + Tool + Workflow——五个能力全部就位。这个 Agent 能理解用户意图、记住用户偏好、查询商品知识、调用企业系统、多步执行复杂任务。它不再是一个"聊天机器人"，而是一个真正的"购物助手"。

底部是 Multi-Agent 协作预告。单 Agent 有三个天花板：上下文有限（token 限制）、无法并行（一次只能做一件事）、专业受限（一个 Agent 不能什么都懂）。四种协作模式：Orchestrator（编排者分配任务）、Pipeline（流水线上下游传递）、Specialist（各司其职）、Debate（辩论式讨论）。这是第十一课的核心内容。

## Key Emphasis
"导购 Agent 正式上线"是一个里程碑——五个能力全部就位。这给学员一个清晰的进度感和成就感。

Multi-Agent 是第十一课的内容——单 Agent 有天花板，但先让一个 Agent 可靠运行，再考虑多个 Agent 协作。

## Optional Extension
**学员问：Multi-Agent 和 Workflow 的关系是什么？**
答：Workflow 管的是"一个 Agent 怎么可靠执行"，Multi-Agent 管的是"多个 Agent 怎么协作"。Workflow 是基础，Multi-Agent 是扩展。一个 Agent 都跑不稳，多个 Agent 只会更乱。

**学员问：四种协作模式哪个最常用？**
答：Orchestrator——一个"调度 Agent"把任务分配给多个"执行 Agent"。这是最自然的模式，也是最容易实现的。Pipeline 适合流水线任务（数据提取→分析→报告），Specialist 适合专业分工（客服 Agent + 技术 Agent + 销售 Agent），Debate 适合需要多角度讨论的场景（方案评审）。

## Take Away
Workflow 不是流程，而是让 AI 在企业中可控运行的机制。五个能力就位，导购 Agent 正式上线。

## Why This Slide Matters
如果跳过这页，学员不知道自己的"构建进度"——学到哪里了，还差什么。能力地图是学习的"进度条"。

## Transition
最后一张——总结和第九课预告。