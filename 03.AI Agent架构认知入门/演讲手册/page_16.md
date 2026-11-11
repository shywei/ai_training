## Slide Objective
给出 2026 AI Agent 七层技术栈生态坐标全景，帮助学员建立新技术的归类定位能力，消除技术焦虑。

## Suggested Presentation
我们来到第五章：2026 技术栈坐标与架构师决策树。请看第 16 页。
今天几乎每天都有新的 AI 工具上 GitHub 热搜，初级工程师常常陷入严重的“技术迷茫与工具焦虑”。
高阶架构师看技术，永远看的是层次和坐标！在 2026 年，整个 AI Agent 生态已经沉淀为极其清晰的【七层技术栈体系】：
- **L1 物理算力与芯片层**：NVIDIA GPU、国产算力、HBM 显存带宽；
- **L2 推理加速与服务层**：vLLM、SGLang、Ollama、TensorRT-LLM（解决高并发吞吐）；
- **L3 基础模型层**：DeepSeek、Qwen、Claude、GPT（提供基座推理智能）；
- **L4 协议与互操作层**：MCP（系统工具）、A2A（智能体协同）、AG-UI（动态前端）；
- **L5 智能体框架与编排层**：LangGraph、Spring AI、Semantic Kernel、原生状态机 FSM；
- **L6 评估与可观测性底座**：LangSmith、Phoenix、OpenTelemetry、Agent Evals（质量与监控）；
- **L7 垂直业务智能体应用**：我们的智能导购 Agent、金融风控 Agent 等。
无论明天 GitHub 推出一个多么花哨的新库，直接问自己：**它处于第几层？解决了哪一层的什么问题？** 焦虑瞬间消失！

## Key Q&A / Clarifications
**Q：企业在实际落地时，应该在哪一层投入最大的研发精力？**
A：L1 到 L3 由芯片厂和模型大厂负责，企业不应重复造轮子；企业的核心护城河永远在 **L5（编排逻辑）、L6（评测与监控）与 L7（垂直业务壁垒与私有知识）**！

## Transition
当产品经理拿着一个具体业务需求找到你时，你该如何决定 Agent 的技术选型？看第 17 页架构师决策树。
