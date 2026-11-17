# 第 07 页：同构提效（四）：官方内置 MCP Client 替代手写桥接

## Slide Objective
讲透 V5 → V6 外部 MCP 工具接入的演进：说明 `spring-ai-starter-mcp-client` 与 `SyncMcpToolCallbackProvider` 如何通过配置替代 493 行手写客户端与桥接代码，并诚实说明当前 Demo 的降级兜底设计。

## Suggested Presentation
各位同学，第 07 课我们为了连接 8081 商品服务和 8082 知识库这两个独立的 MCP Server，在 V5 里足足写了三个大类：`McpProductClient.java`（201 行）、`McpKnowledgeClient.java`（212 行）和 `McpToolCallback.java`（80 行），加起来将近 500 行代码——全是在手动建立 SSE 传输、发 `initialize` 握手、把 MCP 工具描述转成 Spring AI 回调。

在 V6（Spring AI 2.0）里，接入远端 MCP Server 需要写多少代码？答案是**零客户端连接代码，只需配置 + 一行挂载**！

请看右侧代码框的机制：
1. 引入官方依赖 `spring-ai-starter-mcp-client`；
2. 在 `application.yml` 中声明 `spring.ai.mcp.client.sse.connections`，把 8081 和 8082 的 URL 填进去；
3. 框架启动时自动创建 `McpSyncClient`、完成协议握手与工具列表发现，并把它们打包成一个 Spring Bean——`SyncMcpToolCallbackProvider`；
4. 在 `SpringAi2ChatConfig.java` 里，通过 `builder.defaultToolCallbacks(mcpProvider)` 把远端 MCP 工具直接挂进 `ChatClient`！

这里我必须**如实说明工程边界**：在我们的 `enterprise-ai-agent-springai2` 示例工程里，`SpringAi2ChatConfig` 已经写好了 `ObjectProvider<SyncMcpToolCallbackProvider>` 的自动挂载逻辑；但为了让学员在**没有启动 Docker 和 8081/8082 两个 MCP 进程**时也能独立跑通 Demo，我们在 `application.yml` 里默认把 `spring.ai.mcp.client.enabled` 设为 `false`，由 `SpringAi2ToolAdapter` 提供 HTTP 请求 + 本地 Mock 的平滑兜底。现场演示完整联动时，只需把 `enabled` 改成 `true` 即可。

## Key Emphasis
1. **五步闭环清晰**：500 行手写客户端/桥接痛点 ➔ `SyncMcpToolCallbackProvider` 概念 ➔ Starter 自动建连发现原理 ➔ YAML 配置与 `defaultToolCallbacks()` 调用。
2. **严守诚实标注**：清楚告诉学员 Demo 默认 `enabled: false` 配合本地兜底运行，避免误导。

## Optional Extension
### Q1: 在 V7（AgentScope 2.0）工程里是怎么连 8081/8082 MCP Server 的？
**讲师解析**：在 `enterprise-ai-agent-agentscope` 中，为了贯彻六边形架构，我们在 `adapter/tool/McpEnterpriseToolAdapter.java` 中通过官方 `io.modelcontextprotocol.sdk:mcp` 实现统一的 `EnterpriseToolPort` 接口，同样具备连不上 8081/8082 时自动降级内置商品库和知识库的能力，保证离线演示 100% 稳定。

## Classroom Interaction
“对照第 07 课手写 `McpToolCallback` 的经历，现在把连接池和工具发现交给官方 Starter 后，我们研发人员还应该把精力放在 MCP 的哪一部分？”（答案：放在 MCP 工具的业务契约设计、鉴权与治理五件套上，而不是重复写 SSE 握手）。

## Take Away
官方 `spring-ai-starter-mcp-client` 把数百行 MCP 协议握手与回调桥接胶水收敛为几行 YAML 配置与 `defaultToolCallbacks()` 挂载。

## Why This Slide Matters
把原版 PPT 仅在标题提了一句的“官方内置 MCP Client”彻底展开，给出完整配置、挂载代码与 Demo 真实开关说明。

## Transition
Prompt 组装、工具调用、MCP 连接都看完了，V6 在横切安全过滤和流式监控上又是怎么取代 V5 的 `GuardrailsService` 的？请看 P08。
