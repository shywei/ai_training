## Slide Objective
讲解跨路由 Tool 的设计决策和 Prompt 配套。

## Suggested Presentation
`queryProductInfo` 只在 TOOL 路由注册——只有用户问价格/库存时才需要。这很合理，CHAT 路由下注册 product tools 是浪费 token。

但 `save_memory` 不一样。用户在任何对话中都可能表达偏好：
- CHAT 路由："我预算8000" → 应该保存偏好
- KNOWLEDGE 路由："我喜欢轻薄本" → 应该保存偏好
- TOOL 路由："这个太贵了" → 应该保存偏好

如果 `save_memory` 只在 TOOL 路由注册，CHAT 和 KNOWLEDGE 路由下 LLM 调不了。

所以 `save_memory` 是**跨路由注册**的——所有路由都注册。代码：`clientBuilder.functions(mcpKnowledgeClient.getFunctionCallbacks())` 在路由判断之外执行。

但光注册不够。LLM 不知道"什么时候该保存记忆"。如果用户说"今天天气不错"，LLM 也调 save_memory 就不对了。

所以 `PromptManager.java` 加了「记忆使用规则」：
```
- 当用户表达偏好、需求、约束时（如"我喜欢轻薄本""预算8000"），
  主动调用 save_memory 工具保存，memory_type 用 preference
- 不需要每次都保存记忆，只保存有价值的信息
```

这告诉我们：**Tool + Prompt = 完整的 Agent 能力**。暴露 Tool 是第一步，告诉 LLM 什么时候用是第二步。没有 Prompt 配套，LLM 可能乱调用或不调用。

对比：`queryProductInfo` 不需要 Prompt 配套——LLM 看到"多少钱"自然知道要查价格。但 `save_memory` 需要——"我预算8000"不是明确的"请记住"指令，LLM 需要 Prompt 引导才知道应该保存。

## Key Emphasis
跨路由 Tool 是更灵活的设计。Prompt 配套是必须的——不是所有 Tool 都像"查价格"那么直觉，有些需要 Prompt 引导 LLM 正确使用。

## Transition
现在把第七课的数据分层完整过一遍。
