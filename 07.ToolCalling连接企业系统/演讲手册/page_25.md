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

## Transition
下节课见。
