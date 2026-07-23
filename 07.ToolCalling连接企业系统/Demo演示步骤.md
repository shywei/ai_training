# 第七课 Demo 演示步骤

> 讲师上课时的操作手册，照着做就行。

---

## 演示前准备

### 1. 基础环境确认

```bash
# 确认 MySQL (3306), Redis (6379), PostgreSQL (5432) 均已启动
redis-cli ping
mysql -u root -e "SHOW DATABASES LIKE 'agent_db'"
```

### 2. 物理表与初始数据准备

确认商品表已经包含 26 个商品，并包含实时价格/库存信息：

*   **MySQL 业务数据库**：`user` 表、`user_profile` 表、`product` 实时表（由 `docs/schema.sql` 完成）。
*   **PostgreSQL 向量数据库**：`vector_store` 商品描述表、`user_memory` 个人记忆表（由 `docs/schema-knowledge.sql` 完成）。

### 3. 微服务配置

确认 `agent-runtime/src/main/resources/application.yml` 中成功配置了两个 MCP Server 的微服务通信端点：

```yaml
mcp:
  product:
    server-url: http://localhost:8081
  knowledge:
    server-url: http://localhost:8082
```

---

## 启动顺序（极为重要 🚨）

为了保证握手协议能正常发现微服务提供的工具，请务必按以下顺序依次开启终端启动服务：

1.  **终端 1（MCP 知识库微服务）**：
    ```bash
    cd mcp-knowledge-server
    mvn spring-boot:run
    # 启动时若 vector_store 为空，会从 products.json 自动执行 12 个商品的 Embedding 手动索引
    ```
2.  **终端 2（MCP 商品微服务）**：
    ```bash
    cd mcp-product-server
    mvn spring-boot:run
    # 端口为 8081
    ```
3.  **终端 3（Agent 运行期主服务）**：
    ```bash
    cd agent-runtime
    ./mvnw spring-boot:run
    # 端口为 8080，启动时会自动请求 8081/8082 端的 SSE 进行握手与 Tool 发现
    ```
4.  **终端 4（前端界面）**：
    ```bash
    cd web
    npm run dev
    # 访问 http://localhost:3000
    ```

---

## 演示流程

### 演示 1：微服务启动与 MCP Client 握手协议（2 分钟）

**操作：**
1. 观察**终端 3（agent-runtime）** 的启动控制台日志。

**预期结果：**
*   启动成功后，控制台会打印握手发现的所有工具列表：
    ```text
    [MCP] 连接 MCP Server 成功: http://localhost:8081
    [MCP] 连接 MCP Server 成功: http://localhost:8082
    ```

**讲解要点：**
> *   **标准化协议的好处**：与上一节课 Agent 直连数据库耦合不同，现在 Agent 成了 MCP Client。启动时会自动发送 `initialize` 与 8081/8082 端完成协议握手。开发中，我们不需要重新编写任何向量检索代码，只需像调用本地函数一样自动执行动态包装，让整个微服务架构完美解耦。
> *   **哪些 Tool 暴露给 LLM**：只有 product-server 的 queryProductInfo/Price/Stock 包装为 FunctionCallback 注册给 LLM。knowledge-server 的 search_knowledge、search_memory 由系统内部调用（context 构建），save_memory 由系统自动沉淀调用，都不暴露给 LLM。

---

### 演示 2：LLM 路由（意图判定）与按需工具注册（3 分钟）

**操作：**
1. 登录前端界面，依次在聊天框输入三个代表性问题：
   *   输入 A：“`你好，请问你是？`”
   *   输入 B：“`有什么好用的开发本推荐？`”
   *   输入 C：“`拯救者电脑现在多少钱，还有货吗？`”
2. 观察控制台的 `[Router]` 日志输出。

**预期结果：**
*   **控制台日志**：
    ```text
    输入 A -> [Router] 意图判定: CHAT
    输入 B -> [Router] 意图判定: KNOWLEDGE (仅注入 RAG 知识)
    输入 C -> [Router] 意图判定: TOOL (注入 mcp_queryProductInfo 等本地 Callback)
    ```

**讲解要点：**
> *   **确定性路由设计**：通过 `LlmRouter.java` 我们调用快模型进行三分类，**必须设置 temperature = 0**。这样可以强制模型进行确定性输出。
> *   **性能优化细节**：只有当路由判定为 `TOOL` 时，才会在当前调用中向 LLM 请求注册 `mcp_queryProductInfo` 工具。普通闲聊或知识检索时不需要注册，能极大地节省请求首字延迟并降低 Token 计费。

---

### 演示 3：实时查询：MySQL 与 pgvector 的数据分层协作（3 分钟）

**操作：**
1. 在聊天框提问：“`帮我查一下拯救者 Y9000P 还有货吗？我想买一部。`”

**预期结果：**
*   Agent 能够精准识别并返回：“`拯救者 Y9000P 当前售价 8999 元，库存 58 件，在售。`”

**日志观察（agent-runtime）：**
*   查看后端日志或 `logs/llm-chat.log` 包含的响应内容：
    ```text
    [Router] 意图判定: TOOL
    [LLM call] 返回 tool_calls 指令: {"name":"mcp_queryProductInfo","arguments":"{\"productId\":\"NB003\"}"}
    [System] 执行 mcp-product-server 中的 MySQL 查询...
    [System] 回传结果: NB003 价格 8999 元，库存 58 件
    [LLM response] 整合结果生成：拯救者 Y9000P 当前售价 8999 元，库存 58 件...
    ```

**讲解要点：**
> *   **数据分层协作的精髓**：静态的拯救者电脑描述和亮点存在 pgvector 中通过向量检索；但波动频繁的实时价格与库存存在 MySQL 中。当 LLM 被判定需要做事时，会通过 Function Calling 动态查询 MySQL，让静态知识与实时业务数据实现完美协作。

---

### 演示 4：记忆自动沉淀（系统行为）（3 分钟）

**操作：**
1. 连续对话多轮（超过10条消息），触发自动沉淀
2. 观察控制台日志

**预期结果：**
*   对话达到10条消息时，系统自动触发记忆沉淀

**日志观察（agent-runtime）：**
*   控制台日志：
    ```text
    [Memory] 消息数 10 达到阈值 10，触发自动沉淀
    [MCP-Knowledge] 保存记忆: userId=zhangsan, type=conversation
    [MCP-Knowledge] 记忆保存成功
    [Memory] ✅ 自动沉淀完成
    [Memory] Redis 已清空
    ```
*   **PostgreSQL 验证（命令行运行）**：
    ```bash
    psql -U postgres -d agent_db -c "SELECT content FROM user_memory WHERE user_id = 'zhangsan';"
    # 能够看到系统自动沉淀的对话摘要
    ```

**讲解要点：**
> *   **记忆保存是系统行为**：save_memory MCP Tool 存在，但不暴露给 LLM。由 MemoryManager 在对话达到阈值时自动调用。
> *   **画像 vs 记忆**：画像（user_profile MySQL）由 UserProfileExtractor 从对话中提炼结构化标签；记忆（user_memory pgvector）由系统自动沉淀对话事实。画像 > 记忆：画像是结论，记忆是过程。

---

## 常见问题排查

### Q: 启动 agent-runtime 报错 `Connection refused` 到 8081 或 8082
*   **原因**：未按照启动顺序先开启 `mcp-product-server` 或 `mcp-knowledge-server`。
*   **解决**：必须先启动两个微服务，然后再启动主服务。不过，如果它们断开，Demo 设计了优雅降级（控制台报警但服务不崩溃，普通对话依然能跑）。

### Q: 问了价格，但大模型直接自己编了个价格，没有触发 Tool 调用
*   **原因**：路由阶段温度过高，或者问法不够具体，被路由判定为了 `CHAT` 或 `KNOWLEDGE`。
*   **解决**：检查控制台 `[Router]` 的路由输出。多说几次“多少钱”、“是否有货”，或者在 Prompt 中强化行为规则。

### Q: RAG 检索结果不准确（Demo 未覆盖的优化）
*   **原因**：Demo 直接用用户原文做向量检索，短 query（如“多少钱”）缺少产品上下文。
*   **生产优化**：
    *   **Query Rewrite**：LLM 先抽取意图生成结构化 query，再做检索
    *   **Metadata Filtering**：metadata 存结构化标签，查询时先过滤再向量搜索
    *   **Rerank**：用交叉编码器对 Top-K 结果精排
*   Demo 跳过了这些步骤，学员做项目时需要补上。

---

## 演示时间分配

| 演示阶段 | 演示内容 | 耗时 |
|:---|:---|:---|
| 演示前准备 | 4 终端微服务架构图与端口说明 | 1 分钟 |
| 演示 1 | 握手握手握手，SSE 发现 Tools | 2 分钟 |
| 演示 2 | 闲聊/知识/工具的三意图判定展示 | 3 分钟 |
| 演示 3 | 查价格/库存 MySQL 与 RAG 的数据分层协作 | 3 分钟 |
| 演示 4 | 记忆自动沉淀（系统行为） | 2 分钟 |
| **合计** | | **11 分钟** |
