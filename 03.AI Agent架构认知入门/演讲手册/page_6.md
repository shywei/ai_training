## Slide Objective
深入剖析 Agent 运行时的三大核心认知循环（ReAct、Plan-and-Execute、Reflection），讲清“思考-行动-观察”的数学与状态本质。

## Suggested Presentation
我们来到第二章：运行时认知循环。请看第 6 页：三大核心认知循环。
这是所有现代 Agent 能够“自主行动”的底层心智模型：
1. **ReAct 范式（Reason + Act，推理与行动交织）**：
   这是 2026 年应用最广泛的经典基础循环。它的时序非常直观：
   **Thought（思考）**：“用户想买 5000 元左右的轻薄办公本，我需要先查库存”；
   **Action（行动）**：决定调用工具 `search_products(keyword="轻薄本", max_price=5000)`；
   **Observation（观察）**：系统拦截并执行工具，把真实数据库返回的商品列表作为输入回传；
   **Thought（再思考）**：“根据返回的 3 款商品，ThinkPad X1 最符合要求，准备生成推荐”；
   **Final Answer（交付）**：输出最终回复。
2. **Plan-and-Execute（规划与执行分离）**：
   面对超长多步任务，ReAct 容易走一步看一步导致短视。此模式由 Planner 先生成一个包含 5 个步骤的执行图，由 Executor 逐项执行，遇到阻塞时触发 Replanner 动态调整计划；
3. **Reflection（反思与自我批评）**：
   在输出交付前，引入一个批评者视角（Critic），对结果进行逻辑自洽性校验、代码单测验证或合规审查，不达标则自我纠偏重新生成。

## Key Q&A / Clarifications
**Q：ReAct 循环中的“Thought”在 2026 年通常是如何体现的？**
A：在早期，Thought 是在 Prompt 中让模型显式输出一段文字；而在 2025–2026 年现代推理模型中，Thought 已经原生内化为大模型的 `reasoning_content`（慢思考思维链），通过流式输出与正文隔离。在工业工程落地中，我们通常基于流式事件通道实现思考流与正文流的优雅解耦。

## Transition
认知循环中的每一次思考与行动，依赖的全部养料都来自于同一个源头：第 7 页，智能体系统的心智中枢——Context Engineering！
