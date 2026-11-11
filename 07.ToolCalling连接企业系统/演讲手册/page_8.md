## Slide Objective
讲解 ReAct 模式，同时明确 Demo 的边界——Demo 只做到了 Reason + Act，完整的 Agent Loop 需要第八课实现。

## Suggested Presentation
Function Calling 不是凭空出现的，背后有一个重要的思想：ReAct 模式。2022 年 Google 的论文提出，LLM 可以交替进行"推理"和"行动"，而不是只能生成文本。

ReAct 的循环：**Reason（推理）** → **Act（行动）** → **Observe（观察）** → 继续推理...

看实际例子："拯救者和 MacBook 哪个便宜？"LLM 经过两轮 Reason→Act→Observe 循环：推理"需要查两个价格" → 调用 queryProductInfo("NB003") → 观察到"拯救者 ¥8999" → 推理"还需要查 MacBook" → 调用 queryProductInfo("NB002") → 观察到"MacBook ¥14999" → 推理"8999 < 14999" → 回复"拯救者更便宜"。

理解 ReAct 的意义：LLM "决定"调用工具不是随机的，是推理的结果。

**但这里必须说清楚一个重要的边界：Demo 只做到了 Reason + Act，没有做到完整的 ReAct 循环。**

Demo 中的 tool_call 是 Spring AI 框架自动处理的——LLM 返回 tool_call，Spring AI 执行，结果回传 LLM，LLM 生成最终回复。这是一次性的处理，不是 LLM 自主控制的多轮循环。

真正的 Agent Loop 是这样的：LLM 返回 tool_call → 执行 → 观察 → LLM 决定"还不够，再调一个" → 再执行 → 再观察 → 直到 LLM 说"够了"。**循环的控制权在 LLM，不在框架。** 这需要一个 Agent Runtime 来管理循环状态、控制终止条件、处理错误。

Demo 中"拯救者和 MacBook 哪个便宜"的例子，实际上是 LLM 在一次推理中返回了两个 tool_call（Spring AI 支持批量 tool_call），不是真正的多轮循环。区别在于：批量 tool_call 是 LLM 提前规划好的，多轮循环是 LLM 根据中间结果动态决定的。

真正的 Agent Loop 在第八课（Agent Runtime）实现。第七课建立概念，第八课工程落地。

## Key Emphasis
不要让学员误以为 Demo 已经实现了完整的 ReAct 循环。Demo 实现了 Function Calling（单次 tool_call），但 Agent Loop（多轮自主循环）需要 Runtime 支持，是第八课的内容。

## Optional Extension
**学员问：为什么说循环控制权在 LLM 才叫 Agent Loop？框架自动执行 tool_call 不也是一轮一轮来的吗？**
答：看谁决定"下一步做什么"。框架自动执行是 LLM 一次给出全部计划、执行层照单全收；Agent Loop 是 LLM 每轮看到 Observe 结果后重新决定下一步。
Demo 做法：今天的链路里 Spring AI 一次性执行 tool_call，批量调用是 LLM 提前规划好的；第八课 `AgentRuntime` 里 `extractToolCall()` 每轮从 THINK 输出解析 `TOOL_CALL:工具名(参数)`，`MAX_ITERATIONS = 10` 封顶，上一轮工具结果拼进下一轮 THINK 的输入——这才是按中间结果动态决策。
生产做法：循环必须带预算（轮数/时间/token 三个上限都要有），失败有三级容错（第八课：重试/熔断/降级），全程 Trace 可回放。取舍：循环给了 LLM 决策自由度，预算和 Trace 是缰绳——没有缰绳的循环是失控的起点。

## Take Away
判断一段实现是不是真 Agent Loop：看 LLM 能否根据上一轮 Observe 改变下一步计划——一次性批量 tool_call 不算；出现"中间结果决定后续步骤"的需求，才值得上第八课的 Agent Runtime。

## Why This Slide Matters
没有这页，学员会误以为"Spring AI 处理 tool_call 就是 Agent Loop"，不理解真正的 Agent Loop 需要 Runtime 控制循环。

## Transition
Function Calling 定义了"怎么调用工具"，ReAct 解释了"为什么 LLM 能决定调用工具"。但还有一个问题：工具怎么暴露、怎么发现？MCP 解决这个问题。
