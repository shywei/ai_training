# Enterprise AI Agent

企业导购 Agent —— 从 Prompt 到平台的完整演进 Demo。

## 快速启动

```bash
# 1. 配置 API Key
vi agent-runtime/src/main/resources/application-local.yml

# 2. 启动后端（需要 Redis + MySQL + PostgreSQL）
cd agent-runtime && ./mvnw spring-boot:run

# 3. 启动前端
cd web && npm install && npm run dev

# 4. 访问 http://localhost:3000
```

详见各版本部署指南：
- [v1.0 部署指南](docs/v1.0-setup.md) — LLM 交互
- [v2.0 部署指南](docs/v2.0-setup.md) — Memory 记忆系统
- [v3.0 部署指南](docs/v3.0-setup.md) — Knowledge 知识系统与 RAG + 记忆增强

## 项目结构

```
enterprise-ai-agent/
├── agent-runtime/               # 后端（Spring Boot）
│   ├── pom.xml
│   ├── mvnw
│   └── src/main/
│       ├── java/com/example/agent/
│       │   ├── core/                # v1.0 核心组件
│       │   │   ├── ModelRouter.java
│       │   │   ├── PromptManager.java
│       │   │   ├── StructuredOutputHandler.java
│       │   │   ├── GuardrailsService.java
│       │   │   ├── ShoppingAgentController.java
│       │   │   └── LlmInteractionLogger.java
│       │   ├── memory/              # v2.0 记忆组件 + v3.0 记忆增强
│       │   │   ├── SessionMemory.java       # Redis 短期记忆
│       │   │   ├── SummaryPipeline.java     # 摘要压缩
│       │   │   ├── MemoryManager.java       # context 构建（含语义检索）
│       │   │   ├── UserProfile.java         # 用户画像实体
│       │   │   ├── UserProfileExtractor.java # 画像抽取 + pgvector 存储
│       │   │   ├── UserProfileMapper.java   # MySQL 画像操作
│       │   │   ├── UserMemory.java          # v3.0 用户记忆实体
│       │   │   ├── UserMemoryMapper.java    # v3.0 pgvector 记忆操作
│       │   │   └── UserMemoryService.java   # v3.0 记忆存储与语义检索
│       │   └── knowledge/           # v3.0 知识组件
│       │       ├── VectorStoreConfig.java
│       │       ├── KnowledgeService.java
│       │       └── RetrievalService.java
│       └── resources/
│           ├── application.yml
│           ├── logback-spring.xml
│           └── products.json        # v3.0 商品数据
├── docs/
│   ├── schema.sql               # 用户表
│   ├── schema-knowledge.sql     # v3.0 pgvector 建表（知识库+用户记忆）
│   ├── v1.0-setup.md
│   ├── v1.0-prompt-decisions.md
│   ├── v2.0-setup.md
│   ├── v2.0-memory-decisions.md
│   ├── v3.0-setup.md
│   └── v3.0-knowledge-decisions.md
└── web/                         # 前端（Vue 2 + Element UI）
    ├── package.json
    └── src/App.vue
```

## 技术栈

- **后端**: Java 17 + Spring Boot 3.x + Spring AI 1.0.0-M4
- **前端**: Vue 2 + Element UI + marked
- **存储**: Redis（Session）+ MySQL（用户/商品结构化数据）+ PostgreSQL + pgvector（商品描述向量 + 用户记忆向量）
- **Embedding**: BAAI/bge-large-zh-v1.5（SiliconFlow 免费）
- **LLM**: 硅基流动（Qwen2.5-7B / Qwen3-8B）

## 演进路线

| Tag | 课程 | 核心能力 | 状态 |
|-----|------|----------|------|
| v1.0-prompt | 第四课 | LLM 交互 + Prompt + 结构化输出 + Self-Correction | ✅ 完成 |
| v2.0-memory | 第五课 | Session + 滑动窗口 + 摘要压缩 + 用户画像 | ✅ 完成 |
| v3.0-knowledge | 第六课 | 知识库 RAG + 记忆增强（向量检索） | 🔨 进行中 |
| v4.0-tool | 第七课 | Tool Runtime + MCP Server + 企业治理 | ⬜ |
| v5.0-workflow | 第八课 | 状态机 + Agent Runtime + Human-in-the-loop | ⬜ |
| v6.0-framework | 第九课 | Spring AI 重构 + 框架对比 | ⬜ |
| v7.0-platform | 第十课 | 接入平台组件 + 多 Agent 共享 | ⬜ |

## context 演进

每课逐步填充 `PromptManager.buildUserPrompt(message, context)` 的 context 参数：

```
v1.0: context = null
v2.0: context = 对话历史 + 用户画像（MySQL 精确查询）
v3.0: context = 对话历史 + 用户记忆（pgvector 语义检索）+ RAG 知识检索
v4.0: context = 对话历史 + 用户记忆 + RAG + Tool 调用结果
```

v3.0 的核心变化：同一个 pgvector 基础设施，既支撑记忆检索（"这个用户喜欢什么"），又支撑知识检索（"有什么商品推荐"）。

## 设计决策

每个版本的技术选型和踩坑经验，详见各版本 decisions 文档：
- [v1.0 设计决策](docs/v1.0-prompt-decisions.md)
- [v2.0 设计决策](docs/v2.0-memory-decisions.md)
- [v3.0 设计决策](docs/v3.0-knowledge-decisions.md)
