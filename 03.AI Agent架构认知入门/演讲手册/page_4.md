## Slide Objective
讲透 Anthropic 经典架构分水岭：Workflows（确定性编排）vs Autonomous Agents（自主智能体），掌握两种模式的权衡与适用边界。

## Suggested Presentation
请看第 4 页，这是业界公认的最经典架构分界线：Workflows vs Autonomous Agents。
很多技术团队一上来就盲目搞“全自主智能体”，结果线上失控死循环，这就是没搞清楚这道分水岭：
左边是【Workflows（工作流 / 确定性编排）】：
它的**控制流是由人类架构师在代码中预先固化写死的（Code-driven Deterministic Flow）**。系统包含五大经典编排模式：
1. **Prompt Chaining**：上一步的输出作为下一步的输入；
2. **Routing**：先分类意图，按条件分流到特定分支；
3. **Parallelization**：并发调用多个模型提取不同维度信息后聚合；
4. **Orchestrator-Workers**：主管模型拆分子任务，分配给固定的一组工人模型；
5. **Evaluator-Optimizer**：一个模型生成，另一个模型打分审查，不满意打回重写。
Workflows 的确定性极高、行为可预测、极少出错，是企业核心交易业务的基石！
右边是【Autonomous Agents（自主智能体）】：
它的**控制流完全交由大模型自主动态决定（LLM-driven Dynamic Loop）**。模型根据环境返回的 Observation，自己决定下一步是查数据库、还是调搜索、还是交付用户。它的灵活性极高，但不可预测性也更高。
企业级架构的最高境界，是“用确定性的 Workflow 作为外围骨架，在局部需要复杂推理的节点嵌入 Autonomous Agent”！

## Key Q&A / Clarifications
**Q：在企业级电商导购或核心业务系统中，通常采用的是 Workflow 还是 Autonomous Agent？**
A：采用的是【状态机驱动的受控 Agent（Finite State Machine Guarded Agent）】！外层是严格的状态机生命周期，内层是 Tool Calling 认知循环，兼具两者的优势。

## Transition
理清了架构分水岭，我们在产品形态上还会遇到第二道分界线：通用 Harness vs 垂直应用 Agent。
