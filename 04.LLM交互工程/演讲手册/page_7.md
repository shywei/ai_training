## Slide Objective
让学员理解 Token 计费结构、**Prompt Caching（前缀缓存）**、Context Window 注意力退化，以及推理模型思考预算（`reasoning_effort`）与快慢模型选型。

## Suggested Presentation
Token 是大模型的计费单位。你在硅基流动控制台能看到每次调用的 token 消耗。一个汉字约 0.6~1.5 个 token，一个函数 50-200 个 token。

2026 年算 Token 账必须看懂**三档计费**：
1. **缓存命中输入（Cached Input）**：最便宜，只有标准输入价格的 **10%~25%**！
2. **标准未命中输入（Cache Miss Input）**：1 倍基准价。
3. **输出 Token（含推理模型的思维链 Thinking）**：比输入贵 **2-5 倍**。

这就引出了第二张卡片——**Prompt Caching（上下文前缀缓存）**，这是 2026 年 LLM 交互工程的第一降本增效利器（OpenAI、DeepSeek、Claude、通义千问全部支持）。它的原理是：服务端会自动把请求头部相同的 Token 前缀对应的 **KV Cache** 缓存在显存或高速磁盘里（通常满 1024 tokens 自动触发）。只要你每次请求的头部前缀（比如 System Prompt + 工具定义 + 固定示例）一字不差，第二次调用直接命中缓存，**省掉 50%~90% 的输入费用，首字延迟（TTFT）还能降低 80%**！这直接决定了我们第 10 页讲 Prompt 结构时的布局铁律。

第三张卡片看 Context Window。虽然 2026 主流模型窗口已达 256K~100 万 Token，但**窗口大绝不等于可以无脑往里塞**！上下文越长，不仅单次成本剧增、TTFT 变慢，更会出现 **Lost in the Middle（中间信息遗忘/注意力退化）**——模型对放在长上下文正中间的规则遵循率会大幅下降。

第四张卡片看推理模型。推理模型在输出正式回答前会生成大量思维链（Thinking），这些思考过程也是按昂贵的输出 Token 计费的，同样一个问题可能消耗 3-10 倍 Token。在生产中，我们可以通过 `reasoning_effort`（low/medium/high）或 `thinking_budget` 限制它的思考深度；更重要的是做**模型路由**——日常简单问答走快模型（`agent.model.fast`，如 Qwen2.5-7B），复杂分析与严格结构化抽取才走推理模型（`agent.model.reasoning`，如 DeepSeek-R1 / Qwen3-8B）。

你可以在 Demo（`v1.1.1`）里验证：在前端问"推荐一台笔记本"——看聊天气泡顶部的 `[MODEL]` 标签，用的是 `Qwen2.5-7B`。然后问"分析一下游戏本和轻薄本的架构设计差异"——含"分析/架构"关键词，`ModelRouter` 自动切换到推理模型。

## Key Emphasis
讲透“三档 Token 价格”与“Prompt Caching 前缀缓存原理”，并让学员在 Demo 前端观察 `[MODEL]` 标签的切换。

## Optional Extension
**学员问：免费模型够用了，为什么还要算 token 成本与缓存命中率？**
答：免费模型随时下架，付费是生产常态——成本核算与前缀缓存是保命技能。
Demo 做法：本课 `v1.1.1` 先用 `ModelRouter` 演示把简单请求分流给快模型；在进阶架构中可进一步引入统一日志中间件记录每次调用耗时与文本。
生产做法：解析响应 JSON 的 `usage`（含 `prompt_tokens`、`completion_tokens` 与 `cached_tokens`）折算金额落成本表，监控 **Cache Hit Rate（缓存命中率，目标 >70%）**，设日/月配额与预算告警（page_16 第 2 项）。

## Classroom Interaction
让学员算一笔账：如果一个 Agent 的 System Prompt + 工具定义有 4000 tokens，每天调用 10 万次，开启并命中 Prefix Caching（按 0.2x 计费）一年能省下多少比例的输入成本？（答案：直接省掉这部分固定开销的 80%！）

## Take Away
输出 token 比输入贵 2-5 倍，而命中前缀缓存的输入只需 0.1~0.25 倍——精简输出、锁死静态前缀、快慢模型分流，是控制 LLM 成本的三驾马车。

## Why This Slide Matters
跳过这页，学员会无节制地把长上下文塞进 Prompt，成本失控还没有感知——等看到账单时已经晚了一个月。

## Transition
知道了 Token 和模型差异，下一个问题：代码里怎么实现"简单问题走快模型，复杂问题走推理模型"？这就是模型路由。
