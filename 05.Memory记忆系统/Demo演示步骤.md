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
# 执行建表 SQL（包含初始用户：zhangsan/123456）
mysql -u root agent_db < docs/schema.sql
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

### 4. 启动服务（按顺序）

打开终端并执行以下命令：

```bash
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
*   打开 `logs/llm-chat.log` 观察最新的一次调用日志：
    ```text
    [Memory] 读取 Session 历史消息 (条数: 2)
    [System Prompt] (只含有基本角色设定，无画像注入)
    [User Prompt] 包含了注入的历史对话：
    用户: 你好，我是张三，我的职业是程序员。
    助手: 你好！张三。已记下您的职业。
    用户当前问题: 请问我刚才说我的职业是什么？
    ```

**讲解要点：**
> *   **短期记忆的底层原理**：大模型接口本身是无状态的（Stateless）。我们是在 Java 后端（`SessionMemory.java`）中将每轮对话追加到 Redis 的 List 结构中。每次调用大模型前，从 Redis 取出历史消息，拼入 messages 数组或作为 context 注入到 User Prompt 中，以此建立“短期记忆”的假象。

---

### 演示 3：对话累积与摘要压缩（4 分钟）

**操作：**
1. 连续进行多轮简短对话，直到会话消息条数超过 **8 条**（即往返对话 4 次以上）。

**预期结果：**
*   系统自动完成压缩，回答流畅，且不会丢失早期对话的核心背景。

**日志观察（agent-runtime）：**
*   **控制台日志**：
    ```text
    [Summary] 当前会话条数: 8, 超过阈值，开始生成累积摘要...
    [Summary] 调用快模型进行文本摘要...
    [Summary] 摘要生成成功，写回 Redis List。
    ```
*   **Redis 查看（终端运行）**：
    ```bash
    redis-cli LRANGE session:default:messages 0 -1
    # 观察输出：
    # 1) "assistant|[对话摘要]：用户名为张三，职业为程序员..."
    # 2) "user|最近一条消息..."
    ```

**讲解要点：**
> *   **累积摘要的精髓**：随着对话增长，Token 成本成指数上升，且会撑爆窗口限制。我们通过 `SummaryPipeline.java` 判断当消息达到 8 条时，把前 4 条历史调用快模型压缩成一段 `[对话摘要]`，塞回 Redis 头部作为后续记忆基底。
> *   **模型选择**：文本摘要压缩属于简单归纳，不需要推理，我们显式配置使用快模型（`agent.model.summary`，如 Qwen2.5-7B），极大地降低了运行成本并减少了讲师演示时的等待时间。

---

### 演示 4：长期记忆画像的抽取与更新（4 分钟）

**操作：**
1. 在输入框中输入：“`记住我买笔记本的预算是 8000 元，喜欢轻薄的款。`”

**预期结果：**
*   Agent 回复确认已记录，并在控制台触发画像提取过程。

**日志观察（agent-runtime）：**
*   **控制台日志**：
    ```text
    [Memory] 检测到 记住 指令，触发画像抽取
    [UserProfile] 开始抽取长期记忆... 旧画像 facts=张三, preferences=...
    [UserProfile] 调用推理模型生成合并后的画像 JSON...
    [UserProfile] 成功写回 MySQL，user_id = zhangsan
    ```
*   **数据库查询（终端运行）**：
    ```bash
    mysql -u root agent_db -e "SELECT preferences FROM user_profile WHERE user_id = 'zhangsan'"
    # 观察输出：
    # 包含结构化的 JSON 画像数据，如 {"budget":"8000元","brand":"...","preference":"轻薄"}
    ```

**讲解要点：**
> *   **为什么在这里要用推理模型**：画像抽取（`UserProfileExtractor.java`）需要输出严格的 JSON。快模型在处理复杂的 Schema 约束时容易发生格式损毁（如多逗号、漏双引号），导致后端反序列化崩溃。因此我们通过配置独立指定画像提取使用推理模型（`agent.model.extract`，如 Qwen3-8B），保证数据安全入库。
> *   **画像的合并更新**：新偏好“预算8000、轻薄”并不会粗暴覆盖旧事实（“我叫张三”），LLM 会读取 MySQL 中的旧画像与最新对话，进行智能合并更新。

---

### 演示 5：跨会话短期记忆清空与长期记忆生效（2 分钟）

**操作：**
1. 点击前端的**「清空会话」**按钮（此时 Redis 历史消息会被清除）。
2. 在输入框中输入：“`根据我的偏好，帮我推荐一台笔记本电脑。`”

**预期结果：**
*   即使没有任何上下文历史，Agent 依然能根据您的偏好准确回答：“您喜欢轻薄本，且预算在 8000 元左右，我为您推荐...”

**日志观察（agent-runtime）：**
*   查看 `logs/llm-chat.log` 的请求体：
    ```text
    [Memory] 从 MySQL 中获取用户 zhangsan 的长期画像...
    [System Prompt] 注入 context: 用户事实：张三；用户偏好：预算8000元、轻薄。
    [User Prompt] 用户问题: 根据我的偏好，帮我推荐一台笔记本电脑。
    ```

**讲解要点：**
> *   **长期与短期记忆协作**：清空会话证明了 Redis 的短期缓存已被清除。但只要用户登录状态不变，后端 `MemoryManager.buildContext` 就会持久在 MySQL 中读取长期画像，并将其随 System Prompt 一并发送给 LLM。这就实现了“跨 Session 生效”的企业级记忆系统。

---

## 常见问题排查

### Q: 启动后端报错 `Connection refused` (Redis/MySQL)
*   **原因**：本地的 Redis（6379）或 MySQL（3306）服务未启动，或者密码配置错误。
*   **解决**：使用 Docker 启动 Redis 和 MySQL，或确认 `application-local.yml` 里的用户名密码是否匹配本地环境。

### Q: 对话无法保存，日志显示 `[Memory] 画像提取异常`
*   **原因**：画像提取模型配置名称在硅基流动上不存在，或网络波动导致调用超时。
*   **解决**：确认 `application.yml` 里的 `agent.model.extract` 使用的是在线状态的模型名，并检查网络代理配置。

---

## 演示时间分配

| 演示阶段 | 演示内容 | 耗时 |
|:---|:---|:---|
| 演示前准备 | 数据库和 Redis 连接说明 | 1 分钟 |
| 演示 1 | 用户登录及 sessionId 初始化 | 1 分钟 |
| 演示 2 | Session Memory 短期记忆与上下文注入验证 | 2 分钟 |
| 演示 3 | 累积摘要压缩触发与 Redis List 查看 | 4 分钟 |
| 演示 4 | 长期记忆画像抽取与 MySQL 数据落库验证 | 4 分钟 |
| 演示 5 | 跨 Session 记忆生效验证 | 1 分钟 |
| **合计** | | **13 分钟** |
