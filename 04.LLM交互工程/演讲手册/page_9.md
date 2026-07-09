## Slide Objective
把 Prompt 的结构设计、调试、容错和安全合在一页讲，区分 Demo 覆盖和仅 PPT 讲解。

## Suggested Presentation
Prompt 工程分四个维度。

结构设计——三种角色的职责划分。System Prompt 定义 Agent 是谁、能做什么、不能做什么。User Prompt 是用户的实际问题，后续课程会在这里注入 RAG 结果和用户画像。Few-shot 给模型看例子，是最有效的格式控制技巧。这三个 Demo 都覆盖了。

调试四件套——Playground 快速迭代、Logging 记录输入输出、AB Test 对比效果、Evaluation 量化准确率。这四个 Demo 没有实现，是企业级 Prompt 管理的扩展知识。

两道防线——入口防 Prompt Injection（正则检测、关键词拦截），出口做敏感信息脱敏。两道防线各自独立，不合并。Demo 已覆盖，代码在 GuardrailsService 里。

Self-Correction——LLM 输出非法 JSON 时，Java catch 捕获异常，把异常信息和原始输出发回 LLM，让它重新生成。注意：这是服务端重试模式，不是 LLM 自己纠错。Demo 已覆盖。

底部还提了一个 Streaming 和 Guardrails 的冲突：流式输出时不能逐 chunk 检查，会截断句子。Demo 没有处理这个问题，是进阶知识。

## Key Emphasis
标注清楚哪些是 Demo 覆盖的（结构设计、两道防线、Self-Correction），哪些是仅 PPT 讲解的（调试四件套、Streaming 冲突）。不要让学员误以为所有内容都在 Demo 里。

## Take Away
Prompt 是代码，不是文案。结构设计、安全防御、自动重试，三者缺一不可。

## Transition
Prompt 工程讲完了。现在我们看 Demo 里的两个核心组件：PromptManager 和 GuardrailsService。
