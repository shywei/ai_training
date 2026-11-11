## Slide Objective
讲清记忆保存和记忆使用的分工——系统行为 vs LLM 行为。这是记忆系统设计的核心决策。

## Suggested Presentation
记忆系统有两个操作：保存（写入）和使用（读取）。关键是分清谁负责什么。

### 记忆保存是系统行为

不通过 LLM FunctionCallback 调用。两个触发时机：

**自动沉淀**：对话满 20 条消息时触发（`MemoryManager.PRECIPITATE_THRESHOLD = 20`）。`MemoryManager.checkAndPrecipitate()` 检查 Redis 中的消息数量，达到阈值后：① 从 Redis 取出消息，截取前 10 条 ② 调用 LLM 从这 10 条中提取关键信息（`extractConversationSummary()`） ③ 通过 `McpKnowledgeClient.saveMemory()` → MCP → pgvector 写入 ④ Redis 裁剪，保留后 10 条（`KEEP_RECENT = 10`）——不是全清，最近的历史上下文还在，下次对话还能带上。

**清空会话**：用户点击清空时触发。`MemoryManager.clearSession()` 提取剩余消息并保存，然后清空 Redis。

为什么不让 LLM 调 save_memory？三个原因：① 时机不可控——LLM 可能在不恰当的时候调用（用户说"今天天气不错"也保存）② 可能不调——用户说了重要信息但 LLM 没意识到要保存 ③ 幂等性问题——LLM 可能重复调用，导致同一条记忆存多次。自动沉淀机制保证了：只要对话够长，一定会保存，且只保存一次。

### 记忆使用是 LLM 行为

每次对话时，`MemoryManager.buildContext()` 从 pgvector 语义检索相关记忆，注入到 context 的「相关记忆」段。LLM 看到记忆后，结合记忆了解用户的真实需求。

系统提示词引导 LLM：`「参考信息」中的用户记忆，记录了用户之前的需求和决策。推荐商品时，结合记忆了解用户的真实需求。` 这条规则告诉 LLM 记忆有什么用，但不告诉 LLM 怎么保存记忆。

### 画像是更高层的抽象与工具结果可信度治理

画像（user_profile MySQL）由 `UserProfileExtractor` 从对话中提炼结构化标签（facts + preferences）。画像和记忆的关系：记忆是"对话中发生了什么"（原始记录），画像是"用户是谁"（提炼后的结论）。画像从多轮记忆中提炼，不是每次对话都更新。

**【重点澄清：工具结果不可天然视作事实】**
工具调用结果并不天然等于“绝对客观且不可篡改的事实”。外部 API、第三方服务可能返回偶发错误、过期数据甚至脏数据。因此，工具调用结果应作为**带数据源、时间戳和调用上下文的事实证据记录（Evidence）**；必须经过业务逻辑校验、置信度评估与第 05 课的记忆全生命周期准入治理（Mem0 冲突消解、偏好覆写、合规被遗忘权）后，才允许作为高价值结论沉淀进长期用户画像。

Demo 中画像抽取由关键词"记住"触发，这是简化。实际企业中，画像是由专门的画像系统维护的，通过批量任务（每天定时从近期对话中抽取）、事件驱动（会话结束、订单完成后触发）、或流处理（Kafka 实时分析对话标签）来更新。

### 对比 queryProductInfo

queryProductInfo 是 LLM 主动调用的 Tool（用户问价格，LLM 决定查）。save_memory 是系统调用的 Tool（对话沉淀，系统决定存）。两者的控制权不同：一个由 LLM 决策，一个由系统决策。

## Key Emphasis
记忆保存是系统行为，记忆使用是 LLM 行为。这个分工保证了记忆的可靠性（不依赖 LLM 的判断）和灵活性（LLM 可以自由使用记忆上下文）。

## Demo 与生产的差距
Demo 的自动沉淀阈值是硬编码的 20 条消息（`PRECIPITATE_THRESHOLD = 20`，沉淀后保留 `KEEP_RECENT = 10` 条在 Redis）。生产中应该根据对话频率动态调整——高频对话可以降低阈值，低频对话可以提高。Demo 的 LLM 提取摘要用的是同一个 ChatModel，生产中可能用更便宜的模型做摘要（降低 token 成本）。Demo 没有做记忆去重——同一个偏好被多次提到会存多条，生产中需要去重或合并。

## Optional Extension
**学员问：为什么不让 LLM 自己决定什么时候调 save_memory？它不是更"智能"吗？**
答：因为保存是"必须恰好发生一次"的写操作，而 LLM 的决策不可控、不可审计——时机不可控（闲聊也存）、覆盖不可靠（重要信息没意识到要存）、重复执行（同一条存多次）。
Demo 做法：保存权在系统——`MemoryManager.checkAndPrecipitate()` 按消息数阈值触发沉淀，`extractConversationSummary()` 提取摘要后 `McpKnowledgeClient.saveMemory()` 写入，save_memory 不注册给 LLM；使用权才给 LLM（`buildContext()` 检索注入，`PromptManager` 记忆使用规则教它用）。
生产做法：写入补幂等（同一批消息重复沉淀不能重复入库——Demo 的 `MemoryService` 是裸 INSERT，重复调用会存多条）+ 提取用便宜模型降成本 + 语义去重合并策略。取舍：系统行为牺牲"即时记住"的灵活性换可靠性——真要即时性走事件驱动触发（会话结束/订单完成），也不要交给 LLM 判断。

**学员问：沉淀阈值该怎么定？Demo 里是写死的常量。**
答：Demo 用 `MemoryManager` 的阈值常量教学就够了；生产按对话节奏动态调——高频对话降低阈值防丢信息，低频提高阈值省提取成本，再配会话级上限，防止阈值被恶意会话刷爆。

## Take Away
记忆的写入权给系统、读取权给 LLM——凡是"必须恰好发生一次"的写操作（沉淀、扣减、通知），都不能让 LLM 决定时机和次数。

## Why This Slide Matters
跳过这页，学员会让 LLM 持有 save_memory——记忆时存时不存、重复存、无法追责（LLM 为什么调没有审计），数据质量问题反过来污染画像和推荐。

## Transition
现在把第七课的数据分层完整过一遍。
