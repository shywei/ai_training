## Slide Objective
深入状态机的结构——Node、Edge、Condition 三个核心元素，以及 Checkpoint/Resume 机制。

## Suggested Presentation
很多人以为状态机就是一堆状态标签。错。状态机的核心是三个东西：Node、Edge、Condition。

Node 是节点，就是 Agent 所处的当前状态。但节点不只是一个标签——它定义了"在这个状态下，Agent 能做什么"。EXECUTING 节点，Agent 可以调用工具、思考、返回结果。CONFIRMING 节点，Agent 只能等，不能执行任何操作。这就是"状态决定行为"。

Edge 是边，就是状态之间的转换路径。每条边定义了"从状态 A 到状态 B 的条件"。EXECUTING → COMPLETED 的边，条件是"所有步骤完成"。EXECUTING → FAILED 的边，条件是"retryCount >= 3"。边是有方向的——不能从 COMPLETED 回到 EXECUTING。

Condition 是条件，就是触发转换的判断逻辑。注意——条件不是 LLM 决定的，是代码硬编码的确定性逻辑。retryCount >= 3 就走 FAILED，不会因为 LLM "觉得应该再试一次"就多试。这是用确定性约束非确定性的具体体现。

右边讲 Checkpoint 和 Resume。Checkpoint 是在关键节点保存 Agent 的完整状态——当前步骤、已收集的工具结果、状态机位置。就像游戏的存档点。Resume 是从检查点恢复执行。Agent 挂了、用户取消后反悔、超时后重试——都能从最近的检查点继续，不用从头来。

Demo 中已覆盖 CONFIRMING 状态暂停 + 用户确认后 Resume。持久化 Checkpoint（存数据库，跨进程恢复）仅 PPT 讲解——那需要序列化整个 Agent 状态，工程量比较大。

底部的关键认知：LLM 是非确定性的，状态机是确定性的。Workflow 的价值就是用确定性约束非确定性。这个句子今天会反复出现，因为它是第八课的核心思想。

## Key Emphasis
Node/Edge/Condition 是状态机的结构，不是装饰。理解了这三个元素，才能理解为什么状态机能让 Agent 的行为可预测。

Checkpoint 和 Resume 是两个不同的概念——Checkpoint 是"存档"，Resume 是"读档"。不要混淆。

## Optional Extension
**学员问：Condition 能不能用 LLM 来判断？**
答：不建议。Condition 应该是确定性的——如果 A 就走 B，如果 C 就走 D。用 LLM 判断 Condition 就回到了"非确定性"的问题。LLM 负责"想"（THINK），Condition 负责"判断"（状态转换），两者职责不同。

**学员问：Checkpoint 存什么？**
答：至少存三样东西：当前状态（WorkflowState）、已执行的步骤列表（WorkflowStep）、已收集的工具结果（conversationHistory）。这样恢复时 Agent 能知道"我做到哪了，已经查到什么了"。

## Take Away
Node 决定行为，Edge 决定路径，Condition 决定方向。Checkpoint 让 Agent 能"存档读档"。

## Why This Slide Matters
如果跳过这页，学员只知道"有 7 个状态"，但不知道"状态机为什么能约束 LLM"。约束力来自 Edge 和 Condition——确定性的转换条件。

## Transition
状态机定义了"有哪些状态、怎么转换"，接下来讲"谁来执行"——Agent Runtime。