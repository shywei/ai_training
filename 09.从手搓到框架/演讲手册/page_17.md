# 第 17 页：企业级增量（三）：多通道类型化事件流 streamEvents

## Slide Objective
讲透为什么 V5 的“同步阻塞 + 延迟拆字”伪流式与 V6 的“单通道文本流”无法满足现代 Agent UI 交互需求，剖析 AgentScope `streamEvents()` 类型化 `AgentEvent` 多通道解耦原理与 `AgentEventStreamBridge` 真实实现（Demo 已覆盖）。

## Suggested Presentation
各位同学，大家应该用过 DeepSeek 或现代 AI 助手：页面顶部先实时出现可折叠的“深度思考中...”，接着跳出“正在查询商品库存”卡片，然后才开始逐字打印正式回答，遇到高额下单还会弹出审批按钮。

回头看看我们在第 08 课 V5 里怎么做的 SSE 流式？当时受限于 `AgentRuntime` 是同步阻塞循环，后端把所有步骤跑完拿到最终结果字符串，再用 `Flux.interval(15ms)` 把字符串拆成单个汉字推给前端——**这就是典型的“伪流式”**！用户在等待 5~8 秒期间前端毫无动静，既看不到思考过程，也不知道工具在查什么。
到了 V6（Spring AI 2.0），虽然有真流式 `Flux<ChatResponse>`，但它只是一条**单通道文本流**，思考标签、工具调用和正文回答全混在字符串里，前端只能靠脆弱的正则去切分。

现在看 AgentScope 2.0（V7）是怎么从底层解决这个问题的：
它独立于拦截机制之外，原生提供了 `shoppingAgent.streamEvents(message, context)`，在推理和执行的每个瞬间发射**30+ 种强类型 `AgentEvent`**，天然把数据切分成四个独立通道（见左侧）：
1. **思考过程流（thought）**：`THINKING_BLOCK_START / DELTA / END`，前端直接塞进折叠思考卡片；
2. **工具执行流（tool）**：`TOOL_CALL_START / DELTA` 和 `TOOL_RESULT_END`，0 毫秒点亮“正在查 NB001 库存”状态条；
3. **正文回答流（answer）**：`TEXT_BLOCK_START / DELTA / END`，纯粹的打字机回复；
4. **审批与自愈控制流（confirm / repair）**：`REQUIRE_USER_CONFIRM` 与 `CUSTOM`，直接驱动前端弹窗或提示。

在右栏 `AgentEventStreamBridge.java` 中，我们只需要写一个简单的 `switch (event.getType())`，就能把这些原生类型化事件桥接成六边形领域统一的 `WorkflowStreamEvent` 通过 SSE 实时推出去！

## Key Emphasis
1. **三代流式体验本质对比**：V5 算完拆字伪流式（3~8s 延迟） ➔ V6 单通道文本流（通道混杂） ➔ V7 类型化多通道事件流（<100ms 即刻反馈且前后端彻底解耦）。
2. **与真实源码一致**：引用 `AgentEventType` 枚举与 `AgentEventStreamBridge` 映射代码（Demo 已覆盖，且有 `StepVerifier` 单元测试验证）。

## Optional Extension
### Q1: 在只接入普通对话、不需要调多步工具的场景下，Spring AI 2.0 的单通道 `Flux<ChatResponse>` 够用吗？
**讲师解析**：完全够用！如果只是常规问答或 RAG 总结，不需要区分思考过程与多步工具调用状态卡片，直接用 `chatClient.prompt().stream().content()` 即可，开发最简单。只有当你的产品形态需要分通道呈现“思考链 + 工具进度卡片 + 正文 + 审批弹窗”时，多通道 `AgentEvent` 才能发挥巨大威力。

## Classroom Interaction
“请大家看右侧终端返回的 3 行真实 SSE 数据：前端拿到这种带 `type` 字段的 JSON 后，还需要写正则表达式去识别大模型是不是在调用工具吗？”（完全不需要，直接按 `type` 路由到对应 UI 组件）。

## Take Away
`streamEvents()` 通过强类型事件实现“思考、工具、正文、审批”多通道物理分离，彻底消灭伪流式与前端正则拆包。

## Why This Slide Matters
让学员真正看懂现代 Agent 应用前后端分离实时交互的底层工程机制，知道为什么多通道事件流是复杂 Agent 的标配。

## Transition
至此我们攻克了 Checkpointer、Middleware 和多通道事件流。还剩最后一个在企业生产里天天碰到的顽疾（P18）：大模型调工具时老传错参数格式怎么办？
