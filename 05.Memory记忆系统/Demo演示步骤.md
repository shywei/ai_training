# 第五课 Demo 演示步骤

> 讲师上课时的操作手册，照着做就行。

---

## 演示前准备

### 1. 环境检查

```bash
# 确认 Java 17+
java -version

# 确认 Node.js 16+
node -v

# 确认 Redis 正常运行
redis-cli ping

# 确认 MySQL 正常运行
mysql -u root -e "SHOW DATABASES LIKE 'agent_db'"
```

### 2. 数据库初始化

如果尚未建表，请使用 MySQL 客户端运行项目目录下的 SQL 脚本初始化数据库：

```bash
# 1. 执行基础用户表 SQL（包含初始用户：zhangsan/123456）
mysql -u root agent_db < docs/schema.sql

# 2. 补全用户画像表 user_profile 建表 SQL（若 schema.sql 未包含）：
mysql -u root agent_db -e "
CREATE TABLE IF NOT EXISTS user_profile (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     VARCHAR(64)  NOT NULL UNIQUE COMMENT '用户ID',
    facts       TEXT         COMMENT '事实:姓名、职业等',
    preferences TEXT         COMMENT '偏好:颜色、品牌、预算等',
    tasks       TEXT         COMMENT '任务:当前需求、待办',
    constraints TEXT         COMMENT '约束:禁区、限制',
    updated_at  BIGINT       COMMENT '更新时间戳',
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户画像表';
"
```

### 3. 配置确认

编辑 `enterprise-ai-agent/agent-runtime/src/main/resources/application-local.yml`，确认 Redis 和 MySQL 的连接信息正确：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
  datasource:
    url: jdbc:mysql://localhost:3306/agent_db?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root
```

### 4. 切换本课代码版本并启动服务（按顺序）

打开终端，确认切换到第五课基线版本 `v1.2.1` 并执行以下命令：

```bash
# 切换到第五课对应的代码版本 v1.2.1
cd enterprise-ai-agent
git checkout v1.2.1

# 终端 1：启动后端（端口 8080）
cd agent-runtime
./mvnw spring-boot:run

# 终端 2：启动前端（端口 3000）
cd web
npm run dev
# 访问 http://localhost:3000
```

---

## 演示流程

### 演示 1：用户登录与会话初始化（1 分钟）

**操作：**
1. 打开浏览器访问 `http://localhost:3000`。
2. 系统会自动重定向到登录页面。
3. 输入用户名 `zhangsan`，密码 `123456`，点击登录。

**预期结果：**
*   登录成功后进入主界面，右上角显示当前登录的用户“张三”。
*   前端欢迎框弹出，并生成当前用户的动态 `sessionId`。

**讲解要点：**
> *   **为什么实战课需要登录**：与普通聊天应用不同，企业级记忆系统（Memory）的长期记忆必须绑定唯一的 `userId`。只有通过登录鉴权拿到 `userId`，我们才能在 MySQL 中持久化和检索该用户的长期画像（UserProfile）。

---

### 演示 2：Session Memory 短期记忆与上下文注入（2 分钟）

**操作：**
1. 在聊天框中输入：“`你好，我是张三，我的职业是程序员。`”
2. 随后输入：“`请问我刚才说我的职业是什么？`”

**预期结果：**
*   模型能够准确回忆：“你刚才说你的职业是程序员。”

**日志观察（agent-runtime）：**
*   打开 `logs/llm-interactions.log`（或 `logs/llm-chat.log`）观察最新的一次调用日志，或调用 `GET /api/agent/memory?sessionId=...` 查看当前会话缓冲：
    ```text
    [Memory Injection] sessionId=...
    ## 对话历史
    用户: 你好，我是张三，我的职业是程序员。
    助手: 你好！张三。已记下您的职业。
    ```

**讲解要点：**
> *   **短期记忆的底层原理**：大模型接口本身是无状态的（Stateless）。我们是在 Java 后端（`SessionMemory.java`）中将每轮对话追加到 Redis 的 List 结构（`session:{sid}:messages`）中。每次调用大模型前，`MemoryManager.buildContext()` 从 Redis 取出历史消息（及累积摘要），拼装作为 context 注入到 User Prompt 中，以此建立“短期记忆”。

---

### 演示 3：对话累积与摘要压缩（4 分钟）

**操作：**
1. 连续进行多轮简短对话，直到会话消息条数超过 **8 条**（`COMPRESS_THRESHOLD = 8`，即往返对话 4 次以上）。

**预期结果：**
*   系统自动触发 `SummaryPipeline` 完成累积摘要压缩（保留最近 4 条原文 `KEEP_RECENT = 4` + 累积摘要），回答流畅，且不会丢失早期对话的核心背景。

**日志观察（agent-runtime）：**
*   **控制台与摘要日志（`logs/llm-summary.log`）**：
    ```text
    [Memory] Session xxx 消息过多，触发摘要压缩
    [Summary] Session xxx 压缩完成: 前N-4条 → 1条累积摘要 + 4条原文
    ```
    打开 `logs/llm-summary.log` 可直观看到 `SUMMARY REQUEST`（含旧摘要与待压缩对话）及 `SUMMARY RESPONSE`（快模型生成的最新累积摘要）。
*   **Redis 查看（终端运行）**：
    ```bash
    redis-cli LRANGE session:default:messages 0 -1
    # 观察输出（含累积摘要与最近 4 条原文）：
    # 1) "system|[对话摘要] 用户名为张三，职业为程序员..."
    # 2) "user|最近一条消息..."
    ```

**讲解要点：**
> *   **累积摘要的精髓**：随着对话增长，Token 成本成指数上升，且会撑爆窗口限制。我们通过 `SummaryPipeline.java` 判断当消息超过 8 条（`SUMMARIZE_THRESHOLD = 8`）时，把前 N-4 条历史（含旧摘要）调用摘要模型压缩成更新后的累积摘要，与最近 4 条原文（`KEEP_RECENT = 4`）一起直接以 `[对话摘要]` 形式保存在 Redis 消息列表中。
> *   **模型选择**：文本摘要压缩属于简单归纳，不需要复杂推理，我们配置使用快模型（`agent.model.summary`，如 Qwen2.5-7B），极大地降低了运行成本并减少了等待时间。

---

### 演示 4：长期记忆画像的抽取与合并更新（4 分钟）

**操作：**
1. 在输入框中输入包含显式记住偏好的对话：
   `请帮我记一下：我买笔记本的预算是 8000 元，平时写代码用，喜欢轻薄的款，不要游戏本。`
   *（注：后台 `ShoppingAgentController.containsRememberKeyword` 检测到“记住/记一下/记着/请记/帮我记”关键词，即自动触发画像抽取；亦可在前端点击“清空会话”时兜底触发抽取）*

**预期结果：**
*   Agent 正常流式回复，同时后台触发 `UserProfileExtractor.extract()`，读取旧画像与新对话，让模型提取并合并更新至 MySQL `user_profile` 表。

**日志观察（agent-runtime）：**
*   **控制台与画像日志（`logs/llm-profile.log`）**：
    ```text
    [UserProfile] LLM 原始输出: {"facts":"张三，程序员","preferences":"预算8000元，喜欢轻薄款","tasks":"购买写代码的笔记本","constraints":"不要游戏本"}
    [UserProfile] 用户 zhangsan 画像更新:
      事实: 张三，程序员
      偏好: 预算8000元，喜欢轻薄款
    ```
    打开 `logs/llm-profile.log` 可看到 `PROFILE EXTRACT REQUEST`（旧画像 JSON + 新对话）和 `PROFILE EXTRACT RESPONSE`。
*   **数据库查询（终端运行）**：
    ```bash
    mysql -u root agent_db -e "SELECT user_id, facts, preferences, tasks, constraints FROM user_profile WHERE user_id = 'zhangsan'"
    # 观察输出：MySQL user_profile 表已持久化合并后的结构化画像字段
    ```

**讲解要点：**
> *   **为什么在这里要用推理模型**：画像抽取（`UserProfileExtractor.java`）需要输出严格的 JSON。快模型在处理 Schema 约束时容易发生格式损毁（如多逗号、漏双引号），导致后端反序列化报错。因此我们将画像提取路由至推理模型（`agent.model.reasoning`，如 Qwen3-8B），保证数据安全入库。
> *   **画像的合并更新**：新偏好“预算8000、轻薄”并不会粗暴覆盖旧事实（“我是张三，程序员”），`UserProfileExtractor` 会读取 MySQL 中的旧画像与 Redis 中的最新对话，交由 LLM 智能合并更新。

---

### 演示 5：跨会话短期记忆清空与长期记忆生效（2 分钟）

**操作：**
1. 点击前端的**「清空会话」**按钮（此时 Redis 历史消息被清除）。
2. 在输入框中输入：“`根据我的偏好，帮我推荐一台笔记本电脑。`”

**预期结果：**
*   即使没有任何当前会话历史，Agent 依然能根据您的长期画像准确回答：“您是程序员，喜欢轻薄本，预算在 8000 元左右且不要游戏本，我为您推荐...”

**日志观察（agent-runtime）：**
*   查看 `logs/llm-interactions.log`（或 `logs/llm-chat.log`）的请求体：
    ```text
    [Memory Injection] sessionId=...
    ## 用户画像
    事实: 张三，程序员
    偏好: 预算8000元，喜欢轻薄款
    任务: 购买写代码的笔记本
    约束: 不要游戏本

    [User Prompt] 用户问题: 根据我的偏好，帮我推荐一台笔记本电脑。
    ```

**讲解要点：**
> *   **长期与短期记忆协作**：清空会话证明了 Redis 的短期缓存已被清除。但只要用户登录状态不变，后端 `MemoryManager.buildContext()` 就会从 MySQL `user_profile` 中读取长期画像并注入 Prompt。这就实现了“跨 Session 持久生效”的企业级记忆系统。

---

## 常见问题排查

### Q: 启动后端报错 `Connection refused` (Redis/MySQL)
*   **原因**：本地的 Redis（6379）或 MySQL（3306）服务未启动，或者密码配置错误。
*   **解决**：使用 Docker 启动 Redis 和 MySQL，或确认 `application-local.yml` 里的用户名密码是否匹配本地环境。

### Q: 对话无法保存，日志显示 `[UserProfile] 解析失败`
*   **原因**：画像提取模型配置名称在硅基流动上不存在，或误配为小模型导致 JSON 格式损坏。
*   **解决**：确认 `application.yml` 里的 `agent.model.reasoning` 使用的是在线状态的推理模型名（如 `Qwen/Qwen3-8B`），并查看 `logs/llm-profile.log` 中的原始输出。

---

## 演示时间分配

| 演示阶段 | 演示内容 | 耗时 |
|:---|:---|:---|
| 演示前准备 | 数据库和 Redis 连接说明（切换 tag `v1.2.1`） | 1 分钟 |
| 演示 1 | 用户登录及 sessionId 初始化 | 1 分钟 |
| 演示 2 | Session Memory 短期记忆与 `logs/llm-interactions.log` 验证 | 2 分钟 |
| 演示 3 | `SummaryPipeline` 满 8 条累积摘要压缩与 `logs/llm-summary.log` 查看 | 4 分钟 |
| 演示 4 | `@Async UserProfileExtractor` 长期画像抽取与 `logs/llm-profile.log` / MySQL 验证 | 4 分钟 |
| 演示 5 | 跨 Session 长期画像生效验证 | 1 分钟 |
| **合计** | | **13 分钟** |
