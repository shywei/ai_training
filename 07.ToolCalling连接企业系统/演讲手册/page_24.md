## Slide Objective
展示 Agent 构建进度和第七课能力地图，给学员全局视角。

## Suggested Presentation
Agent 构建进度：第四课 LLM 交互 ✓，第五课 Memory ✓，第六课 Knowledge ✓，第七课 Tool Calling ✓。第八课 Workflow 待完成。

每一课给 Agent 增加了一个能力层：第四课让它能"说话"（LLM 交互），第五课让它能"记住"（Memory），第六课让它能"理解"（Knowledge/RAG），第七课让它能"做事"（Tool Calling）。

第七课能力地图，六个知识模块：

**Function Calling**：概念（LLM 返回调用指令）、两种实现（@Tool vs FunctionCallbackWrapper）、选型理由（版本决定）。

**MCP 协议**：四大能力（Tool/Resource/Prompt/Sampling）、三层架构（Client/Transport/Server）、传输选型（SSE vs stdio）。

**LLM 路由**：三种方案（关键词/Embedding/LLM）、选型策略（意图数量决定）、实现代码。

**数据分层**：pgvector（知识/记忆）、MySQL（画像/实时数据）、MCP 统一访问。

**Tool vs Resource**：选型决策树、跨路由设计、Prompt 配套。

**企业治理**：权限、幂等、超时、审计、安全。

一句话带走：**Agent 的能力 = LLM 的决策 + Tool 的执行 + 标准协议的连接。**

## Key Emphasis
能力地图要让学员看到"我学了什么、还有什么没学"。进度图要让学员看到"我走到哪了"。一句话带走要让学员记住核心思想。

## Transition
来总结第七课，预告第八课。
