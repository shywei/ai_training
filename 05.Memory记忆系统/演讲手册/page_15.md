## Slide Objective
展示 v2.0 的完整文件结构，帮学员建立"怎么读 Demo 代码"的路线图。

## Suggested Presentation
这是 v2.0 的完整文件结构。18 个 Java 文件，看起来很多，但有清晰的分层。

**读代码的顺序建议：**

第一步：先看 `ShoppingAgentController.java`。这是入口，所有请求都从这里进来。看 chat 方法——它调用了 MemoryManager.buildContext 构建上下文，调用 PromptManager.buildUserPrompt 构建 Prompt，然后调 LLM。看完这个文件，你就理解了整个请求流程。

第二步：看 `MemoryManager.java`。这是 Memory 的核心。buildContext 方法做了什么？从 MySQL 查用户画像，从 Redis 查对话历史，拼成一个字符串。就这么多。这个方法是 context 注入的枢纽——后续加 RAG、加 Tool，都是在这个方法里追加内容。

第三步：看 `SessionMemory.java`。Redis 操作。addMessage 追加消息、getMessages 读取全部、滑动窗口裁剪。代码很简单，但要理解"为什么用 List 而不是 Hash"。

第四步：看 `SummaryPipeline.java`。压缩逻辑。summarizeSession 方法取出旧消息，拼 Prompt，调 LLM 压缩，写回 Redis。重点看 Prompt 的写法——它告诉 LLM 要合并旧摘要。

第五步：看 `UserProfileExtractor.java`。画像抽取。extract 方法取出旧画像和新对话，拼 Prompt，调 LLM 合并。重点看旧画像是怎么传给 LLM 的。

其他文件：`User.java`、`UserMapper.java`、`AuthController.java` 是登录相关的，`LlmInteractionLogger.java` 是日志，`GuardrailsService.java`、`ModelRouter.java`、`StructuredOutputHandler.java` 是第四课的，不需要重新看。

**日志文件是调试的核心工具：**

`logs/llm-chat.log`——每次对话的请求和响应。看 System Prompt 里注入了什么 context。这是验证"Memory 有没有生效"的最直接方式。

`logs/llm-summary.log 或 llm-profile.log`——画像抽取和压缩的完整过程。看旧画像 JSON、对话内容、LLM 返回的合并结果。这是验证"画像抽取有没有工作"的最直接方式。

**四个模型配置从 yml 读取，不硬编码：**

`agent.model.fast`——对话路由（快模型）
`agent.model.reasoning`——对话路由（推理模型）
`agent.model.summary`——上下文压缩（快模型）
`agent.model.extract`——画像抽取（推理模型）

四个配置独立，可以随意组合。换模型只改 yml，不改代码。

## Key Emphasis
"怎么读 Demo 代码"比"Demo 有什么文件"更重要。给学员一个明确的阅读路线图，让他们能自己读懂代码。

## Transition
最后做个总结。
