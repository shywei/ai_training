## Slide Objective
让学员理解 Token 和 Context Window 的工程含义，以及模型选型的成本考量。

## Suggested Presentation
Token 是大模型的计费单位。一个汉字通常 1-2 个 token，一个英文单词 1 个 token，一个函数大约 50-200 个 token。输出 token 比输入贵 2-5 倍——这是成本控制的关键。

Context Window 是模型一次能处理的最大 token 数。输入加输出不能超限。2026 年主流模型支持 100 万 token，约 50 万字。但窗口越大，推理越慢、成本越高。不是越大越好。

右边两个模型的对比：Qwen2.5-7B 是快模型，日常对话够用；Qwen3-8B 是推理模型，有思维链，适合复杂分析。推理模型回答同一个问题可能消耗 3-10 倍的 token。这就是为什么需要模型路由——简单问题不要用推理模型，浪费钱。

## Key Emphasis
Token 计费和 Context Window 是成本控制的基础。学员需要建立"token = 钱"的意识。

## Take Away
Token 是钱，窗口有上限。简单问题用快模型，复杂问题用推理模型。

## Transition
知道了 Token 和模型差异，下一个问题就是：代码里怎么实现"简单问题走快模型，复杂问题走推理模型"？这就是模型路由。
