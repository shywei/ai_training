# 第 04 页：同构提效（一）：Prompt 组装与模型调用 ChatClient

## Slide Objective
通过五步闭环讲透 `PromptManager` → `ChatClient.builder()` 的演进：明确问题、概念、分离装配原理、架构未变的本质，并展示 V5 与 V6 的真实代码对照（Demo 已覆盖）。

## Suggested Presentation
各位同学，进入第一部分“同构效率提升”，我们先看最基础的环节：**怎么拼装 Prompt 并调用大模型？**

请顺着页面上方这 4 个格子把逻辑串起来：
1. **V5 存在什么问题？** 看下方左侧的 `PromptManager.java`（65 行）：每次调用前，我们得手写 `String.format` 替换人设变量，手动 `new ArrayList<Message>()` 往里加 `SystemMessage` 和 `UserMessage`，构造 `Prompt` 对象调用 `chatModel.call()`，再写一长串 `.getResult().getOutput().getText()` 取结果——而且安全过滤 `guardrailsService.sanitize()` 还得在每个业务方法里人工调一遍，非常容易漏！
2. **引入什么概念？** Spring AI 2.0 推出了 **ChatClient Fluent 声明式 API**，设计风格完全对齐大家熟悉的 `RestClient` 和 `WebClient`。
3. **底层原理是什么、怎么解决问题？** 它的核心原理是**“配置期与运行期分离”**。请看右侧 `SpringAi2ChatConfig.java`：在 Spring Boot 启动配置期，我们用 `ChatClient.builder(chatModel)` 一次性把全局的 `defaultSystem`、拦截器链 `defaultAdvisors` 和工具集 `defaultTools` 挂载好，注册为一个 Bean；到了运行期，业务类 `SpringAi2WorkflowAdapter` 只需要一行代码 `chatClient.prompt().user(promptContext).call().content()`，内部就会自动帮你组装好消息列表、走完拦截链并解包出文本！
4. **架构评估**：这一步改变，从 Agent 架构角度看**没有任何变化**——底层发给大模型的依然是 System Prompt + User Prompt。它改变的是代码组织方式，把配置样板代码从 65 行压缩到 25 行，且杜绝了业务端漏挂安全拦截的风险。

## Key Emphasis
1. **真实 Demo 位置**：`enterprise-ai-agent-springai2` 中的 `SpringAi2ChatConfig.java` 与 `SpringAi2WorkflowAdapter.java`。
2. **架构清醒度**：`ChatClient` 并没有发明新的 Agent 架构，它只是把过程式的消息包装变成了声明式 Builder，属于纯粹的开发效率提升。

## Optional Extension
### Q1: 如果某一次业务请求需要临时换一套 System 变量，或者把模型输出直接转成 Java 对象，怎么调用？
**讲师解析**：`ChatClient` 支持在运行时按次覆盖或增强：比如 `.prompt().system(s -> s.param("agentName", "VIP导购")).user(msg).call().entity(ShoppingIntent.class)`。使用 `.entity(...)` 时框架会自动附加结构化 JSON Schema 并在响应后反序列化为对应 Java 类。

## Classroom Interaction
“对比左右两段代码：如果现在要求所有大模型请求前都加一条日志切面，右边代码需要改业务调用方的那一行 `chatClient.prompt()...` 吗？”（答案：不需要，只需改配置类的 `defaultAdvisors`）。

## Take Away
`ChatClient.builder()` 通过“配置期固化 + 运行期链式调用”，消除了消息包装与切面漏调的样板代码，但底层交互架构未变。

## Why This Slide Matters
本页只聚焦 `ChatClient` 这一件事，做到上方概念与下方代码 100% 严密对应，让学员立刻学会怎么配、怎么调。

## Transition
搞定了 `ChatClient` 的 API 写法，那为什么 Spring AI 2.0 还要把底座拉升到 JDK 21 和 Spring Boot 4.0 呢？接下来 P05 我们专门把这两项底座升级的原理和配置代码讲透。
