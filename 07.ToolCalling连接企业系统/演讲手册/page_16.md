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

## Transition
先看 LLM 路由——有了 Tool 之后，怎么决定调哪个。
