## Slide Objective
展示 context 参数的演进——贯穿后续所有课程的主线。

## Suggested Presentation
这一页既是知识也是 Demo。左边是 context 的演进路线：v1.0 是 null，v2.0 是画像加对话历史，v3.0 会加入 RAG 检索结果，v4.0 会加入 Tool 调用结果。

关键设计决策：PromptManager 的接口 buildUserPrompt(message, context) 不变，变的是 context 的内容。这就是为什么第四课要把 Prompt 构建独立成一个组件——为后续扩展留好接口。

右边是 v2.0 的 context 实际内容。三部分：用户画像（事实、偏好、任务、约束）、对话历史（用户和助手的来回消息）、用户当前问题。这些全部拼接成一个字符串注入到 User Prompt 里。

验证：看 logs/llm-chat.log 里的完整 System Prompt，能直观看到 context 是怎么注入的。

## Transition
context 注入讲完了。接下来讲企业必须面对的问题：安全与隐私。
