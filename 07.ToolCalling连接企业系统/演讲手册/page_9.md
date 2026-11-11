## Slide Objective
建立 MCP 协议的认知：为什么需要标准协议、不用 MCP 的问题是什么。

## Suggested Presentation
不用 MCP 的做法：Tool 代码直接写在 Agent 项目里。`ShoppingAgentController` 里直接调 `RetrievalService`（查 pgvector）、直接调 `UserProfileMapper`（查 MySQL）。能用，但有问题。

第一个问题：**耦合**。Agent 和数据库绑定。换数据库（比如从 pgvector 换到 Milvus），Agent 代码要改。第二个问题：**不可复用**。每个 Agent 都要写一遍同样的 Tool 逻辑。导购 Agent 写了查商品的代码，客服 Agent 要再写一遍。第三个问题：**不可独立部署**。Tool 逻辑和 Agent 逻辑在同一个进程里，不能独立扩缩容。商品查询是高频操作，需要多部署几个实例，但 Agent 本身不需要。

MCP 的做法：Tool 独立部署为 MCP Server，Agent 作为 MCP Client 通过标准协议调用。一次开发，到处复用。换框架只改 Client，Server 不变。换 Agent，MCP Server 通用。

类比：REST API 之于 Web 服务，MCP 之于 AI Agent 和 Tool。有了标准协议，生态才能繁荣。

MCP 是 Anthropic 2024 年底提出的开放标准，已开源（Apache 2.0 协议）。不是 Anthropic 独占——Claude Desktop、Cursor、Windsurf、OpenClaw 都支持。MCP 正在成为 AI Tool 的事实标准。

Demo 中有两个 MCP Server：mcp-product-server（端口 8081）提供商品查询 Tool，mcp-knowledge-server（端口 8082）提供知识检索、记忆服务和用户画像。agent-runtime 作为 MCP Client，不直接访问任何数据库。

## Key Emphasis
MCP 的核心价值不是"技术先进"，而是"标准化"。标准化意味着：Tool 可以复用、可以独立部署、可以被任何支持 MCP 的 Agent 调用。这是企业架构的基本原则。

## Optional Extension
**学员问：我们只有一个 Agent、两三个 Tool，值得上 MCP 吗？**
答：按消费方数量判断，不按技术新旧。MCP 摊薄的是集成成本，只有一个消费方时它摊不薄。
Demo 做法：两个 MCP Server（8081 商品、8082 知识）+ agent-runtime 作 Client，`McpKnowledgeClient` / `McpProductClient` 只走协议不碰数据库——双 Server 是为了演示复用与独立部署这两个收益点。
生产做法：一个消费方 → 直连 + 治理三件（超时/幂等/审计）即可；两个以上消费方（多个 Agent、外部系统、低代码平台）或需要独立扩缩容/独立权限边界 → 上 MCP；工具规模化后由 MCP Gateway 统一注册/鉴权/限流/审计（第十一课支柱一）。取舍：MCP 多一跳网络，换来的是换库不改消费方、Server 独立扩缩容。

## Take Away
上不上 MCP 的判据是"这个能力有几个消费方"——单消费方直连最省，两个以上（或多 Agent、多端）才用协议摊薄集成与治理成本。

## Why This Slide Matters
跳过这页，学员会走向两个极端：给单 Agent 单工具也搭 MCP 全家桶（多一层网络和运维没有复用收益），或把该共享的工具焊死在 Agent 进程里、第二个消费方来时被迫重构。

## Transition
MCP 协议定义了四大能力：Tool、Resource、Prompt、Sampling。每个能力解决不同的问题。
