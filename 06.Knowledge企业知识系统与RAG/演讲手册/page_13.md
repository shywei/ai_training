## Slide Objective
展示 context 的演进过程——从 v1.0 到 v3.0，每课填充什么。学员需要理解"Agent 能力是逐步增强的"，以及 v3.0 context 的四部分是怎么组装的。

## Suggested Presentation
context 是整个 Agent 的核心参数。第四课留了扩展点：`buildUserPrompt(message, context)` 的 context 参数，v1.0 传 null。每课逐步填充，Agent 能力逐步增强。

看上面的四格对比。

v1.0（第四课）：context = null。纯 Prompt，没有上下文。每次对话都是陌生人。Agent 能回答一般问题，但不记得用户，也不知道商品信息。

v2.0（第五课）：context = 用户画像 + 对话历史。Agent 记住用户了——知道用户喜欢什么、之前聊过什么。但不知道商品信息，用户问"有什么推荐的笔记本"答不上来。

v3.0（第六课）：context = 用户画像 + 对话历史 + RAG 检索结果 + 相关记忆。Agent 能查商品了——知道有什么商品、什么配置、适合什么场景。这是现在的状态。

v4.0（第七课）：context = ... + Tool 调用结果。Agent 能查库存、下单。从"能说"到"能做"。

看右边的代码。`MemoryManager.buildContext()` 方法，四部分同时注入：

① 用户画像（MySQL，始终查询）。`profileMapper.selectOne()` 按 userId 查 MySQL，返回 facts/preferences/tasks/constraints。这是 v2.0 的能力，v3.0 保留。

② 相关记忆（pgvector，始终查询）。`memoryService.search()` 按用户消息做语义检索，找到历史对话中和当前问题相关的片段。有结果就注入"相关记忆"标题下。这是 v3.0 新增的能力——自动沉淀机制把 Redis 的对话历史沉淀到 pgvector。

③ 对话历史（Redis，沉淀后为空）。`sessionMemory.getMessages()` 读取当前会话的消息。自动沉淀后这部分会很短。

④ 参考知识（pgvector，关键词过滤后检索）。`retrievalService.retrieve()` 做商品知识检索。先过关键词过滤，再做向量搜索。

Token 预算分配：画像 10%、历史 30%、知识 40%、余量 20%。知识占比最大，因为这是本课的核心价值。生产中需要根据模型的 Context Window 动态调整。

## Key Emphasis
学员应该理解"Agent 能力是逐步增强的"——不是一步到位，而是每课加一层。context 演进是这个过程的具体体现。每加一层，Agent 的能力就上一个台阶。

## Transition
context 的组成清楚了。但还有一个关键问题：检索结果怎么注入 Prompt？
