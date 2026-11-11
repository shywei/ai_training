## Slide Objective
展示 Function Calling 的完整 6 步流程和实际例子。

## Suggested Presentation
完整流程 6 步：

**① 定义函数**：每个函数需要三个要素——名称（如 `queryProductInfo`）、描述（"查询商品价格和库存"）、参数 Schema（JSON Schema 格式，定义参数类型和必填项）。Schema 是关键，它告诉 LLM 这个函数需要什么参数。

**② 注册到 LLM 请求**：在当前标准 OpenAI API 请求中，使用 `tools` 参数（2023 年早期的顶层 `functions` / `function_call` 字段已废弃），把所有可用函数的定义封装为 `{"type": "function", "function": {...}}` 传进去，并可通过 `strict: true` 开启 Structured Outputs 约束解码、通过 `parallel_tool_calls` 控制是否允许单轮并发调用多个工具。LLM 收到请求后，知道"我有这些工具可以用"。

**③ 用户提问**：用户说"ThinkPad X1 多少钱"。

**④ LLM 决策输出**：LLM 不直接回复价格（它不知道实时价格），而是返回结构化的 `tool_calls` JSON 数组：`[{"id": "call_abc123", "type": "function", "function": {"name": "queryProductInfo", "arguments": "{\"productId\":\"NB001\"}"}}]`。注意此时 `content` 字段为 null——LLM **完全不生成文本回复，也绝对不执行任何实际代码**，它只负责输出结构化调用决策。

**⑤ 调度执行与沙箱职责划分**：宿主环境（Agent Runtime / Host，如 Spring AI ToolCallingManager）拦截并解析该 tool_call。首先进行**参数强校验与权限判定**。对于**本地工具**，由 Host 在受控沙箱或本地进程环境中执行；对于**远程 MCP 工具**，由 Host/Client 负责工具发现、契约校验与权限调度，实际业务代码由**远程 MCP Server 及其下游服务环境执行**，各执行环境分别承担相应的沙箱隔离、超时控制与异常捕获责任。

**⑥ 结果组装回传 LLM**：把函数执行结果作为 `role=tool` 的消息追加到对话历史，LLM 基于证据生成最终回复："ThinkPad X1 Carbon 当前售价 ¥9999，库存 58 件，在售。"

实际例子中还有一个细节：LLM 自动把"ThinkPad X1"映射到商品 ID "NB001"。这个映射是 LLM 通过函数描述和上下文推断出来的，不需要我们写映射规则。

## Key Emphasis
1. **严格坚守权力边界与沙箱职责**：大模型（LLM）**绝不拥有物理执行权**，它只是认知决策生成器，负责产生结构化 Tool Call 请求。Agent Runtime / Host 负责工具选择、参数校验、权限判定与调度；本地工具由宿主沙箱执行，远程工具由 MCP Server 及其宿主环境执行，各执行环境分别承担相应的沙箱隔离与安全责任。
2. **Schema 质量与约束解码**：Schema 的质量直接决定 LLM 调用的准确性。description 写得清楚，LLM 就能正确判断"什么时候该调这个函数"和"传什么参数"。

## Optional Extension
**学员问：6 步流程里，哪一步最容易出生产事故？**
答：第⑤步"Agent Runtime 执行函数"。前四步错了顶多答得不对，第⑤步错了是企业系统被真实调用。
Demo 做法：Spring AI 拦截 tool_call 后自动执行 FunctionCallback，MCP 调用自带超时预算（`McpProductClient.init()` 里 `.requestTimeout(Duration.ofSeconds(10))`，`McpKnowledgeClient` 15 秒），失败只影响本次回答。
生产做法：治理预算全部押在执行步——入口限流、参数白名单强校验、写操作认幂等键、超时分级（连接超时/读超时/整轮预算）、破坏性操作人机协同审批、失败走补偿、全程审计。本课的治理五件套与安全矩阵专页逐件讲实现；第八课讲调用侧的重试/熔断/降级怎么和这套叠加。

## Take Away
牢记执行权力边界：LLM 是决策参谋（生成 Tool Call JSON），Agent Runtime 才是掌管安全防线与执行权的执政官（校验/沙箱/执行/容错）。Function Calling 六步里只有第⑤步真正碰企业系统——安全校验与治理必须全量布防在第⑤步。

## Why This Slide Matters
跳过这页，学员会把工程精力花在 Prompt 和 Schema 调优上，而事故全出在执行步——函数无限等待挂住会话、失败重试变重复扣款、出事查不到调用记录。

## Transition
既然了解了这6步，那在底层的网络通信中，这个协议的 JSON 请求和响应体到底长什么样呢？我们看下一页：协议底层格式。
