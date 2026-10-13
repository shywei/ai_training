## Slide Objective
讲解模型路由和 Workflow 编排——不是所有步骤都用同一个模型，Workflow 有不同编排模式。

## Suggested Presentation
左边讲 Router 节点。在 Workflow 中，不是所有步骤都用同一个模型。TaskPlanner 规划用 reasoning 模型——需要理解任务、拆步骤，这是复杂推理。THINK 思考用 reasoning 模型——需要推理、决定下一步。最终回复用 chat 模型——生成自然语言，不需要强推理。

这就是"动态模型选择"——根据任务类型，自动选最合适的模型。简单任务用快模型（便宜、低延迟），复杂任务用推理模型（强、贵）。这在企业环境里很重要——每天调用上万次，模型选择直接影响成本。

但要注意底部的标注：Demo 实际采用统一模型。为什么？因为 Demo 为了简化部署，TaskPlanner 和 THINK 都用同一个 reasoning 模型（Qwen3-8B），最终回复也用同一个。分级模型路由标注为"生产建议架构"——学员可以在自己扩展时实现。这个区分很重要，不能让学员以为 Demo 已经实现了分级路由。

右边讲两种编排模式。顺序编排——步骤按顺序执行：Plan → Step1 → Step2 → Done。Demo 采用这种方式，简单可控。条件编排——根据条件走不同分支：if 库存>0 → 下单，else → 预订。这需要 Workflow Engine（如 LangGraph、Temporal），Demo 未覆盖。

顺序编排适合大多数场景——多步查询、对比分析、分步执行。条件编排适合复杂业务流程——审批流、异常处理、多分支决策。

## Key Emphasis
Demo 实际采用统一模型——这个标注要强调。不能让学员以为 Demo 已经实现了分级路由。

条件编排是"仅 PPT 讲解"——学员只需要知道概念和方向，不需要自己实现。

## Optional Extension
**学员问：怎么实现分级模型路由？**
答：在 AgentRuntime 的 callLLM() 方法里，根据调用场景选择不同的 model 参数。TaskPlanner 和 THINK 传 reasoning 模型，最终回复传 chat 模型。application.yml 里已经配了多个模型名——agent.model.reasoning、agent.model.chat。只需要在 callLLM() 里传不同的值就行。

**学员问：LangGraph 的条件编排是怎么做的？**
答：LangGraph 用"状态图"——节点（Node）和边（Edge），边上可以加条件（Condition）。类似状态机，但更灵活——支持并行、子图、动态边。第十课会对比 LangGraph 和手搓 Workflow 的差异。

## Take Away
不是所有步骤都用同一个模型。按任务类型选模型，是成本优化的关键。

## Why This Slide Matters
如果跳过这页，学员不知道 Workflow 可以"分级"——模型分级、编排分级。这是从 Demo 到生产的认知升级。

## Transition
接下来讲容错——工具调用失败怎么办。