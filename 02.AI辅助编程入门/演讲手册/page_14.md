## Slide Objective
深入权衡 MCP 与本地 CLI 的边界，讲清 MCP 的 Token 放大陷阱，建立务实的工具集成架构观。

## Suggested Presentation
请看第 14 页：支柱 ⑤ · MCP 与本地 CLI 的边界权衡。
2025–2026 年，Anthropic 开源的【MCP（Model Context Protocol，模型上下文协议）】成为了智能体连接外部世界的通用标准协议。通过 MCP，Agent 能像插拔 USB 一样连接 GitHub、飞书、PostgreSQL、Jira 和内部内部系统。
但在享受 MCP 便利的同时，高级架构师必须警惕它的【Token 经济学陷阱】！
每个接入的 MCP Server，都会把自身包含的所有工具名称、参数 JSON Schema、字段描述一次性全部塞进 System Prompt。如果你一口气接了 10 个 MCP Server，光是工具定义就能吞噬 15,000 个 Token，不仅每轮调用都要反复付费，更容易引发严重的工具调用幻觉！
**因此，请牢记 2026 的边界权衡法则：**
- **优先使用本地 CLI**：对于本机的 `git`、`mvn`、`gh`、`curl`、`jq`，直接通过 Bash / Terminal 执行，零额外 Token 开销，速度极快；
- **必要接入企业 MCP**：只在需要连接带鉴权的企业私域系统（如查询内部生产数据库脱敏视图、读取飞书知识库、同步 Jira 任务）时，才按需挂载专用的企业 MCP Server。

## Key Q&A / Clarifications
**Q：在第 07 课中我们还会深入学习 MCP 吗？**
A：是的！第 07 课《ToolCalling连接企业系统》将全面带大家手写原生的 MCP Client 与 Server，并实现 Tool RAG（按需检索工具，彻底解决工具定义 Token 膨胀问题）。

## Transition
六件套的最后一根支柱，是解决单 Agent 上下文被翻页日志撑爆的王牌武器：Subagents。
