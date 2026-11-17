# 第 08 页：同构提效（五）：CallAdvisor 与 StreamAdvisor 双向拦截链

## Slide Objective
以真实 Spring AI 2.0.2 GA API（`ChatClientRequest` / `ChatClientResponse` / `CallAdvisorChain` / `StreamAdvisorChain`）讲透 `GuardrailsCallAdvisor` 与 `TelemetryStreamAdvisor` 如何取代散落的 `GuardrailsService`（Demo 已覆盖）。

## Suggested Presentation
各位同学，在 V5 中，我们的安全过滤 `GuardrailsService` 是散落在 Controller 和 Runtime 里手工显式调用的——新来的同事写一个新接口，一忘掉调 `checkInjection()` 或 `sanitize()` 就直接裸奔了。同时，如果是流式响应，很难在每个分片输出时统计首字延迟 TTFT。

为了解决“横切逻辑散落、容易漏调”的问题，Spring AI 2.0 确立了**双向洋葱责任链机制**：同步调用实现 `CallAdvisor`，流式调用实现 `StreamAdvisor`。（顺带避坑：早期 1.0 Milestone 版本的 `CallAroundAdvisor` 和 `AdvisedRequest` 在 2.0.2 GA 已经全面重构为 `CallAdvisor` 和 `ChatClientRequest` / `ChatClientResponse`。）

它的工作原理和大家熟悉的 `Servlet Filter` 完全一致：
- 每个 Advisor 通过 `getOrder()` 声明优先级；
- 请求**入站**时，按 Order 从小到大依次执行前置逻辑；
- 穿过 `chain.nextCall(req)` 或 `chain.nextStream(req)` 进入底层模型；
- 响应**出站**时，按相反顺序依次执行后置审查与打点。

请看屏幕下方左右两段在 `enterprise-ai-agent-springai2` 中真实编译通过的代码：
- **左边 `GuardrailsCallAdvisor`**：入站先做 Prompt 注入拦截，再把手机号、身份证号脱敏重写进 `UserMessage`；调完 `chain.nextCall(req)` 后，在出站阶段对敏感密钥词打码。
- **右边 `TelemetryStreamAdvisor`**：针对流式响应，通过 `chain.nextStream(req).map(...)` 捕获第一个分片到达的时刻，精准算出 TTFT 首字延迟。切记一条红线：**绝对不要在 `adviseStream` 的 `map()` 里调阻塞式数据库或 Redis**，否则会直接卡死 Netty EventLoop！

最后只需在 `SpringAi2ChatConfig` 的 `.defaultAdvisors(List.of(guardrailsAdvisor, repairAdvisor, telemetryAdvisor))` 一挂，全局所有请求自动受保护。

## Key Emphasis
1. **严格对齐真实 2.0.2 API**：`adviseCall(ChatClientRequest, CallAdvisorChain)`、`adviseStream(ChatClientRequest, StreamAdvisorChain)`、`getName()`、`getOrder()`。
2. **两项代码全覆盖**：同步安全守卫 + 流式 TTFT 监控均给出对应真实代码。
3. **响应式避坑**：`StreamAdvisor` 内严禁同步阻塞 I/O。

## Optional Extension
### Q1: 在我们的 `enterprise-ai-agent-springai2` 工程中，`GuardrailsCallAdvisor` 拦截同步调用，如果是流式调用怎么也加上安全过滤？
**讲师解析**：只需要让 `GuardrailsCallAdvisor` 同时实现 `StreamAdvisor` 接口（在 `adviseStream` 中先对入站 `request` 做注入校验和 PII 脱敏，再对流式输出块做增量敏感词缓冲过滤），就可以同时覆盖 `.call()` 和 `.stream()` 两条路径。

## Classroom Interaction
“对比 Spring MVC 的 `HandlerInterceptor`，如果我有 `GuardrailsCallAdvisor`（Order=100）和 `SchemaRepairCallAdvisor`（Order=200），入站时谁先执行？出站时谁先执行？”（入站 100 先，出站 200 先回溯、100 最后收尾）。

## Take Away
用 `CallAdvisor` 与 `StreamAdvisor` 把安全拦截和遥测监控收敛到统一洋葱责任链中，业务代码无需再操心横切逻辑。

## Why This Slide Matters
确保学员掌握真实可用的 Spring AI 2.0.2 GA Advisor 编程接口，并理解同步与流式两条拦截链的运行原理与避坑红线。

## Transition
到这里，Spring AI 2.0 的四大王牌（`ChatClient`、虚拟线程/可观测、`@Tool`+内置MCP、`Advisor` 链）我们已经全部看完了。那用上了 Spring AI 2.0 之后，我们的 `SpringAi2WorkflowAdapter` 到底长什么样？是不是真的“几行代码搞定整个 Agent”了？请看揭示真相的 P09！
