# 第六课 Demo 演示步骤

> 讲师上课时的操作手册，照着做就行（对应 Git 标签 `v1.3` 单体架构 `agent-runtime`）。

---

## 演示前准备

### 0. 检出对应版本源码（关键前置步骤）

```bash
# 必须先切换到第六课对应的代码标签（v1.3 单体架构集成 Knowledge 与 RAG）
cd enterprise-ai-agent
git checkout v1.3
```

### 1. 环境与扩展检查

```bash
# 确认 PostgreSQL 服务已启动（默认端口 5432）
# 进入 PostgreSQL 命令行，确认已启用 pgvector 扩展：
psql -U postgres -d agent_db -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

### 2. 数据库表初始化

确认 MySQL 业务数据库与 PostgreSQL 向量数据库建表均已完成：

*   **MySQL（结构化存储）**：用户画像表 `user_profile`、商品表 `product`（`docs/schema.sql`，提供初始数据）
*   **PostgreSQL（向量存储）**：商品知识向量表 `vector_store` 与用户语义记忆表 `user_memory`（`docs/schema-knowledge.sql`）

### 3. 配置确认

编辑 `agent-runtime/src/main/resources/application.yml`（或 `application-local.yml`），确认新增的 PostgreSQL 独立数据源和 Embedding 模型配置已加载：

```yaml
spring:
  datasource:
    # MySQL 主数据源（用户画像与结构化商品）
    url: jdbc:mysql://localhost:3306/agent_db?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root
    # PostgreSQL 向量数据库独立数据源（VectorStoreConfig.java 读取）
    pgvector:
      url: jdbc:postgresql://localhost:5432/agent_db
      username: postgres
      password: postgres
      driver-class-name: org.postgresql.Driver

  # Embedding 向量化模型（Spring AI OpenAI 兼容接口调用 SiliconFlow）
  ai:
    openai:
      embedding:
        options:
          model: BAAI/bge-large-zh-v1.5 # 1024 维度
```

### 4. 启动后端与前端

```bash
# 终端 1：启动 Spring Boot（v1.3 单体 agent-runtime，端口 8080）
cd agent-runtime
./mvnw spring-boot:run

# 终端 2：启动前端
cd web
npm run dev
# 访问 http://localhost:3000
```

---

## 演示流程

### 演示 1：商品知识库数据索引（3 分钟）

**操作：**
1. 启动后端后，使用命令行工具（如 Git Bash 或 Curl）触发手动索引接口（`KnowledgeController.java`）：
   ```bash
   curl -X POST http://localhost:8080/api/knowledge/index
   ```
2. 观察控制台的日志打印，也可通过 `curl http://localhost:8080/api/knowledge/status` 查看索引状态。

**预期结果：**
*   接口返回成功，控制台打印类似如下日志：
    ```text
    [Knowledge] 开始索引 26 个商品...
    [Knowledge] [1/26] Embedding: ThinkPad X1 Carbon Gen12
    [Knowledge] [2/26] Embedding: MacBook Pro 14 M3 Pro
    ...
    [Knowledge] ✅ 索引完成，共 26 个商品
    ```

**讲解要点：**
> *   **手动索引设计**：在 Demo 中通过 `KnowledgeController`（`POST /api/knowledge/index`）与 `KnowledgeService` 手动触发，能让学员非常清晰地理解 **Data Pipeline（数据管道）** 的运作过程。它先解析 `products.json`（26 个商品、4 个品类），然后通过 Spring AI `EmbeddingModel`（`BAAI/bge-large-zh-v1.5`）把文本转换为 1024 维的浮点数组，最后经 `PgVectorStore` 写入 PostgreSQL 的 `vector_store` 表。
> *   **维度对齐坑**：强调 BGE 输出的向量是 1024 维，对应的 PG 建表语句里必须也是 `embedding vector(1024)`，如果写成 1536 或 3072 会导致查询匹配失败，排错极难。

---

### 演示 2：RAG 语义检索与推荐（3 分钟）

**操作：**
1. 访问 `http://localhost:3000` 并登录（例如 `zhangsan/123456`）。
2. 在聊天框中提问：“`我经常需要出差和写代码，有什么推荐的笔记本电脑？`”

**预期结果：**
*   Agent 会通过 `RetrievalService.retrieve()` 从商品库检索，并给出匹配推荐（如优先推荐轻薄的 ThinkPad X1 Carbon 或 MacBook Pro），而且能引用具体的处理器和重量指标进行解释。

**日志观察（agent-runtime）：**
*   打开 `logs/llm-chat.log` 观察最新的一次调用日志：
    ```text
    [Knowledge] 检索成功，召回文档数: 3
    [System Prompt] (System Prompt 中注入了知识使用规则：严禁自己捏造商品和配置)
    [Context] 包含：
      ## 参考知识
      [1] ThinkPad X1 Carbon ... 超轻机身 1.08kg，适合商务出行。
      [2] MacBook Pro 14 ... 续航持久，适合编程开发。
    ```

**讲解要点：**
> *   **Prompt 知识使用规则与数据隔离**：大模型训练数据包含过期的网络信息。要让它听话地使用最新的公司产品，我们在 `PromptManager.java` 里加入了规则指令：“`推荐商品必须基于商品库知识，不要编造商品配置，不确定时如实告知。`”不加这些指令，LLM 会忽略检索内容直接幻觉。

---

### 演示 3：关键词匹配跳过检索（2 分钟）

**操作：**
1. 在聊天框输入通用闲聊：“`你好呀`” 或者 “`今天天气不错`”。

**预期结果：**
*   Agent 迅速进行常规的问答回复，后台没有进行向量查询。

**日志观察（agent-runtime）：**
*   观察控制台：没有输出任何 `[Knowledge] 检索成功` 等日志。
*   打开 `logs/llm-chat.log`，可以看到最新请求的 `[Context]` 字段中根本没有出现 `## 参考知识` 这一段，说明检索没有被触发。

**讲解要点：**
> *   **检索拦截的设计意图**：如果每个简单的“你好”都要去调 Embedding 模型算相似度并去查向量库，会浪费 Token 预算，且无关知识塞进 Context 也会干扰模型的语义理解。在 `v1.3` Demo 中，`RetrievalService.isProductRelated()` 使用轻量级关键词匹配进行过滤拦截；在下一课（第七课 `v1.4`）中，我们将进一步演进为 `LlmRouter` 意图分类器。

---

### 演示 4：历史对话自动沉淀与语义记忆检索（4 分钟）

**操作：**
1. 在聊天框中输入：“`记住我平时还做视频剪辑，希望显卡性能强一些。`”
2. 继续多轮对话触发自动沉淀（或点击前端的「清空会话」按钮验证跨会话记忆）。
3. 退出登录张三，然后重新登录张三。
4. 输入：“`给我推荐几款电脑。`”

**预期结果：**
*   Agent 能直接通过 `UserMemoryService.search()` 从 pgvector `user_memory` 表召回历史语义记忆，回复：“`您之前提到经常做视频剪辑，需要显卡性能强的电脑。我为您推荐...`”。

**日志观察（agent-runtime）：**
*   查看后端日志或 `logs/llm-chat.log`：
    ```text
    [Memory] 从 pgvector 中检索到用户 zhangsan 的相关语义记忆。
    [Context] 包含了：
      ## 相关记忆
      用户在历史会话中曾表示：平时还做视频剪辑，希望显卡性能强一些。
    ```

**讲解要点：**
> *   **非结构化记忆的沉淀**：很多原始对话语境无法被粗暴分类进 MySQL 用户画像（`user_profile` 只能存结构化偏好、事实结论标签）。
> *   因此，在 `v1.3` 中我们新增了 `UserMemoryService.java` 与 `UserMemoryMapper.java`，并在 `MemoryManager.java` 中利用 pgvector 实现了 **非结构化语义记忆** 的沉淀（`checkAndPrecipitate()`），把对话原文片段转为向量存入 `user_memory` 表，从而实现了跨会话的语义细节追踪。

---

## 常见问题排查

### Q: 索引数据时报错 `Relation "vector_store" does not exist`
*   **原因**：PostgreSQL 对应的数据库 `agent_db` 未建表，或者表结构定义没在当前 Database 下执行。
*   **解决**：登录 PG，选择 `agent_db` 数据库，运行 `docs/schema-knowledge.sql` 进行建表。

### Q: 报错 `MySQL 不支持 Vector 字段` 或类似方言错误
*   **原因**：`VectorStoreConfig.java` 中配置的数据源注入失效，Spring AI 自动装配将其默认绑定到了 MySQL 主业务库。
*   **解决**：检查 `application.yml` 配置的 `spring.datasource.pgvector` 是否拼写正确，并确认 `VectorStoreConfig` 里的 Bean 声明已成功将 PostgreSQL 独立数据源注入给 `PgVectorStore` 实例。

---

## 演示时间分配

| 演示阶段 | 演示内容 | 耗时 |
|:---|:---|:---|
| 演示前准备 | 向量库表定义与 MySQL 数据分层对比 | 1 分钟 |
| 演示 1 | 知识库手动触发索引（`POST /api/knowledge/index` 日志观察） | 3 分钟 |
| 演示 2 | RAG 语义检索与 Prompt 整合（`RetrievalService` 验证） | 3 分钟 |
| 演示 3 | 闲聊触发关键词过滤（`isProductRelated()` 跳过检索验证） | 2 分钟 |
| 演示 4 | 历史消息向 pgvector `user_memory` 沉淀与跨 Session 语义召回 | 4 分钟 |
| **合计** | | **13 分钟** |
