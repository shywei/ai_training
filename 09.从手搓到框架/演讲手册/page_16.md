# 第 16 页：Middleware 落地与对比：onActing 无损挂起 HITL

## Slide Objective
展示 `LifecycleMiddleware.onActing()` 实现高危工具无损挂起 HITL 的真实代码与挂载方式（Demo 已覆盖），并用四维矩阵彻底对比 Spring AI `Advisor` 与 AgentScope `MiddlewareBase` 的选型边界。

## Suggested Presentation
各位同学，直接看屏幕上半部分的真实代码——这就是 8084 工程里 `LifecycleMiddleware.java` 的核心片段，它完全演示了认知钩子怎么实现“人在回路（HITL）无损挂起”：

当 `ReActAgent` 推演出需要调用 `createOrder` 工具时，框架在真正执行工具方法之前，会先回调所有中间件的 `onActing()` 方法：
1. **第一步（领域规则判定）**：从 `ActingInput` 里拿出 `ToolUseBlock` 的参数，交给纯 Java 的 `domainService.isHighRiskAction()` 判定（比如金额 $\ge ¥5000$）；
2. **第二步（保存断点快照）**：一旦命中高风险，立刻构造 `AgentCheckpoint`（状态标为 `WorkflowState.CONFIRMING`）存入 `checkpointStore`，并在 `RuntimeContext` 打上标记；
3. **第三步（发射挂起事件短路执行）**：注意看最后这句——`return Flux.just(new RequireUserConfirmEvent(cpId, List.of(toolCall)));`！它**没有调用 `next.apply(in)`**，也就是直接短路跳过了底层工具执行，安全地把请求控制权交还给前端弹出确认框！

而如果是查询商品规格这种低风险工具，直接走 `return next.apply(in);` 无缝放行，对业务工具代码**零侵入**！

接着看下半部分的**四维选型对照表**：
- 如果你只需要做**全局 PII 脱敏、注入防护、Token 计数和请求日志**，用 Spring AI `Advisor` 就够了；
- 但只要涉及**高危工具执行前风控挂起、思维链审计、异常触发自愈**，AgentScope 的 `MiddlewareBase` 就是降维打击。

## Key Emphasis
1. **看懂“短路挂起”原理**：不调 `next.apply(in)` 而是直接 `return Flux.just(new RequireUserConfirmEvent(...))`，工具就不会被真实执行。
2. **零侵入解耦**：风控拦截全写在 `LifecycleMiddleware`，`ShoppingAgentTools` 里不需要写任何审批状态判断代码。

## Optional Extension
### Q1: 当用户在前端点击“确认批准下单”后，之前挂起的 `createOrder` 工具是怎么接着执行的？
**讲师解析**：用户点击确认后，前端调用 `POST /api/v7/workflow/confirm?executionId=...&checkpointId=...&confirmed=true`。后端 `AgentScopeWorkflowAdapter.confirmAction()` 加载该 `AgentCheckpoint`，取出 `pendingConfirmation` 中暂存的 `createOrder` 工具参数并触发 `enterpriseToolPort.createOrder()` 完成正式成单，再将 Checkpoint 推进至 `COMPLETED` 终态。

## Classroom Interaction
“注意看代码里第 4 行的 `next.apply(in)`：如果把 `return Flux.just(RequireUserConfirmEvent)` 错写成先调 `next.apply(in)` 再发射事件，会发生什么线上事故？”（钱已经扣了/订单已经下了，才弹窗问用户同不同意！）。

## Take Away
在 `onActing` 中通过“领域风控判定 ➔ 保存 Checkpoint ➔ 短路发射 `RequireUserConfirmEvent`”，无需修改业务工具即可实现 HITL 无损挂起。

## Why This Slide Matters
用不到 15 行真实代码把“认知钩子如何落地企业级审批挂起”讲透，同时形成清晰的 `Advisor` vs `Middleware` 选型参考。

## Transition
解决了状态恢复和审批挂起，接下来看第三个 V5 极难做好的企业级能力（P17）：交互体验！怎么告别“算完再拆字”的伪流式，把思考、工具进度、正文和审批分通道实时推给前端？
