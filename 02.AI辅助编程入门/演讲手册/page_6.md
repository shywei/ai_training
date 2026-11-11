## Slide Objective
盘点 2026 主流编程模型梯队，确立“Plan 慢思考推理模型 + Act 快速执行模型”的黄金组合拳。

## Suggested Presentation
请看第 6 页：2026 编程大模型选型与“Plan + Act 双模组合拳”。
在 2026 年，编程领域的模型格局呈现清晰的两极分化：
一极是【深度慢思考推理模型（Deep Reasoning Models）】，如 Claude 3.7 Sonnet (Thinking)、OpenAI o1/o3/GPT-5.5、DeepSeek-R1、Qwen3-Max。它们擅长复杂的数学证明、全局系统架构解耦、跨多模块时序梳理与疑难 Bug 根因分析；
另一极是【高吞吐快速执行模型（Fast Act Models）】，如 Claude 3.5 Sonnet、DeepSeek-V4、Qwen3-Coder-32B。它们响应极快、遵循指令严格、单 Token 价格极度亲民，极其擅长根据明确设计写出语法标准的函数、单测和配置文件。
因此，顶级开发者的标准打法是【Plan + Act 双模组合拳】：
在 **Plan 规划阶段**：调用推理模型进行只读扫描，推演边界条件，产出无歧义的 `tasks.md` 任务单；
在 **Act 执行阶段**：切换为快速轻量模型，按照任务单逐个小步编码和跑单测！兼具了巅峰智商与极致低成本。

## Key Q&A / Clarifications
**Q：国内私有化部署场景，推荐哪款开源编程模型？**
A：首选 Qwen3-Coder-32B 或 DeepSeek 垂直代码系列。它们在真实企业 Java、Spring Boot 和 SQL 生成基准测试中表现卓越，且 32B 规模在双卡 RTX 4090 或单张 A100/H800 上即可高效跑起来。

## Transition
选好模型后，我们如何向 Coding Agent 下达指令？第三章，面向 Coding Agent 的提示词工程。
