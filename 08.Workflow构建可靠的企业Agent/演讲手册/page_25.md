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

底部是 Multi-Agent 协作预告。单 Agent 有三个天花板：上下文有限（token 限制）、无法并行（一次只能做一件事）、专业受限（一个 Agent 不能什么都懂）。这是第十课的核心内容。主流协作模式——Router（前端分流）、Supervisor-Worker（主从拆解与汇总）、Handoff（控制权与上下文单向移交），以及 Pipeline 串行、Parallel 并行汇总等。第十课会展开每种模式的控制权、适用场景和框架实现，帮你建立“什么场景选什么编排”的判断力。

## Key Emphasis
"导购 Agent 正式上线"是一个里程碑——五个能力全部就位。这给学员一个清晰的进度感和成就感。

Multi-Agent 是第十课的内容——单 Agent 有天花板，但先让一个 Agent 可靠运行，再考虑多个 Agent 协作。

## Optional Extension
**学员问：Multi-Agent 和 Workflow 的关系是什么？**
答：Workflow 管的是"一个 Agent 怎么可靠执行"，Multi-Agent 管的是"多个 Agent 怎么协作"。Workflow 是基础，Multi-Agent 是扩展。一个 Agent 都跑不稳，多个 Agent 只会更乱。**Demo 做法**：单 Agent + 本课 Workflow 五件套（状态机/容错/确认/Trace）。**生产做法**：先把单 Agent 可靠性做扎实，再按任务形态选协作模式（Router/Supervisor-Worker/Handoff）——第十课展开选型判断。

**学员问：这么多协作模式哪个最常用？**
答：Router 和 Supervisor-Worker。Router 最简单——前端按任务类型分流给不同 Agent，适合任务明确、互不交叉。Supervisor-Worker 最常用——一个"主 Agent"拆解任务、分配给多个"子 Agent"、再汇总，适合复杂任务需要统一规划。Pipeline 适合流程固定的串行任务（数据提取→分析→报告），Handoff 适合专家接力、上下文隔离（客服 Agent 转技术 Agent），Parallel+汇总适合独立子任务并行后聚合。选型判断是第十课的重点。

## Take Away
验收"Agent 上线"的清单是五件套：Prompt、Memory、Knowledge、Tool、Workflow（含确认/容错/Trace）——缺任何一件都叫 Demo，不叫上线。

## Why This Slide Matters
跳过这页，学员会拿缺件的 Agent（没有确认和 Trace）当"上线"交付，出事故才发现五件套缺了安全和可观测。

## Transition
最后一张——总结和第九课预告。