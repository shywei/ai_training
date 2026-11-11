## Slide Objective
过渡页——告诉学员接下来的内容不是新概念，而是有了 Tool/MCP 之后，第六课的知识体系怎么升级。

## Suggested Presentation
前面 15 页讲了第七课的核心主题：Function Calling（LLM 怎么调工具）、MCP 协议（工具怎么标准化暴露）、Tool vs Resource（工具的两种形态）。这些是全新的概念。

接下来的内容不是新概念，而是**有了这些能力之后，第六课的知识体系怎么升级**。

**LLM 路由**：第六课每次都自动注入 RAG，不管用户问什么。有了 Tool/MCP 之后，我们可以让 LLM 判断意图，按需注入——问价格时调 Tool，问推荐时注入 RAG，闲聊时什么都不加。路由不是第七课的新概念，是 Tool 能力带来的架构升级。

**记忆系统设计**：第六课的记忆读写直连 pgvector（UserMemoryMapper）。有了 MCP 之后，记忆读写统一走协议——MemoryManager 通过 McpKnowledgeClient 调用 MCP Server。同时明确了记忆保存是系统行为（自动沉淀），不是 LLM 行为。

**数据分层升级**：第六课的 Agent 直连 MySQL 和 pgvector。有了 MCP 之后，所有数据访问统一通过协议——画像走 MCP Resource，记忆走 MCP Tool，商品知识走 MCP Tool，实时数据走 MCP Tool。Agent 不直接碰任何数据库。

**context 演进**：第六课的 context 是自动拼接所有信息。有了路由之后，context 按需构建——KNOWLEDGE 注入 RAG，TOOL 注册 FunctionCallback，CHAT 只有画像+记忆+历史。

这四页展示的是：**有了新能力之后，旧系统怎么变好**。这是工程课的特点——不是学完概念就结束，要看概念怎么落地到已有系统中。

## Key Emphasis
这四页不是"第七课的附加内容"，而是"第六课到第七课的演进"。学员应该看到：同样的业务需求（导购推荐），有了 Tool/MCP 之后，架构变了多少。

## Optional Extension
**学员问：路由、记忆、数据分层第六课都讲过，这四页的"升级"到底升在哪？**
答：升在访问路径和注入策略，不在能力本身。能力没变，边界变了。
Demo 做法：`MemoryManager.buildContext()` 里画像走 `McpKnowledgeClient.readUserProfile()`（MCP Resource）、记忆走 `searchMemory()`（MCP Tool）；RAG 只在 KNOWLEDGE 路由才注入——第六课是"每次都注入"，第七课是"按需注入"。
生产做法：注入策略带 token 预算和优先级（画像>记忆>历史>检索，本课后面 context 页展开），context 构建异步预取 + 缓存。取舍：按需注入省 token，代价是路由判错就缺上下文——所以路由要有保守的默认值（后面路由页讲）。第八课 `AgentRuntime` 会把 context 构建挪进执行循环入口，作为每轮 THINK 的共享底料。

## Take Away
验收一次架构升级的标准是"能力不变、路径变好"——用同一组问题对比回答质量和依赖清单，就能验证升级没跑偏、没丢功能。

## Why This Slide Matters
跳过这页，学员会把第六课直连架构当"过时要扔"、把 MCP 化当新功能往上堆——丢掉"能力不变"这个验收锚点，升级变成重写，回归无从谈起。

## Transition
先看 LLM 路由——有了 Tool 之后，怎么决定调哪个。
