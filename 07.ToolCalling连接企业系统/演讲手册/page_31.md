## Slide Objective
把这门课的关键断言逐条变成在 `v1.4` 代码上"动手能验证"的八个实验：Tool 自动发现、实时数据来自 MySQL、数据分层、路由确定性、记忆自动沉淀、MCP Resource 读取验证（`user://{userId}`）、MCP 断线重连与冷却保护验证（`ensureConnected`）、以及 `v1.4` 治理缺口现场观察（直指第八课演进目标）。每一条都对应 `v1.4` 真实存在的接口与代码——`GET /api/agent/chat`、`POST /api/agent/recommend`、`logs/llm-route.log`、`logs/llm-extract.log`、`McpKnowledgeClient`、`McpProductClient`——这页是"听完"和"会做"的分界线。

## Suggested Presentation
按顺序做八个实验，每个先说验证什么、再说看哪里：

**① Tool 发现与握手**：按 `mcp-knowledge-server(:8082)` → `mcp-product-server(:8081)` → `agent-runtime(:8080)` 顺序启动三个服务，看 agent-runtime 控制台 `[MCP] 发现 3 个 Tool` / `[MCP] 已包装 3 个 FunctionCallback`（`McpProductClient.init()` 打的）；knowledge-server 侧注册的是 4 Tool + 1 Resource（`user://{userId}`，见 `mcp-knowledge-server/McpServerConfig`），但 `save_memory` 不包装给 LLM——**验证"Tool 是自动发现的，加 Tool 不用改 Client"**。

**② 实时数据来自 MySQL**：调 `GET /api/agent/chat` 问"拯救者 Y9000P 多少钱"（NB003，`docs/schema.sql` 初始数据 8999 元/库存 12 件），直接改 MySQL `product` 表的 `price` 再问一遍，答案跟着变——证明走的是 `ProductRepository.getProductInfo()` 的参数化查询，不是模型记忆。

**③ 数据分层**：问"推荐一台写代码的笔记本"（`LlmRouter` 判为 `KNOWLEDGE` → 走 `search_knowledge` → pgvector 描述语义检索）和"NB001 多少钱"（`LlmRouter` 判为 `TOOL` → 走 `queryProductInfo` → MySQL 实时数据），对照两段回答的来源差异——描述归 pgvector、价格库存归 MySQL，MCP 统一出口。

**④ 路由判定是确定性的**：调用 `POST /api/agent/recommend` 或 `GET /api/agent/chat`（`LlmRouter.route()` 的入口）发同样内容两次，查看 `logs/llm-route.log` 的 ROUTE REQUEST/RESPONSE 里 Route 与 Selected Model 完全一致——因为路由调用带 `withTemperature(0.0)`（`LlmRouter.route()`）。

**⑤ 记忆自动沉淀（系统行为）**：聊满 20 条消息看控制台 `[Memory] 消息数 20 达到阈值 20，触发自动沉淀`（`MemoryManager.PRECIPITATE_THRESHOLD = 20`）及 `logs/llm-extract.log`，用 `psql` 查 `user_memory` 表能看到摘要——验证 `save_memory` 不暴露给 LLM、由系统控制时机。

**⑥ MCP Resource 读取验证（`user://{userId}`）**：调用 `GET /api/agent/chat` 对话时，观察 `MemoryManager.buildContext()` 调用 `McpKnowledgeClient.readUserProfile(userId)`，通过 MCP Resource URI `user://U001` 从 `mcp-knowledge-server` 读取 MySQL 用户画像并注入 `context` 的「用户画像」段——验证 Resource 由 Client 代码主动按 URI 直读，无需耗费一次 LLM `tool_calls` 往返。

**⑦ MCP 断线重连与冷却保护验证（`ensureConnected`）**：手动停掉 `mcp-product-server(:8081)` 后提问商品价格，观察 `McpProductClient.ensureConnected()` 触发连接异常降级，且在 10 秒冷却期内（`RECONNECT_COOLDOWN_MS = 10_000`）再次提问会直接跳过重连、防止每次请求卡死阻塞；随后重启 `mcp-product-server`，等待 10 秒后再提问，观察控制台打印重连成功并重新发现 3 个 Tool，价格查询自动恢复！

**⑧ `v1.4` 治理缺口现场观察（引出第八课）**：在 `v1.4` 现场亲自观察四个真实局限：
1. 问一句跨路由的多步问题（如"帮我推荐两款笔记本并查实时库存对比哪款更值得买"），观察单次三选一路由 + 单轮 Function Calling 无法分步规划执行；
2. 停掉下游服务时，单次调用失败直接报错返回，没有指数退避重试与连续失败熔断器；
3. 查看 `MemoryService.save()` 代码，每次沉淀都是裸 `INSERT`，无幂等键去重；
4. 查看 `McpToolCallback` 日志，只有控制台文本输出，无高危操作人工确认拦截、也无步骤级执行追踪（Trace）。
——这四个现场观察到的缺口，直接指向我们**下一课（第八课 Workflow `v1.5`）**要手写的 `AgentRuntime`、`TaskPlanner`、`ErrorRecovery`、`ConfirmationService` 和 `TraceService`！

## Key Emphasis
必须讲清：前七个实验验证了 `v1.4` 的核心能力（MCP Tool 自动发现、实时查询、数据分层、确定性路由、记忆自动沉淀、Resource 读取、10 秒冷却断线重连），第 ⑧ 个实验则让学员亲眼看到 `v1.4` 的能力边界与治理缺口——缺口不是听来的，是自己跑出来的。

## Optional Extension
**学员问：这些验证点跑完都过了，是不是说明 `v1.4` Demo 可以接生产了？**
答：恰恰相反，第 ⑧ 个验证点跑完你就拿到了"不能直接接生产"的现场证据。
Demo 做法：⑧ 里四个现场（单步无规划、失败无退避重试与熔断、写入无幂等、无确认与结构化追踪）把 `v1.4` 的真实边界摊在桌面上——下一课第八课我们先补齐多步编排与执行层容错（重试/熔断/确认/Trace）。
生产做法：再按本课治理五件套（限流/幂等/超时/补偿/审计）逐格落地。Demo 的价值是把能力与缺口都变成可复现的实验，而不是把缺口留着上生产。

**学员问：实验②改数据库答案就变，怎么防止 LLM 幻觉出不存在的商品数据？**
答：这正是 Tool Calling 的意义——让事实来自系统不来自模型。
Demo 做法：问一个 `product` 表里不存在的商品 ID（如 XX999），看返回"未找到商品ID"（`McpServerConfig` 的 `isError(true)` 分支）——LLM 拿到明确错误就不会编。
生产做法：Tool 返回结构化错误码 + 可读信息，Prompt 层约定"工具报错就说查不到，不要编"（`PromptManager` 的知识使用规则已有雏形）；再加 Tool 参数白名单（第 10 项差距"安全"）。取舍：约束越死，长尾问题的灵活度越低——但对价格/库存这类事实，宁可呆板不可编造。

## Classroom Interaction
分组认领实验：每组挑一个（建议 ⑥⑦⑧ 分给基础好的组），先预测现象、再动手、最后对着 `v1.4` 代码指认"是哪几行让这个现象发生"（如 ⑥ 的 `McpKnowledgeClient.readUserProfile()`、⑦ 的 `McpProductClient.ensureConnected()`）。做完每组用一句话回答"这个实验验证的工程结论是什么"。

## Take Away
八个验证点跑完，`v1.4` Demo 的能力边界和治理缺口都是自己亲手确认的：能力来自 Tool 自动发现 + Resource 读取 + 实时数据 + 分层检索 + 确定性路由 + 冷却重连，缺口指向第八课的多步编排容错与五件套生产治理。

## Why This Slide Matters
跳过这页，学员对 Demo 的认知停在"课件说它能"，既不知道怎么验证一个 Agent 系统的真实行为（改数据看变化、停服务看重连冷却），也不知道怎么用可复现的实验自证治理缺口。

## Transition
验证点都跑通了，代码也按路线图读完了。拉远看全局（page_32）——第七课的能力地图和构建进度，确认自己走到哪、还差什么。
