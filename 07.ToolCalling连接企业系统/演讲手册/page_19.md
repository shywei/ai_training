## Slide Objective
讲清记忆保存和记忆使用的分工——系统行为 vs LLM 行为。这是记忆系统设计的核心决策。

## Suggested Presentation
记忆系统有两个操作：保存（写入）和使用（读取）。关键是分清谁负责什么。

### 记忆保存是系统行为

不通过 LLM FunctionCallback 调用。两个触发时机：

**自动沉淀**：对话达到 10 条消息时触发。`MemoryManager.checkAndPrecipitate()` 检查 Redis 中的消息数量，超过阈值后：① 从 Redis 取出所有消息 ② 调用 LLM 从对话中提取关键信息（`extractConversationSummary()`） ③ 通过 `McpKnowledgeClient.saveMemory()` → MCP → pgvector 写入 ④ 清空 Redis。

**清空会话**：用户点击清空时触发。`MemoryManager.clearSession()` 提取剩余消息并保存，然后清空 Redis。

为什么不让 LLM 调 save_memory？三个原因：① 时机不可控——LLM 可能在不恰当的时候调用（用户说"今天天气不错"也保存）② 可能不调——用户说了重要信息但 LLM 没意识到要保存 ③ 幂等性问题——LLM 可能重复调用，导致同一条记忆存多次。自动沉淀机制保证了：只要对话够长，一定会保存，且只保存一次。

### 记忆使用是 LLM 行为

每次对话时，`MemoryManager.buildContext()` 从 pgvector 语义检索相关记忆，注入到 context 的「相关记忆」段。LLM 看到记忆后，结合记忆了解用户的真实需求。

系统提示词引导 LLM：`「参考信息」中的用户记忆，记录了用户之前的需求和决策。推荐商品时，结合记忆了解用户的真实需求。` 这条规则告诉 LLM 记忆有什么用，但不告诉 LLM 怎么保存记忆。

### 画像是更高层的抽象

画像（user_profile MySQL）由 `UserProfileExtractor` 从对话中提炼结构化标签（facts + preferences）。画像和记忆的关系：记忆是"对话中发生了什么"（原始记录），画像是"用户是谁"（提炼后的结论）。画像从多轮记忆中提炼，不是每次对话都更新。

Demo 中画像抽取由关键词"记住"触发，这是简化。实际企业中，画像是由专门的画像系统维护的，通过批量任务（每天定时从近期对话中抽取）、事件驱动（会话结束、订单完成后触发）、或流处理（Kafka 实时分析对话标签）来更新。

### 对比 queryProductInfo

queryProductInfo 是 LLM 主动调用的 Tool（用户问价格，LLM 决定查）。save_memory 是系统调用的 Tool（对话沉淀，系统决定存）。两者的控制权不同：一个由 LLM 决策，一个由系统决策。

## Key Emphasis
记忆保存是系统行为，记忆使用是 LLM 行为。这个分工保证了记忆的可靠性（不依赖 LLM 的判断）和灵活性（LLM 可以自由使用记忆上下文）。

## Demo 与生产的差距
Demo 的自动沉淀阈值是硬编码的 10 条消息。生产中应该根据对话频率动态调整——高频对话可以降低阈值，低频对话可以提高。Demo 的 LLM 提取摘要用的是同一个 ChatModel，生产中可能用更便宜的模型做摘要（降低 token 成本）。Demo 没有做记忆去重——同一个偏好被多次提到会存多条，生产中需要去重或合并。

## Transition
现在把第七课的数据分层完整过一遍。
