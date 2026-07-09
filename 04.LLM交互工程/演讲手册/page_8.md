## Slide Objective
讲清楚结构化输出的两种方案和代码层兜底，让学员理解"为什么需要双重保障"。

## Suggested Presentation
企业痛点：你让大模型返回 JSON，它可能返回"好的，以下是推荐：{...} 希望对你有帮助！"。Jackson 直接解析报错。

两种解决方案。方案一：在 Prompt 里明确要求"只输出 JSON，不要添加任何其他文字"。所有模型都支持，但不保证 100% 合规——模型偶尔还是会加废话。方案二：用 API 原生的 response_format 参数强制输出 JSON Schema。合规率接近 100%，但只有部分模型支持。

不管用哪种方案，Java 端都必须做兜底：JSON 提从（从废话中提取 JSON 部分）、Schema 验证（检查字段是否齐全）、Bean Mapping（用 Jackson 映射为 POJO）。

看右边的代码。StructuredOutputHandler 做三件事：extractJson 从 LLM 输出中提取 JSON（处理纯 JSON、代码块、混合文本三种情况），validate 做 Schema 验证，buildCorrectionPrompt 在验证失败时构建重试提示词。

## Key Emphasis
两种方案不是二选一，而是配合使用。代码兜底是必须的，不管用哪种方案。

## Take Away
提示词约束是通用方案，代码验证是必须的兜底，两者缺一不可。

## Transition
结构化输出解决了"怎么拿到干净数据"。但还有一个更基础的问题：怎么让大模型按你的意图行事？这就是 Prompt 工程。
