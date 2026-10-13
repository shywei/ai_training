# 第八课 Demo 演示步骤

> 讲师上课时的操作手册，照着做就行。

---

## 演示前准备

### 1. 基础环境确认

```bash
# 确认 MySQL (3306), Redis (6379), PostgreSQL (5432) 均已启动
redis-cli ping
mysql -u root -e "SHOW DATABASES LIKE 'agent_db'"
```

### 2. 数据库和 MCP Server

同第七课，无新增表、无新增 MCP Server。确认以下服务可正常启动：
- mcp-knowledge-server (:8082)
- mcp-product-server (:8081)

### 3. 启动顺序

```bash
# 终端 1：MCP Knowledge Server
cd mcp-knowledge-server && mvn spring-boot:run

# 终端 2：MCP Product Server
cd mcp-product-server && mvn spring-boot:run

# 终端 3：Agent Runtime
cd agent-runtime && ./mvnw spring-boot:run

# 终端 4：前端（可选，也可直接用 curl）
cd web && npm install && npm run dev
```

### 4. 验证服务就绪

```bash
# 确认 agent-runtime 启动
curl -s http://localhost:8080/api/agent/workflow -X POST -d "message=hello&sessionId=test" -d "userId=" 
# 应返回正常回复

# 确认 Workflow 端点可用
curl -s http://localhost:8080/api/agent/workflow/traces
# 应返回 {"traces":[]}
```

---

## 演示 1：多步对比查询（核心场景 🌟）

> **讲解要点**：第七课只能一问一答，第八课 Agent 能分步查询、综合分析。

### 操作

```bash
curl -s "http://localhost:8080/api/agent/workflow" \
  -X POST \
  -d "message=帮我对比拯救者和MacBook的价格，预算8000" \
  -d "sessionId=demo1" \
  -d "userId=U001" | python3 -m json.tool
```

### 预期结果

```json
{
  "traceId": "trace-xxxxxxxx",
  "status": "completed",
  "message": "根据对比，拯救者 Y9000P 售价 ¥8999，MacBook Pro 14 售价 ¥14999。您的预算 8000 元，拯救者更接近预算...",
  "state": "COMPLETED"
}
```

### 查看 Trace

```bash
# 用返回的 traceId 查看执行链路
curl -s "http://localhost:8080/api/agent/workflow/trace?traceId=trace-xxxxxxxx" | python3 -m json.tool
```

预期看到：
- TaskPlanner 规划了 3 步
- Step 1: search_knowledge("拯救者") → 成功
- Step 2: search_knowledge("MacBook") → 成功
- Step 3: LLM 综合分析 → 完成
- 每步有 llmThought（LLM 思考过程）和 durationMs（耗时）

### 讲解要点

- "看 Trace，Agent 不是一步完成的，而是分了 3 步"
- "TaskPlanner 先规划，然后 Runtime 逐步执行"
- "每步都有思考过程，出问题可以回溯"

---

## 演示 2：容错降级

> **讲解要点**：生产环境工具会挂，Agent 必须有容错能力。

### 操作

```bash
# 先停止 mcp-knowledge-server（终端 2 按 Ctrl+C）

# 然后发起请求
curl -s "http://localhost:8080/api/agent/workflow" \
  -X POST \
  -d "message=查询华为MateBook价格" \
  -d "sessionId=demo2" \
  -d "userId=U001" | python3 -m json.tool
```

### 预期结果

在 agent-runtime 终端看到日志：
```
[ErrorRecovery] 工具 search_knowledge 失败 (1/3): timeout
[ErrorRecovery] 重试等待 1000ms (第 1 次重试)
[ErrorRecovery] 工具 search_knowledge 失败 (2/3): timeout
[ErrorRecovery] 重试等待 2000ms (第 2 次重试)
[ErrorRecovery] 工具 search_knowledge 失败 (3/3): timeout
[AgentRuntime] 工具调用失败，降级为直接回答
```

最终返回一个基于已有信息的回答（不是报错）。

### 讲解要点

- "重试 3 次，指数退避 1s→2s→4s"
- "重试耗尽后不是报错，而是降级——让 LLM 基于已有信息回答"
- "有回答总比白屏好"

### 恢复

```bash
# 重启 mcp-knowledge-server
# 然后查看熔断状态
curl -s "http://localhost:8080/api/agent/workflow/circuit-breaker" | python3 -m json.tool

# 重置熔断器
curl -s -X POST "http://localhost:8080/api/agent/workflow/circuit-breaker/reset"
```

---

## 演示 3：人在回路

> **讲解要点**：高风险操作不能让 LLM 自己决定，必须有人类兜底。

### 操作

```bash
curl -s "http://localhost:8080/api/agent/workflow" \
  -X POST \
  -d "message=我要买拯救者" \
  -d "sessionId=demo3" \
  -d "userId=U001" | python3 -m json.tool
```

### 预期结果

```json
{
  "traceId": "trace-xxxxxxxx",
  "status": "confirming",
  "message": "⚠️ 操作确认\n\n即将执行高风险操作：执行 place_order\n工具：place_order\n\n请确认是否继续？",
  "state": "CONFIRMING",
  "confirmRequired": true,
  "pendingTool": "place_order"
}
```

### 用户确认

```bash
curl -s "http://localhost:8080/api/agent/workflow/confirm" \
  -X POST \
  -d "traceId=trace-xxxxxxxx" \
  -d "confirmed=true" \
  -d "sessionId=demo3" \
  -d "userId=U001" \
  -d "message=我要买拯救者" | python3 -m json.tool
```

预期：status: "completed"，返回下单结果。

### 用户取消（可选演示）

```bash
# 重新发起请求，然后取消
curl -s "http://localhost:8080/api/agent/workflow/confirm" \
  -X POST \
  -d "traceId=trace-xxxxxxxx" \
  -d "confirmed=false" \
  -d "sessionId=demo3" | python3 -m json.tool
```

预期：status: "completed"，message: "已取消操作。"

### 讲解要点

- "place_order 被 ConfirmationService 拦截了"
- "工作流暂停，等用户确认才继续"
- "这是企业安全的底线——AI 不应该对所有操作都有执行权"

---

## 演示 4：查看 Trace 列表

> **讲解要点**：Tracing 是调试、审计、优化的基础。

### 操作

```bash
curl -s "http://localhost:8080/api/agent/workflow/traces" | python3 -m json.tool
```

### 预期结果

返回最近的 Trace 列表，每个包含 traceId、userMessage、state、totalDurationMs、stepCount。

### 讲解要点

- "每次执行都有完整记录"
- "可以查看每步的思考过程、工具调用、耗时"
- "出问题时，不用让用户再操作一次，直接看 Trace 就能定位"

---

## 演示 5：统一入口演示（可选）

> **讲解要点**：前端统一调 /workflow，AgentRuntime 内部判断简单/复杂。

### 操作

```bash
# 简单问题
curl -s "http://localhost:8080/api/agent/workflow" \
  -X POST \
  -d "message=你好" \
  -d "sessionId=demo5" | python3 -m json.tool
# 返回：mode: "simple", stepCount: 0

# 复杂任务
curl -s "http://localhost:8080/api/agent/workflow" \
  -X POST \
  -d "message=帮我对比拯救者和MacBook" \
  -d "sessionId=demo5" | python3 -m json.tool
# 返回：mode: "workflow", stepCount: 3
```

### 讲解要点

- "前端不用关心复杂度，统一调 /workflow"
- "简单任务 mode: simple，一次 THINK 就回答"
- "复杂任务 mode: workflow，TaskPlanner 拆步骤，多轮执行"
- "前端统一调 /workflow，简单复杂都走这里"

---

## 常见问题排查

### Q: TaskPlanner 没有规划，直接回答了？
A: 检查日志中 `[TaskPlanner]` 输出。如果消息不含"对比""比较"等关键词，且只包含一个商品名，TaskPlanner 会跳过规划。尝试用"帮我对比 A 和 B"格式。

### Q: 确认请求没有触发？
A: LLM 需要判断"要下单"才会调用 place_order Tool。如果 LLM 只是推荐商品而没有调用 Tool，不会触发确认。尝试更明确地说"我要买""帮我下单"。

### Q: 重试日志没看到？
A: MCP Server 必须真的超时或报错才会触发重试。正常情况下工具调用一次就成功。要演示重试，需要模拟故障（停止 MCP Server 或注入延迟）。

### Q: Trace 中没有 llmThought？
A: llmThought 是 LLM 在 THINK 阶段的输出。如果 LLM 直接回答而没有调用 Tool，不会有 THINK 阶段的记录。

---

## 演示时间建议

| 演示 | 时间 | 说明 |
|------|------|------|
| 演示 1：多步查询 | 5 分钟 | 核心场景，必须做 |
| 演示 2：容错降级 | 3 分钟 | 需要停止 MCP Server，提前准备 |
| 演示 3：人在回路 | 3 分钟 | 核心场景，必须做 |
| 演示 4：Trace 列表 | 2 分钟 | 看一眼即可 |
| 演示 5：新旧对比 | 2 分钟 | 可选 |
| **合计** | **13-15 分钟** | |