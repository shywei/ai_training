# 第 06 页：同构提效（三）：@Tool 声明式契约 + ToolCallingAdvisor 自动闭环

## Slide Objective
讲透 V5 → V6 工具声明与调度的同构升级：如何用 `@Tool` + `@ToolParam` 消除手写 Schema 胶水，以及内置 `ToolCallingAdvisor` 在幕后自动完成工具调用递归闭环的完整原理（Demo 已覆盖）。

## Suggested Presentation
各位同学，回忆一下第 07、08 课我们在 V5 里写工具调用有多痛苦：不仅要手写 80 行的 `McpToolCallback` 去拼参数描述，还得在 `AgentRuntime.java` 里写 95 行正则表达式 `Pattern.compile("TOOL_CALL:(\\w+)\\((.*?)\\)")` 去从大模型输出的自然语言里硬抠工具名和参数！大模型只要多输出一个换行、或者一个中文括号，正则立刻失配，整个任务卡死。

在 V6（Spring AI 2.0）中，官方用两个核心组件彻底消灭了这两块胶水：

1. **第一块：`@Tool` + `@ToolParam` 声明式契约推导**。请看左下角 `SpringAi2ToolAdapter.java`：你只需要在普通的 Java 方法上加 `@Tool(name="queryProductInfo", description="...")`，参数上加 `@ToolParam`。框架启动时会自动通过反射把方法签名转成标准 JSON Schema，走大模型原生的 `tools` 协议字段，再也不靠 Prompt 文本约定格式！
2. **第二块：内置 `ToolCallingAdvisor` 自动接管闭环**。很多同学奇怪：我只写了 `@Tool` 方法，谁来解析模型返回的 JSON、谁来反射调方法、谁把结果再喂回给模型？答案就是 `ChatClient` 默认内置挂载的 `ToolCallingAdvisor`！请看右侧的三步工作流：
   - Step 1：发请求时自动带上所有 `@Tool` 的 JSON Schema；
   - Step 2：收到模型响应后检查 `chatResponse.hasToolCalls()`；
   - Step 3：如果有工具调用意图，内部 `ToolCallingManager` 自动反序列化参数、反射执行你的 Java 方法，再把返回值包装成 `ToolResponseMessage` 自动回填模型发起下一轮推理，直到模型输出最终文本！

因此，原来 V5 里 95 行的正则解析与调度循环，在 V6 里**直接归零**。但请大家特别注意右下角的警示：默认 `ToolCallingAdvisor` 是在单次请求里**一口气把工具跑完**的，如果想中途拦截人工确认，就需要关闭自动执行或交给更高阶框架。

## Key Emphasis
1. **两项严格对应**：上方讲 `@Tool` 与 `ToolCallingAdvisor`，下方分别给出 `SpringAi2ToolAdapter` 代码和 `ToolCallingAdvisor` 闭环机制 + 关闭开关代码。
2. **架构判断**：这依然是同构功能替换——底层同样走大模型 Function Calling 与 Tool 执行观察循环，只是省去了手写 Schema 和正则解析胶水。

## Optional Extension
### Q1: 既然 `ToolCallingChatOptions.internalToolExecutionEnabled(false)` 可以关闭自动工具执行，为什么不推荐在复杂多步 Agent 里自己关掉重写？
**讲师解析**：因为一旦关闭，你就又得自己写 `while (chatResponse.hasToolCalls())` 循环去手动调用 `ToolCallingManager` 并管理上下文消息列表，又退回到了自己造循环轮子的老路上。

## Classroom Interaction
“大家可以想一下，如果在 `@Tool` 和 `@ToolParam` 里不写 `description`，会发生什么？”（大模型只看得见 JSON Schema 里的 `description` 和字段名，不写清楚就会猜错参数或选错工具）。

## Take Away
用 `@Tool` 声明工具契约，让内置 `ToolCallingAdvisor` 自动完成“识别 ➔ 执行 ➔ 回填”闭环，彻底告别脆弱的正则表达式。

## Why This Slide Matters
不仅教会学员怎么打 `@Tool` 注解，更把幕后黑盒 `ToolCallingAdvisor` 的工作原理与适用限制一次性讲透。

## Transition
本地 Java 方法变成工具只需要一个 `@Tool` 注解，那第 07 课我们对接的 8081 商品库和 8082 知识库这两个外部 MCP Server，在 V6 里又该怎么接？请看 P07。
