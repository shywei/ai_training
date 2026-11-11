## Slide Objective
总结第七课三个核心能力，预告第八课。

## Suggested Presentation
第七课学了三个核心能力：

**LLM 路由**——Agent 判断意图，决定该做什么。不是所有对话都走同一个流程。KNOWLEDGE 注入 RAG，TOOL 注册 FunctionCallback，CHAT 直接回复。路由是 Agent "智能"的体现。

**MCP 协议**——Tool 的标准化暴露和调用。Tool 独立部署，标准协议，任何 Agent 都能调。MCP 正在成为 AI Tool 的事实标准。

**Function Calling**——LLM 主动调用工具。LLM 不只是回答问题，还能决定"该做什么"。底层是 ReAct 模式：推理→行动→观察→循环。注意：不是所有 MCP Tool 都暴露给 LLM。Demo 中只有 queryProductInfo/Price/Stock 暴露给 LLM，save_memory 由系统内部调用。

Agent 能力演进：
- 第四课：能"说话"（LLM 交互 + Streaming + 结构化输出）
- 第五课：能"记住"（Session + 摘要 + 用户画像）
- 第六课：能"理解"（RAG + pgvector + 记忆增强）
- 第七课：能"做事"（MCP + Function Calling + 路由）
- 第八课：能"规划"（状态机 + 多步骤编排）← 下节课

第八课预告：状态机 + Agent Runtime + Human-in-the-loop。第七课的 Agent 能"做事"了，但还是一问一答——用户问一句，Agent 答一句。第八课要让 Agent 能完成复杂的多步骤任务——"帮我对比三款笔记本，选出最合适的，然后下单"。中间需要多步推理、人工确认、错误恢复。这是从"对话式 Agent"到"任务式 Agent"的跃迁。

## Key Emphasis
三个能力缺一不可：路由决定做什么，MCP 提供能力，Function Calling 让 LLM 调用。缺少任何一个，Agent 都不能"做事"。

## Optional Extension
**学员问：第八课的 Agent Runtime 和今天讲的 Function Calling 到底差在哪？今天不也能调工具了吗？**
答：差在"谁推进流程"。今天是一问一答：tool_call 执行完就出最终回复；Agent Runtime 是任务驱动的循环，中间结果决定下一步。
Demo 做法：今天的链路里 Spring AI 执行完 tool_call 就生成最终回复；第八课 `AgentRuntime.execute()` 是 THINK→ACT→OBSERVE 循环（`MAX_ITERATIONS = 10` 封顶），加 `TaskPlanner` 规划、`ErrorRecovery` 三级容错、`ConfirmationService` 人在回路、`TraceService` 全程追踪。
生产做法：任务式 Agent 上线要配执行预算（轮数/时间/token）、失败恢复（重试/熔断/降级，第八课讲透参数取舍）、写操作确认与审计。取舍：能一问一答解决的别上循环——循环给了 LLM 决策自由度，也给了它跑偏的空间，预算和 Trace 是缰绳。后续对齐课程线：框架怎么接管这些胶水在第九课（AgentScope Java 2.0），多 Agent 编排（Router / Supervisor-Worker / Handoff）在第十课。

## Take Away
判断"对话式"该升"任务式"的信号：一次请求需要多步工具调用且中间结果影响下一步——出现这个信号就上 Agent Runtime，否则把逻辑堆进 Prompt 只是便宜的正确。

## Why This Slide Matters
跳过这页，学员会把多步骤任务硬塞进一问一答（一次规划全部 tool_call），中间步骤失败无法恢复、无法确认，也分不清"该补的能力去哪课补"，后续学习一路带着错误的架构假设。

## Transition
下节课见。

## 课后扩展学习导读（深水区进阶指引）
对于希望在企业级工具调用、微服务集成与运行时安全领域进一步深潜的学员，本课根目录下特别沉淀了一份高质量的进阶资料：[扩展学习指南.md](../扩展学习指南.md)。
- **核心定位**：第七课让 Agent 迈出了从“能说”到“能做”的关键一步。课堂代码 `v1.4` 跑通了 Spring AI 与 MCP 的核心调用，而《扩展学习指南.md》遵循 3W 精选导读法则，专为解决大规模生产环境下的工程治理与安全红线而编写。
- **四大核心主题导读**：
  1. **MCP 规范演进与微服务边界**：深入官方规范站（modelcontextprotocol.io），对比 2025-11-25 会话式 Streamable HTTP 与 2026-07-28 Stateless Core 无状态核心规范，拆解 MRTR 多轮工具请求机制，剖析 MCP Java SDK 2.2.0 中 Classic 与 Modern 源码实现差异。
  2. **大规模工具集治理（Tool RAG）**：面对企业 500+ API 时，如何通过两阶段匹配架构（语义粗排向量检索 + 上下文精排与权限剪枝）动态挂载 Top-5 工具 Schema，彻底避免上下文撑爆与工具误选（Tool Confusion）。
  3. **分布式工具调用治理五件套**：深入 Redis + Lua 分布式原子令牌桶限流、Idempotency-Key 幂等三态租约状态机（防并发重入与超时重试雪崩），以及 Resilience4j 熔断降级在 Tool Runtime 中的深度集成。
  4. **间接提示词注入防御（Indirect Prompt Injection）**：剖析 OWASP LLM 排名第一的致命攻击链，研读微软 Spotlighting（arXiv:2403.14720）数据标记隔离技术（Delimitation, Data Marking, Enclosure），掌握工具返回数据的强类型校验与权限隔离。
- **自测检验**：指南末尾准备了 3 道高水准架构思考题（含 Tool RAG 误触发申请退款根治、弱网库存锁定超时重试风暴排查、跨工具链间接注入纵深防御），请学员课后精读指南并尝试独立思考作答！
