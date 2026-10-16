# Docker Compose 一键部署

把 7 个服务（3 个 Spring Boot + 前端 + MySQL + Redis + PostgreSQL/pgvector）编排为一键启动。

## 前置要求

- Docker + Docker Compose v2（Docker Desktop 自带）
- 硅基流动 API Key（https://cloud.siliconflow.cn）

## 快速启动

```bash
cd enterprise-ai-agent

# 1. 配置 API Key
cp .env.example .env
vi .env    # 填入 SILICONFLOW_API_KEY

# 2. 一键启动（首次构建约 5-10 分钟，含 Maven/npm 依赖下载）
docker compose up -d --build

# 3. 打开页面
# http://localhost:3000
```

初始化全自动：
- MySQL 首次启动执行 `docs/schema.sql`（建表 + 26 个商品 + 演示账号 demo/demo123）
- PostgreSQL 首次启动执行 `docs/schema-knowledge.sql`（pgvector 扩展 + vector_store/user_memory 表）
- 商品知识库仍需在页面点「加载商品知识」手动索引（教学设计，不是自动化遗漏）
- 启动顺序无需关心：MCP Client 内置懒重连（10s 冷却），agent-runtime 与 MCP Server 谁先就绪都能自动接上

## 服务清单

| 服务 | 内部端口 | 宿主机端口 | 说明 |
|------|---------|-----------|------|
| web (nginx) | 80 | **3000** | 页面入口：静态文件 + /api 反代 |
| agent-runtime | 8080 | 8080（调试） | Agent 主服务 |
| mcp-product-server | 8081 | 不暴露 | 商品 MCP Server |
| mcp-knowledge-server | 8082 | 不暴露 | 知识/记忆 MCP Server |
| MySQL | 3306 | 不暴露 | user / user_profile / product |
| Redis | 6379 | 不暴露 | 会话工作缓冲 |
| PostgreSQL | 5432 | 不暴露 | vector_store / user_memory |

## 为什么用 nginx

1. **托管前端静态文件**（vue build 产物），不用在容器里跑 dev server
2. **反代 `/api` 到 agent-runtime**，替代开发环境的 vue proxy，单入口免 CORS
3. **SSE 流式保护**：`proxy_buffering off` + 600s 超时——不关缓冲会复现「（无响应）」问题（响应被 nginx 缓存或中途掐断）

## 常用操作

```bash
docker compose ps                  # 查看状态
docker compose logs -f agent-runtime   # 看 Agent 日志（演示容错降级就盯这个）
docker compose stop mcp-knowledge-server   # 演示容错降级：停知识服务
docker compose start mcp-knowledge-server  # 恢复
docker compose down                # 停止（数据卷保留）
docker compose down -v             # 停止并清空数据（重新初始化）
```

## 构建说明

- 三个 Java 服务共用 `docker/Dockerfile.java`，用 `MODULE` 构建参数区分，Maven 依赖走 BuildKit 缓存（`--mount=type=cache`），代码改动不重新下载依赖
- 前端 `docker/Dockerfile.web`：node:20 打包 → nginx:1.27 托管
- 基础镜像：`maven:3.9-eclipse-temurin-17`（构建）/ `eclipse-temurin:17-jre-jammy`（运行）/ `pgvector/pgvector:pg16`（向量库）

## 与手动部署的关系

`Demo演示步骤.md` 的四终端启动方式仍然有效，二选一：
- **课堂演示**：建议 docker compose 一键起环境，再用终端 `docker compose logs -f agent-runtime` 盯日志
- **开发调试**：手动起服务，改代码即时生效

## 故障排查

| 现象 | 处理 |
|------|------|
| 页面能开但发消息无响应 | `docker compose logs agent-runtime` 看报错；确认 .env 里 API Key 有效 |
| 启动报数据库连接失败 | 首次初始化较慢，`docker compose ps` 等 MySQL/PG 变 healthy 后 `docker compose up -d` 再拉一次 |
| RAG 查不到商品 | 页面点「加载商品知识」手动索引 |
| 改了 SQL 想重新初始化 | `docker compose down -v && docker compose up -d --build`（会清空数据） |
| 构建慢 | 首次正常；Maven/npm 均有镜像层缓存，增量构建只需 1-2 分钟 |
