# Enterprise AI Agent

企业导购 Agent —— 从 Prompt 到平台的完整演进 Demo。

## 快速启动

### 方式一：Docker Compose 一键部署（推荐）

```bash
cp .env.example .env   # 填入 SILICONFLOW_API_KEY
docker compose up -d --build
# 访问 http://localhost:3000
```

自动完成数据库初始化（建表 + 演示数据），详见 [Docker 部署指南](docs/docker-setup.md)。

### 方式二：本地手动启动

```bash
# 1. 配置 API Key
vi agent-runtime/src/main/resources/application-local.yml

# 2. 启动后端（需要 Redis + MySQL + PostgreSQL）
cd agent-runtime && ./mvnw spring-boot:run

# 3. 启动 MCP Server（两个独立进程）
cd mcp-product-server && mvn spring-boot:run   # 端口 8081
cd mcp-knowledge-server && mvn spring-boot:run  # 端口 8082

# 4. 启动前端
cd web && npm install && npm run dev

# 5. 访问 http://localhost:3000
```

详见各版本部署指南：
- [v1.0 部署指南](docs/v1.0-setup.md) — LLM 交互
- [v2.0 部署指南](docs/v2.0-setup.md) — Memory 记忆系统
- [v3.0 部署指南](docs/v3.0-setup.md) — Knowledge 知识系统与 RAG + 记忆增强
- [v4.0 部署指南](docs/v4.0-setup.md) — Tool Calling + LLM 路由

## 架构（v4.0 MCP 化）

```
┌─────────────────────────────────────────────────────┐
│                   前端 (Vue) :3000                   │
└──────────────────────┬──────────────────────────────┘
                       │ SSE
┌──────────────────────▼──────────────────────────────┐
│              agent-runtime (MCP Client) :8080        │
│                                                      │
│  LlmRouter → KNOWLEDGE / TOOL / CHAT                │
│                                                      │
│  context = 画像(MCP Resource) + 记忆(MCP Tool) + 历史(Redis)  │
│          + RAG(MCP Tool,仅KNOWLEDGE) + Tool结果(仅TOOL)     │
│                                                      │
│  记忆通过系统自动沉淀（不暴露给 LLM）        │
└───────┬──────────────────────────┬───────────────────┘
        │ MCP (SSE)                │ MCP (SSE)
        ▼                          ▼
┌───────────────────┐   ┌──────────────────────────────┐
│ mcp-product-server│   │  mcp-knowledge-server        │
│ :8081             │   │  :8082                       │
│                   │   │                              │
│ Tool:             │   │ Tool:                        │
│  queryProductInfo │   │  search_knowledge (RAG)      │
│  queryPrice       │   │  search_memory   (记忆检索)  │
│  queryStock       │   │  save_memory     (记忆写入)  │
│                   │   │  index_knowledge (知识索引)  │
│                   │   │                              │
│ MySQL             │   │ Resource:                    │
│ (价格/库存/状态)  │   │  user://{userId} (用户画像)  │
│                   │   │                              │
│                   │   │ PostgreSQL + pgvector        │
│                   │   │ MySQL (画像)                 │
│                   │   │ (商品向量 + 用户记忆向量)     │
└───────────────────┘   └──────────────────────────────┘
```

### 数据分层

| 数据 | 存储 | 访问方式 | MCP Server |
|------|------|---------|------------|
| 用户画像 | MySQL | Resource: user://{userId}（会话开始加载） | mcp-knowledge-server |
| 用户记忆 | pgvector | Tool: search_memory（系统自动沉淀） | mcp-knowledge-server |
| 商品知识 | pgvector | Tool: search_knowledge | mcp-knowledge-server |
| 商品实时数据 | MySQL | Tool: queryProductInfo 等 | mcp-product-server |

### 记忆架构

```
用户画像（user_profile MySQL）：用户是谁（提炼后的结构化标签）
  → UserProfileExtractor 从对话中抽取 → 写入 MySQL
  → 通过 MCP Resource 读取

用户记忆（user_memory pgvector）：对话中发生了什么（事实记录）
  → MemoryManager 自动沉淀（10条消息触发）→ 通过 MCP 写入 pgvector
  → 通过 MCP Tool 语义搜索

画像 > 记忆：画像是结论，记忆是过程。画像从多轮记忆中提炼。
```

### Tool vs Resource

| MCP 概念 | 用途 | Demo 中的使用 |
|----------|------|--------------|
| Tool | 动态操作（搜索、写入） | search_knowledge / search_memory / save_memory / index_knowledge / queryProductInfo |
| Resource | 静态上下文数据 | user://{userId} 用户画像（从 MySQL 读取） |

## 项目结构

```
enterprise-ai-agent/
├── agent-runtime/               # 后端（Spring Boot, MCP Client）
│   └── src/main/java/com/example/agent/
│       ├── core/                # v1.0 核心组件
│       │   ├── PromptManager.java          # System Prompt + User Prompt
│       │   ├── StructuredOutputHandler.java
│       │   ├── GuardrailsService.java
│       │   ├── ShoppingAgentController.java # 主控制器（路由 + Tool 注册 + 演示 API）
│       │   └── LlmInteractionLogger.java
│       ├── memory/              # v2.0 记忆组件
│       │   ├── SessionMemory.java       # Redis 短期记忆
│       │   ├── MemoryManager.java       # context 构建 + 自动沉淀（全部通过 MCP）
│       │   ├── UserProfile.java         # 用户画像实体
│       │   ├── UserProfileExtractor.java # 画像抽取（LLM → MySQL）
│       │   └── UserProfileMapper.java   # MySQL 画像操作
│       └── tool/                # v4.0 Tool 组件
│           ├── LlmRouter.java          # LLM 路由 + 模型选择（整合 ModelRouter）
│           ├── McpProductClient.java    # MCP Client（商品查询）
│           ├── McpKnowledgeClient.java  # MCP Client（知识检索+记忆+画像+索引）
│           │   - search() / searchMemory() / saveMemory() / readUserProfile() / indexKnowledge()
│           └── ProductApiController.java # 商品 REST API（调试用）
├── mcp-product-server/          # MCP Server（商品查询, :8081）
│   └── src/main/java/com/example/mcp/product/
│       ├── McpServerConfig.java         # 3 Tool: queryProductInfo/Price/Stock
│       └── ProductRepository.java       # MySQL product 表查询
├── mcp-knowledge-server/        # MCP Server（知识+记忆+画像, :8082）
│   └── src/main/java/com/example/knowledge/
│       ├── McpServerConfig.java         # 4 Tool + 1 Resource
│       ├── EmbeddingService.java        # SiliconFlow Embedding API
│       ├── VectorSearchService.java     # 商品知识 pgvector 检索
│       ├── MemoryService.java           # 用户记忆 pgvector 读写
│       ├── UserProfileService.java      # 用户画像 MySQL 读取
│       ├── MysqlDataSourceConfig.java   # MySQL 数据源配置
│       └── KnowledgeIndexer.java        # 知识库自动+手动索引
├── web/                         # 前端（Vue 2 + Element UI）
└── docs/                        # 建表 SQL + 各版本部署指南
```

## 技术栈

- **后端**: Java 17 + Spring Boot 3.x + Spring AI 1.0.0-M4
- **MCP**: MCP Java SDK 1.1.3（io.modelcontextprotocol.sdk:mcp）
- **前端**: Vue 2 + Element UI + marked
- **存储**: Redis（Session）+ MySQL（用户/商品结构化数据）+ PostgreSQL + pgvector（商品描述向量 + 用户记忆向量）
- **Embedding**: BAAI/bge-large-zh-v1.5（SiliconFlow 免费）
- **LLM**: 硅基流动（Qwen2.5-7B / Qwen3-8B）

## 演进路线

| Tag | 课程 | 核心能力 | 状态 |
|-----|------|----------|------|
| v1.0-prompt | 第四课 | LLM 交互 + Prompt + 结构化输出 + Self-Correction | ✅ 完成 |
| v2.0-memory | 第五课 | Session + 滑动窗口 + 摘要压缩 + 用户画像 | ✅ 完成 |
| v3.0-knowledge | 第六课 | 知识库 RAG + 记忆增强（向量检索） | ✅ 已完成 |
| v4.0-tool | 第七课 | Tool Runtime + LLM路由 + Function Calling + MCP 协议 | ✅ 已完成 |
| v5.0-workflow | 第八课 | 状态机 + Agent Runtime + Human-in-the-loop | ⬜ |
| v6.0-framework | 第九课 | Spring AI 重构 + 框架对比 | ⬜ |
| v7.0-platform | 第十课 | 接入平台组件 + 多 Agent 共享 | ⬜ |

## context 演进

每课逐步填充 `PromptManager.buildUserPrompt(message, context)` 的 context 参数：

```
v1.0: context = null
v2.0: context = 对话历史 + 用户画像（MySQL 精确查询）
v3.0: context = 对话历史 + 用户记忆（pgvector 语义检索）+ RAG 知识检索
v4.0: context = 对话历史 + 用户记忆（MCP Tool）+ RAG（MCP Tool,仅KNOWLEDGE路由）+ Tool调用结果（仅TOOL路由）
       用户画像通过 MCP Resource 加载，记忆通过系统自动沉淀（非 LLM 调用）
```

v3.0 的核心变化：同一个 pgvector 基础设施，既支撑记忆检索（"这个用户喜欢什么"），又支撑知识检索（"有什么商品推荐"）。

v4.0 的核心变化：
- LLM 路由决定是否注入 RAG/Tool，不再自动注入所有 context
- 记忆和知识统一通过 MCP 协议访问，Agent 不直接耦合向量数据库
- save_memory 作为 FunctionCallback 暴露给 LLM，支持主动保存用户偏好

## 设计决策

每个版本的技术选型和踩坑经验，详见各版本 decisions 文档：
- [v1.0 设计决策](docs/v1.0-prompt-decisions.md)
- [v2.0 设计决策](docs/v2.0-memory-decisions.md)
- [v3.0 设计决策](docs/v3.0-knowledge-decisions.md)
- [v4.0 设计决策](docs/v4.0-tool-decisions.md)

## 第七课教学资料

- [PPT](../07.ToolCalling连接企业系统/) — 10 页 HTML 幻灯片
- [演讲手册](../07.ToolCalling连接企业系统/演讲手册/) — 逐页讲解词
- [部署指南](docs/v4.0-setup.md) — 启动顺序、配置说明、演示流程
- [设计决策](docs/v4.0-tool-decisions.md) — 技术选型和踩坑经验
