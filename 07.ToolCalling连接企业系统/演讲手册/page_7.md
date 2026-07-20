## Slide Objective
讲解 Function Calling 的思想基础——ReAct 模式。理解 ReAct 才能理解 LLM 为什么能"决定"调用工具。

## Suggested Presentation
Function Calling 不是凭空出现的，背后有一个重要的思想：ReAct 模式。2022 年 Google 的论文《ReAct: Synergizing Reasoning and Acting in Language Models》提出，LLM 可以交替进行"推理"和"行动"，而不是只能生成文本。

ReAct 的循环：**Reason（推理）** → **Act（行动）** → **Observe（观察）** → 继续推理...

看实际例子："拯救者和 MacBook 哪个便宜？"

LLM 的推理过程：
- **推理1**：用户要比较两个商品的价格，我需要查两个商品的信息
- **行动1**：调用 queryProductInfo("NB003") — 拯救者
- **观察1**：返回"价格 ¥8999"
- **推理2**：拯救者 ¥8999，还需要查 MacBook 的价格
- **行动2**：调用 queryProductInfo("NB002") — MacBook Pro 14
- **观察2**：返回"价格 ¥14999"
- **推理3**：8999 < 14999，拯救者更便宜
- **回复**："拯救者 ¥8999 比 MacBook Pro ¥14999 便宜 ¥6000"

这不是 LLM 一次性回答的，而是经过两轮 Reason→Act→Observe 循环。每次 tool_call 就是一次循环。LLM 在每一步都在"思考"——需要什么信息、该调什么函数、拿到结果后怎么处理。

理解 ReAct 的意义：LLM "决定"调用工具不是随机的，是推理的结果。它看到用户问价格，推理出"我需要查数据库"，然后调用 queryProductInfo。这个"推理"过程是 LLM 的核心能力——理解意图、分解任务、选择工具。

## Key Emphasis
ReAct 是思想，Function Calling 是工程实现。理解思想才能理解 LLM 的行为——为什么它会"决定"调用这个工具而不是那个工具。

## Transition
Function Calling 定义了"怎么调用工具"（协议），ReAct 解释了"为什么 LLM 能决定调用工具"（思想）。但还有一个问题：工具怎么暴露、怎么发现？MCP 解决这个问题。
