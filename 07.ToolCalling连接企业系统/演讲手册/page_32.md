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

**企业治理五件套**：限流、幂等、超时、补偿、审计。

一句话带走：**Agent 的能力 = LLM 的决策 + Tool 的执行 + 标准协议的连接。**

## Key Emphasis
能力地图要让学员看到"我学了什么、还有什么没学"。进度图要让学员看到"我走到哪了"。一句话带走要让学员记住核心思想。

## Optional Extension
**学员问：能力地图六个模块，哪个是上线前的硬门槛？**
答：企业治理。前五个模块决定"能不能做"，治理决定"做错了兜不兜得住"。
Demo 做法：`v1.4` Demo 覆盖了 Function Calling / MCP / 路由 / 数据分层，治理只有客户端超时（`requestTimeout`）、断线冷却重连（`ensureConnected`）与控制台调用日志（`McpToolCallback`）——下一课第八课我们将补上重试熔断（`ErrorRecovery`）、人工确认（`ConfirmationService`）与内存追踪（`TraceService`），而五件套里的幂等、限流、补偿与持久化审计则是接生产系统必须补齐的硬门槛。
生产做法：五件套逐件落地（本课治理专页：幂等键挡重试、审计包补偿、限流站入口），平台期收进 MCP Gateway 统一注册/鉴权/RBAC/审计（第十一课支柱一）；多 Agent 场景的治理归第十课的编排层。取舍：按写接口优先做，读接口可以先只上超时 + 限流。

## Take Away
用能力地图当上线 checklist：能力层全绿只代表功能做完，治理层全绿才代表可以上线——两列分开打分，别用一列的进度冒充另一列。

## Why This Slide Matters
跳过这页，学员会以"功能都实现了"作为完成标准——治理层一片空白就上生产，第一个写接口事故（重复下单、打挂下游、无法追责）才暴露清单缺一列。

## Transition
能力地图对完号，用下一页的 8 道课后练习把今天的内容变成自己的，然后总结第七课、预告第八课。
