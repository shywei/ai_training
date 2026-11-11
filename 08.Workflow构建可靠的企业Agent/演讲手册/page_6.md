## Slide Objective
深入状态机的结构——Node、Edge、Condition 三个核心元素，Checkpoint/Resume 机制，以及手搓状态机与 2026 主流图编排引擎（StateGraph）四大核心原语的架构映射。

## Suggested Presentation
很多人以为状态机就是一堆状态标签。错。状态机的核心是三个东西：Node、Edge、Condition。

Node 是节点，就是 Agent 所处的当前状态与计算单元。节点不只是一个标签——它定义了"在这个状态下，Agent 能做什么"。EXECUTING 节点，Agent 可以调用工具、思考、返回结果。CONFIRMING 节点，Agent 只能等，不能执行任何操作。这就是"状态决定行为"。

Edge 是边，就是状态之间的转换路径。每条边定义了"从状态 A 到状态 B 的条件"。EXECUTING → COMPLETED 的边，条件是"所有步骤完成"。EXECUTING → FAILED 的边，条件是"retryCount >= 3"。边是有方向的——不能从 COMPLETED 回到 EXECUTING。

Condition 是条件，就是触发转换的判断逻辑。注意——条件不是 LLM 决定的，是代码硬编码的确定性逻辑。retryCount >= 3 就走 FAILED，不会因为 LLM "觉得应该再试一次"就多试。这是用确定性约束非确定性的具体体现。

左下方我们加了一个非常关键的架构桥接：**手搓 `WorkflowState + AgentRuntime` 如何直接映射到 2026 主流图编排引擎（`StateGraph`，如 LangGraph / Spring AI Alibaba Graph / AgentScope）的四大核心原语**：
1. **`State & Reducer`（全局共享状态与状态合并器）** ↔ 对应我们 Demo 中的 `WorkflowTrace` + `conversationHistory`（记录每步状态与增量消息合并）；
2. **`Nodes`（计算/动作节点）** ↔ 对应 Demo 中的 `TaskPlanner`（规划节点）、`THINK`（推理节点）、`executeToolCall`（工具执行节点）；
3. **`Conditional Edges`（条件路由边）** ↔ 对应 Demo 中判断是否有 `ToolCall`（有则走工具节点，无则收敛结束）、是否触发 `retryCount >= 3`（超限走降级/失败分支）；
4. **`Interrupt & Checkpointer`（断点挂起与状态持久化）** ↔ 对应 Demo 中的 `CONFIRMING` 状态暂停与 `resumeAfterConfirm` 恢复执行。
理解了今天手搓的这四块，第九课、第十课用框架做图编排和多 Agent 协作时，你就一眼能看穿框架底层的源码设计。

右边讲 **持久化执行与跨进程故障重建（Durable Execution & Rehydration）**，这是生产级 Agent 系统的生命线：

### 1. 状态持久化策略（Node Checkpointing）
- **为什么必须节点级写入**：不能等整个复杂任务全部跑完才存盘。在一个多步任务中，Agent 可能先查了库存、又调用了风控系统，如果第三步系统崩溃，前两步的外部调用结果绝对不能丢失！
- **持久化写入点**：在每一个 Node/Step 执行完毕后，宿主必须**实时将关键状态、上下文快照（当前步骤、已返回的工具结果、会话历史变量）写入底层存储（PostgreSQL / Redis）**。
- **并发防护与乐观锁（Version Lock）**：存储表必须带有 `version` 乐观锁字段。当分布式集群中发生网络脑裂或 Worker 竞争时，只有版本号匹配的写入才被接受，防止两个 Worker 同时恢复同一个任务造成双下单。

### 2. 跨进程故障恢复与重水化重建（Process Rehydration）
- **故障与漂移场景**：企业生产环境每天都会发生容器 OOM、节点漂移（K8s Pod 重启）、网络瞬断。
- **重水化（Rehydration）机制**：当新拉起的 Worker 接管该任务时，直接根据 `traceId` 读取最新的 Checkpoint 快照。它在内存中**完全反序列化、重建状态机实例与执行上下文**，恢复所有变量与历史观察结果。
- **关键价值：中断点断点续传**：Agent 直接从**最近中断的 Step 继续执行**，**绝不需要调用大模型从头重新规划**，更关键的是**绝对不会重复调用已经执行成功的写操作工具（如扣库存）**！这正是生产级 Agent 与玩具 Demo 的分水岭。

底部的关键认知：LLM 是非确定性的，状态机是确定性的。Workflow 的价值就是用确定性约束非确定性。这个句子今天会反复出现，因为它是第八课的核心思想。

## Key Emphasis
Node/Edge/Condition 是状态机的结构，不是装饰。理解了这三个元素，以及它们与现代 `StateGraph` 四大原语（`State & Reducer`、`Nodes`、`Conditional Edges`、`Interrupt & Checkpointer`）的一一对应关系，才能看懂所有主流 Agent 编排框架。

Checkpoint 和 Resume 是两个不同的概念——Checkpoint 是"存档"，Resume 是"读档"。不要混淆。

## Optional Extension
**学员问：Condition 能不能用 LLM 来判断？**
答：不建议。Condition 应该是确定性的——如果 A 就走 B，如果 C 就走 D。用 LLM 判断 Condition 就回到了"非确定性"的问题。LLM 负责"想"（THINK），Condition 负责"判断"（状态转换），两者职责不同。

**学员问：Checkpoint 存什么？持久化表怎么设计？**
答：先定"存什么"，再定"怎么存"。存的至少三样：当前状态（WorkflowState 枚举值）、已执行的步骤列表（WorkflowStep）、已收集的工具结果（conversationHistory）——恢复时 Agent 才知道"我做到哪了，已经查到什么了"。
**Demo 做法**：这三样散在内存（TraceService.traces + ConfirmationService.pendingConfirmations 两个 ConcurrentHashMap），只做 CONFIRMING 暂停 + 确认后 Resume，进程停了就没了。
**生产做法**：workflow_checkpoint 表（trace_id 主键 / state / current_step / step_payload JSON / version 乐观锁 / updated_at），在状态变更点写一行，恢复时按 trace_id 读回重放。取舍：JSON 打包 vs 拆列（恢复快 vs 可 SQL 审计）；只在状态变更点写（写放大低）vs 每轮都写（回放更细）。

## Take Away
先写状态转换表（状态 × 触发条件）再写代码，手搓 `WorkflowState + AgentRuntime` 的四要素直接对应现代 `StateGraph` 的 `State & Reducer / Nodes / Conditional Edges / Interrupt & Checkpointer`。

## Why This Slide Matters
跳过这页，学员会把状态转换条件交给 LLM 判断、或散进 if-else 里，出错时说不清走了哪条 Edge——状态机退化成装饰，后续学框架图编排也会失去底座。

## Transition
状态机的结构与现代 StateGraph 原语拆解清楚了，下一页把"状态机编排"与"纯 ReAct 裸跑"放在一起做正面对比。