# Enterprise AI Agent

企业导购 Agent —— 从 Prompt 到平台的完整演进 Demo。

## 快速启动

```bash
# 1. 配置 API Key
vi agent-runtime/src/main/resources/application-local.yml

# 2. 启动后端
cd agent-runtime
./mvnw spring-boot:run

# 3. 启动前端（新终端）
cd web
npm install
npm run dev

# 4. 访问 http://localhost:3000
```

详见 [v1.0 部署指南](docs/v1.0-setup.md)

## 项目结构

```
enterprise-ai-agent/
├── agent-runtime/               # 后端（Spring Boot）
│   ├── pom.xml
│   ├── mvnw
│   └── src/main/
│       ├── java/.../core/       # 业务代码
│       └── resources/           # 配置文件
│
└── web/                         # 前端（Vue 2 + Element UI）
    ├── package.json
    ├── vue.config.js            # dev server 代理配置
    ├── public/index.html
    └── src/
        ├── main.js
        └── App.vue              # 聊天界面
```

## 技术栈

- **后端**: Java 17 + Spring Boot 3.x + Spring AI
- **前端**: Vue 2 + Element UI
- **LLM**: 硅基流动（免费模型）

## 演进路线

| Tag | 课程 | 核心能力 | 状态 |
|-----|------|----------|------|
| v1.0-prompt | 第四课 | LLM 交互 + Prompt + 结构化输出 + Self-Correction | 🔨 进行中 |
| v2.0-memory | 第五课 | Session + 滑动窗口 + 摘要压缩 + 用户画像 | ⬜ |
| v3.0-knowledge | 第六课 | Data Pipeline + RAG + 混合检索 | ⬜ |
| v4.0-tool | 第七课 | Tool Runtime + MCP Server + 企业治理 | ⬜ |
| v5.0-workflow | 第八课 | 状态机 + Agent Runtime + Human-in-the-loop | ⬜ |
| v6.0-framework | 第九课 | Spring AI 重构 + 框架对比 | ⬜ |
| v7.0-platform | 第十课 | 接入平台组件 + 多 Agent 共享 | ⬜ |
