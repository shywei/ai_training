# 第九课 Demo 演示步骤：从手搓到框架

> **讲师须知**：本手册为第九课《从手搓到框架：框架的边界》专用的现场教学操作手册。本课通过同一导购业务下的**三套对比演示工程（纯手搓原版、Spring AI 2.0 版、AgentScope 2.0 版）**，利用 IDE 分屏代码走读与终端 cURL / SSE 交互演练，帮助学员直观掌握：Spring AI 2.0 升级了什么、哪些编排逻辑仍需手写、AgentScope 2.0 解决了哪些复杂运行时痛点、以及不同业务需求该如何选型并做好六边形防套牢隔离。

---

## 演示全景与三套对比工程端口矩阵

三套对比工程端口互不冲突，并存运行于同一开发环境中，底层复用同一套 Docker 基础容器与企业 MCP 服务：

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       全课程三套对比工程同屏对照全景图                         │
└─────────────────────────────────────────────────────────────────────────────┘
  ① enterprise-ai-agent 【手搓原版对照】
     ├── 技术栈: Spring AI 1.0.0-M4 + Spring Boot 3.4.1 + Java 17
     ├── 端口: 8080
     └── 对照重点: 展示纯手写 1834 行状态机、正则解析与 while 循环的工程痛点

  ② enterprise-ai-agent-springai2 【Spring AI 2.0 基础设施版】
     ├── 技术栈: Spring AI 2.0.2 GA + Spring Boot 4.0.0 + Java 21
     ├── 端口: 8083 (独立运行)
     └── 演示重点: ChatClient.builder()、@Tool 声明式工具、CallAdvisor/StreamAdvisor
                  【看清边界：单次通信与工具调用极简，但多轮状态机与断点挂起仍需手写】

  ③ enterprise-ai-agent-agentscope 【AgentScope 2.0 智能体框架版】
     ├── 技术栈: AgentScope Java 2.0.0 GA + Spring Boot 3.4.1 + Java 17+
     ├── 端口: 8084 (独立运行)
     └── 演示重点: ReActAgent 自动循环、五阶段 Middleware 钩子、类型化多通道事件流、StateStore
                  【看清价值：复杂多步规划、高危审批挂起与多通道流式如何大幅降低开发难度】
```

### 端口矩阵与组件拓扑速查表

| 组件 / 服务名称 | 监听端口 | 归属与技术栈 | 职责与演示说明 |
|:---|:---:|:---|:---|
| **MySQL 8.0** | `3306` | Docker (`enterprise-ai-agent`) | 用户画像与订单数据存储（库名：`agent_db`） |
| **Redis 6+** | `6379` | Docker (`enterprise-ai-agent`) | 会话上下文与 Checkpointer 状态热缓存 |
| **PostgreSQL 14+ (pgvector)**| `5432` | Docker (`enterprise-ai-agent`) | 向量知识检索 + `agent_execution_checkpoint` 表 |
| **MCP Product Server** | `8081` | Spring Boot 3.4.1 (Java 17) | 企业商品信息与库存查询 MCP 服务 |
| **MCP Knowledge Server**| `8082` | Spring Boot 3.4.1 (Java 17) | 政策手册与 RAG 向量检索 MCP 服务 |
| **基准工程（手搓原版）** | `8080` | `enterprise-ai-agent` (Java 17) | v5.0 基准线（可选启动对照） |
| **第一级工程（现代基础设施）** | `8083` | `springai2` (Java 21) | v6.0 对照线（提供 `/api/v6/workflow/*`） |
| **第二级工程（高阶应用框架）** | `8084` | `agentscope` (Java 17+) | v7.0 重构线（提供 `/api/v7/workflow/*`） |

---

## 演示前环境准备（保姆级前置步骤）

### 步骤 0：启动公共 Docker 中间件并初始化 Checkpoint 快照表
在终端中进入原工程目录启动 Docker：
```bash
cd ai_training/enterprise-ai-agent
docker compose up -d

# 验证基础服务存活
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

在 PostgreSQL 中执行 `agent_execution_checkpoint` 表结构创建脚本：
```bash
# 登录 PostgreSQL 容器并执行建表脚本
docker exec -i enterprise-ai-agent-postgres psql -U postgres -d knowledge_db < "../enterprise-ai-agent-springai2/docs/schema-checkpoint.sql"
```

### 步骤 1：启动双 MCP Server（商品与知识服务）
```bash
# 终端 1：启动商品 MCP Server (端口 8081)
cd ai_training/enterprise-ai-agent/mcp-product-server
mvn spring-boot:run

# 终端 2：启动知识 MCP Server (端口 8082)
cd ai_training/enterprise-ai-agent/mcp-knowledge-server
mvn spring-boot:run
```

### 步骤 2：配置大模型密钥环境变量
为两个新工程的终端配置 SiliconFlow（或兼容 OpenAI 协议）API 密钥：
```bash
# Windows PowerShell
$env:SILICONFLOW_API_KEY="sk-your-siliconflow-api-key"
$env:LLM_BASE_URL="https://api.siliconflow.cn"
$env:LLM_MODEL_NAME="Qwen/Qwen2.5-7B-Instruct"

# Linux / macOS Bash
export SILICONFLOW_API_KEY="sk-your-siliconflow-api-key"
export LLM_BASE_URL="https://api.siliconflow.cn"
export LLM_MODEL_NAME="Qwen/Qwen2.5-7B-Instruct"
```

### 步骤 3：启动 Spring AI 2.0 对照工程 (端口 8083)
```bash
# 终端 3：进入 springai2 工程并启动 (需要 JDK 21 环境)
cd ai_training/enterprise-ai-agent-springai2
mvn spring-boot:run
```
*日志出现 `Tomcat started on port 8083 (http) with context path '/'` 即就绪。*

### 步骤 4：启动 AgentScope 2.0 重构工程 (端口 8084)
```bash
# 终端 4：进入 agentscope 工程并启动 (JDK 17 或 21 均可)
cd ai_training/enterprise-ai-agent-agentscope
mvn spring-boot:run
```
*日志出现 `Tomcat started on port 8084 (http) with context path '/'` 即就绪。*

---

## 第一部分：第一级阶梯同屏代码对比（`v5.0 手搓` $\to$ `v6.0 Spring AI 2.0`）

> **授课心智**：在 IDE 中左右分屏打开代码，引导学员观察从手搓底层胶水到官方基础设施的演变。

### IDE 分屏对比点 1：Prompt 构建与管理
- **左屏（手搓原版）**：打开 `enterprise-ai-agent/src/main/java/.../PromptManager.java` (65行)
  - 讲解点：“看，过去我们需要手动维护一个模板字符串，通过 `String.format()` 挨个替换系统人设、工具元数据与商品上下文，手动创建 `SystemMessage` 和 `UserMessage` 列表，繁琐且容易遗漏。”
- **右屏（Spring AI 2.0）**：打开 `enterprise-ai-agent-springai2/src/main/java/.../SpringAi2ChatConfig.java` (25行核心)
  - 讲解点：“现在 Spring AI 2.0 提供了 Fluent 链式构建器 `ChatClient.builder(chatModel)`，通过 `.defaultSystem(...)` 声明式注入，参数由 Starter 自动装配，样板代码锐减 61%！”

### IDE 分屏对比点 2：工具声明与 Function Calling 调度
- **左屏（手搓原版）**：打开 `AgentRuntime.java` 第 380~420 行
  - 讲解点：“过去模型调用工具，全靠 Prompt 约定输出 `TOOL_CALL:queryProductInfo(productId=NB001)`，后端写极其脆弱的正则表达式提取。一旦模型多打了一个换行或中文括号，正则当场报废！并且还要手写 `McpToolCallback` 做复杂的 JSON Schema 转化。”
- **右屏（Spring AI 2.0）**：打开 `SpringAi2ToolAdapter.java`
  - 讲解点：“看现在的 `@Tool` 注解。只需在 Java 方法上标上 `@Tool(name = "queryProductInfo", description = "...")`，框架自动通过反射推导 OpenAPI/JSON Schema。更重要的是，Spring AI 2.0 内置了 `ToolCallingAdvisor`，模型返回 `finish_reason=tool_calls` 后，框架底层自动反序列化参数并反射执行，正则胶水代码全部归零（-100%）！”

### IDE 分屏对比点 3：安全切面拦截机制
- **左屏（手搓原版）**：打开 `GuardrailsService.java`
  - 讲解点：“以前安全过滤是散落在业务代码里的，在 Controller 调一次，在 Runtime 里调一次，开发极易漏调。”
- **右屏（Spring AI 2.0）**：打开 `GuardrailsCallAdvisor.java`
  - 讲解点：“现在严格实现了 Spring AI 2.0.2 GA 规范的 `CallAdvisor`。通过 `AdvisorChain`，在模型调用入站前统一拦截 Prompt 注入并执行手机号脱敏（`138****5678`），在模型出站后统一对 `password` 等敏感词打码，实现了真正的切面生命周期治理。”

### ⭐ 关键痛点揭示（第一级阶梯的极限与反思）
打开 `SpringAi2WorkflowAdapter.java`，让学员看第 80~150 行：
- **讲师话术**：“大家看，虽然 `SpringAi2WorkflowAdapter` 比手搓的 651 行短了不少（降至 240~300 行），**但是**：
  1. 状态机推进逻辑（`PLANNING -> EXECUTING -> CONFIRMING`）依然要我们手动写代码跳转！
  2. 遇到人在回路（HITL），依然要手动将快照存到 Checkpoint，手动返回 `confirming` 状态！
  3. 收到用户的审批确认后，依然要开发者手动写方法去复水状态并唤醒执行！
  4. **结论**：Spring AI 的定位是**基础设施（如同 Java EE 的 Servlet API）**，它解决的是单次调用与工具通信的标准化；但企业级 Agent 需要多轮认知编排、生命周期钩子与多通道事件，我们需要更高层级的**应用框架（如同 Spring MVC）**！”

---

## 第二部分：第二级阶梯同屏代码对比（`v6.0 Spring AI 2.0` $\to$ `v7.0 AgentScope 2.0`）

> **授课心智**：展示从“手写多步状态推进”到“应用框架全面接管”的第二次质变飞跃。

### IDE 分屏对比点 4：核心工作流编排适配器
- **左屏（Spring AI 2.0）**：`SpringAi2WorkflowAdapter.java`（约 300 行，手动编排 while 循环与状态快照）
- **右屏（AgentScope 2.0）**：`AgentScopeWorkflowAdapter.java`（核心编排仅约 110 行！）
  - 讲解点：“看右侧，我们直接使用 AgentScope 官方的 `ReActAgent`。只需一行代码 `shoppingAgent.call(message, context).block()`，框架内部自动闭环多轮思考推演、工具选择与循环终止收敛。消灭了 83% 的状态机胶水代码！”

### IDE 分屏对比点 5：拦截扩展哲学差异（Advisors vs Middleware）
- **左屏（Spring AI 2.0）**：`GuardrailsCallAdvisor.java`
  - 讲解点：“Spring AI 的 Advisor 是面向**单次 HTTP 请求/响应**的责任链，它根本无法洞察智能体当前进行到了思考推演的哪一步。”
- **右屏（AgentScope 2.0）**：`LifecycleMiddleware.java`
  - 讲解点：“AgentScope 的 Middleware 实现了 `onAgent`、`onSystemPrompt`、`onModelCall`、`onReasoning`、`onActing` 五阶段生命周期钩子（异常通过 Reactor `.onErrorResume()` 捕获）。在 `onActing`（工具执行前），中间件直接拦截到了大额订单，通过发射 `RequireUserConfirmEvent` 优雅挂起任务，并在 Checkpoint 保存快照。这是请求级拦截器根本做不到的！”

---

## 第三部分：现场 cURL / 终端实操演练（四大硬核机制验证）

### 演练 1：类型化 `AgentEvent` 多通道流式输出 (SSE)
> **演示目标**：展示 Thought（思考流）、Tool（工具流）、Answer（回答流）与 Control（控制流）的彻底解耦。

在终端中执行命令请求 `8084` 端口：
```bash
curl -N -X GET "http://localhost:8084/api/v7/workflow/stream-events?message=帮我查一下拯救者Y9000P的库存和配置&sessionId=sess-demo-sse"
```

**控制台实时流式输出效果**：
```
data:{"channel":"control","type":"step_start","step":1,"action":"AgentScope ReAct Reasoning & Execution"}
data:{"channel":"thought","type":"thought","payload":"用户询问联想拯救者Y9000P的库存和配置，我需要调用 query_product 工具进行查询..."}
data:{"channel":"tool","type":"tool_start","payload":"Calling tool: queryProductInfo with args: {productId=NB001}"}
data:{"channel":"tool","type":"tool_result","payload":"{\"id\":\"NB001\",\"name\":\"联想拯救者 Y9000P\",\"price\":8999.00,\"stock\":18,\"specs\":\"i9-14900HX/16G/1T/RTX4060\"}"}
data:{"channel":"answer","type":"token","payload":"联想拯救者 Y9000P 目前在售，配备 i9-14900HX 处理器与 RTX4060 显卡，当前商城售价为 ¥8999，库存充足（18件）。"}
data:{"channel":"control","type":"step_complete","step":1,"status":"COMPLETED"}
```

**讲师讲解话术**：
- “看终端返回的每一行 SSE 数据，它不再是混在一起的文本字符，而是带有明确 `channel` 属性的类型化事件！”
- “`channel: thought` 是模型的思维链，前端可以直接将其塞进折叠展开卡片；”
- “`channel: tool` 让前端在 0 毫秒内点亮‘正在查库存’步骤指示器；”
- “`channel: answer` 才是打字机最终回复，完全消灭手写正则拆分字符串的伪流式！”

---

### 演练 2：Schema Repair Loop 自愈纠错循环演示
> **演示目标**：展示当模型生成的工具参数发生格式错误或校验失败时，系统不再粗暴降级或报错崩塌，而是自动触发自愈修复提示词驱动大模型自我修正。

在终端中执行单测验证自愈拦截器：
```bash
cd ai_training/enterprise-ai-agent-agentscope
mvn test -Dtest=AgentScopeArchitectureTest#testToolOrderRequestValidation
```

**后台日志呈现机制走读**：
打开 `SchemaRepairMiddleware.java`，指给学员看以下关键逻辑：
```
[SchemaRepairMiddleware] 捕获到工具入参校验异常: 手机号格式不符合标准11位手机号规则
[SchemaRepairMiddleware] 触发第 1/3 次模式自愈重试循环, RepairPrompt: 
【系统纠错通知】你上一轮尝试调用的工具入参校验失败，请依据以下错误修复后重新调用：
- contactPhone: 必须符合有效的 11 位中国大陆手机号规范 (当前值: 12345)
- productId: 格式必须为 ^(NB|PH|ACC)[0-9]{3}$ (例如 NB001，当前值: INVALID_ID)
```
**讲师讲解话术**：
- “在手搓 v5.0 中，只要参数校验失败，工作流立即抛出异常并粗暴降级；”
- “而在 AgentScope 中，`SchemaRepairMiddleware` 拦截到 `ValidationError`，自动将错误违规细节拼装成 Repair Prompt 塞回模型上下文，模型立刻自主纠正并重新下发合法参数，这就是从‘粗暴熔断’到‘自主认知自愈’的技术跃迁！”

---

### 演练 3：高风险大额订单 HITL 挂起 + 断点复水 + 时光倒流分支
> **演示目标**：验证金额 > ¥5000 触发业务硬风控挂起，并在 PostgreSQL 中持久化快照，随后进行复水与分支衍生。

#### ① 发起大额下单请求（触发挂起）
```bash
curl -X POST "http://localhost:8084/api/v7/workflow/execute" \
  -d "message=帮我下单购买一台联想拯救者Y9000P，金额8999元，地址为北京市海淀区中关村南大街1号&sessionId=sess-hitl-01"
```

**预期返回 JSON（状态为挂起 `AWAITING_APPROVAL` / `confirming`）**：
```json
{
  "executionId": "v7-exec-a81d9f02",
  "checkpointId": "cp-hitl-4f81c9a0",
  "status": "confirming",
  "state": "CONFIRMING",
  "message": "大额高风险动作已拦截，等待用户确认",
  "pendingConfirmation": {
    "toolName": "createOrder",
    "amount": 8999.00,
    "riskReason": "订单金额 ¥8999.00 超过高风险免密阈值 ¥5000.00",
    "arguments": {
      "productId": "NB001",
      "quantity": 1,
      "totalAmount": 8999.00
    }
  }
}
```

#### ② 模拟断点复水（Rehydration：进程无缝恢复状态）
```bash
curl -X POST "http://localhost:8084/api/v7/workflow/rehydrate" \
  -d "checkpointId=cp-hitl-4f81c9a0"
```
**预期返回**：成功从 PostgreSQL 反序列化出 `executionId`, `stepCursor=1`, `callStack` 和 `scratchpad`，执行栈完美苏醒，不丢失任何中间状态！

#### ③ 时光倒流与分支衍生（Time-Travel Forking）
假设用户在审批界面改变了主意，不想要拯救者了，要求分叉回退重试：
```bash
curl -X POST "http://localhost:8084/api/v7/workflow/fork" \
  -d "baseCheckpointId=cp-hitl-4f81c9a0&note=用户临时改买轻薄本MacBook"
```
**预期返回**：生成全新的 `forkedExecutionId`，其 `parentCheckpointId` 稳稳指向挂起前的检查点，原始快照历史完全不受破坏！

#### ④ 人在回路用户最终审批确认（恢复执行并完成下单）
```bash
curl -X POST "http://localhost:8084/api/v7/workflow/confirm" \
  -d "checkpointId=cp-hitl-4f81c9a0&approved=true&reason=预算已获批"
```
**预期返回**：状态解除挂起跃迁至 `COMPLETED`，订单正式创建并生成 `ORD-xxxx` 单号！

---

### 演练 4：六边形架构防套牢与零侵入平替验证
> **演示目标**：证明底层框架从 Spring AI 2.0 切换为 AgentScope 2.0 时，核心业务领域与端口代码 0 修改，且单测全部通过。

在 IDE 或终端中对比两套工程的纯领域包：
```bash
# 查看 springai2 的 domain 目录
ls ai_training/enterprise-ai-agent-springai2/src/main/java/com/example/agent/domain

# 查看 agentscope 的 domain 目录
ls ai_training/enterprise-ai-agent-agentscope/src/main/java/com/example/agent/domain
```
*两者包含完全一致的 `ShoppingDomainService.java`、`AgentCheckpoint.java`、`ShoppingIntent.java`、`ToolOrderRequest.java`，没有包含任何具体的 AI 框架依赖注解！*

在 `enterprise-ai-agent-agentscope` 下执行全量单元测试：
```bash
cd ai_training/enterprise-ai-agent-agentscope
mvn test
```

**预期输出（4 项核心架构测试全绿通过）**：
```
[INFO] Running com.example.agent.AgentScopeArchitectureTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.258 s -- in com.example.agent.AgentScopeArchitectureTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] BUILD SUCCESS
```

---

## 演示常见排障手册（Troubleshooting）

### Q1: 提示 PostgreSQL 找不到 `agent_execution_checkpoint` 表？
- **原因**：尚未向 PostgreSQL 容器中注入本课专属的 Checkpoint 表结构。
- **解决**：在终端执行：
  ```bash
  docker exec -i enterprise-ai-agent-postgres psql -U postgres -d knowledge_db < "../enterprise-ai-agent-springai2/docs/schema-checkpoint.sql"
  ```
  执行后可进入容器执行 `\dt` 查看表是否创建成功。

### Q2: 启动 `enterprise-ai-agent-springai2` (8083) 报 JDK 版本不匹配？
- **原因**：Spring AI 2.0.2 GA 与 Spring Boot 4.0 强制要求 **JDK 21+**（深度绑定虚拟线程与模式匹配）。
- **解决**：在终端执行 `java -version` 确认当前 JDK 版本。若为 JDK 17，请设置 `JAVA_HOME` 指向本地的 JDK 21 路径后再执行 `mvn spring-boot:run`。

### Q3: 端口 `8083` 或 `8084` 报端口已被占用（Port already in use）？
- **原因**：先前运行的后台进程尚未完全杀死。
- **解决**：
  ```powershell
  # Windows PowerShell 查询占用端口的 PID
  netstat -ano | findstr :8083
  netstat -ano | findstr :8084
  # 根据最后显示的 PID 强行终止进程
  taskkill /PID <PID> /F
  ```

### Q4: 调用大模型提示 `401 Unauthorized` 或调用超时？
- **原因**：环境变量 `SILICONFLOW_API_KEY` 未配置或未在启动应用所在的终端生效。
- **解决**：确认终端执行了 `$env:SILICONFLOW_API_KEY="sk-xxxx"`。
- **离线演示兜底保障**：若网络彻底断开，`McpEnterpriseToolAdapter` 与 `SpringAi2ToolAdapter` 均内置了 Mock 数据回退机制，单元测试（`mvn test`）仍可 100% 独立离线运行通过！

---

## 演示时间分配建议表

| 演示环节 | 推荐耗时 | 重点讲解与聚焦核心 |
|:---|:---:|:---|
| **0. 全景拓扑与三工程矩阵介绍** | 2 分钟 | 讲透 8080 (手搓原版) $\to$ 8083 (现代基建) $\to$ 8084 (应用框架) 三层定位 |
| **1. 第一级代码对比 (v5.0 $\to$ v6.0)** | 4 分钟 | PromptManager vs ChatClient、正则提取 vs @Tool、揭示手搓工作流依然存在的痛点 |
| **2. 第二级代码对比 (v6.0 $\to$ v7.0)** | 3 分钟 | 300行工作流 vs 110行 ReActAgent、Advisor 单次请求切面 vs Middleware 五阶段认知钩子 |
| **3. 终端实操：28类事件流与模式自愈** | 3 分钟 | 演示 8084 stream-events 多通道解耦与 Schema Repair Loop 自愈纠错 |
| **4. 终端实操：HITL挂起与时光倒流** | 3 分钟 | 演示大额订单 (>¥5000) 拦截、PostgreSQL Checkpoint 复水与分支衍生 |
| **5. 六边形平替单测验证** | 2 分钟 | 运行 `mvn test` 验证 4 项测试全绿，强调防套牢工程心智 |
| **合计** | **17 分钟** | 节奏紧凑、层次分明、理论与工程双重震撼 |
