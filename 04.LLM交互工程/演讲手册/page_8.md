## Slide Objective
讲清楚结构化输出的真正目的——不是"展示 JSON"，而是"让 Java 代码能程序化处理 LLM 的输出"。同时讲解两种方案的原理和取舍。

## Suggested Presentation
这一页的核心问题：LLM 的输出是给人看的，还是给代码用的？

聊天场景下，LLM 返回自然语言够了——用户自己能读。但企业系统不一样。用户说"帮我推荐笔记本"，LLM 返回"推荐联想小新Pro，大概 6000 到 8000"——这段话人能看懂，但 Java 代码怎么提取商品名？价格区间？用正则？格式一变就崩了。

如果 LLM 返回 JSON 呢？`{"products":[{"name":"联想小新Pro","price":"6000-8000"}]}`——Jackson 直接映射为 Java 对象，`result.getProducts().get(0).getName()` 就拿到了商品名。可靠、确定、可编程。

坦诚说：第四课的 Demo 还不能完整体现结构化输出的价值——因为当前没有下游系统。等到第七课 Tool/MCP，LLM 返回 JSON 告诉 Java"要调哪个工具、传什么参数"，Java 解析后调用对应 API；等到第八课 Workflow，LLM 返回 JSON 决策结果，状态机根据字段决定下一步走哪里。那时候你会真正理解为什么需要结构化数据。现在是打基础。

怎么让 LLM 返回 JSON？两种方案。

**方案一：Prompt 约束（Demo 已实现）。** 在 system prompt 里要求"只输出 JSON，不要添加任何其他文字"。所有模型都支持，但不保证 100% 合规——模型偶尔还是会加废话。所以代码层兜底是必须的。Demo 里的 StructuredOutputHandler 就是做这个的。

**方案二：API 原生 Structured Output（Demo 未实现，你需要知道）。** OpenAI 在 2024 年推出了 `response_format` 参数。你可以在请求里传一个 JSON Schema，模型保证输出严格符合这个 Schema。原理是模型在生成时做 token 级别的约束——如果下一个 token 会导致输出不符合 Schema，就禁止生成这个 token。

用法是在请求体里加一个字段：
```json
{
  "response_format": {
    "type": "json_schema",
    "json_schema": {
      "name": "recommend",
      "schema": {
        "type": "object",
        "properties": {
          "recommendation": {"type": "string"},
          "products": {"type": "array", "items": {...}}
        }
      }
    }
  }
}
```
模型返回的 JSON 100% 符合你定义的结构。

但有两个限制：第一，只有部分模型支持——OpenAI 原生支持，硅基流动部分模型支持，很多国产模型不支持。第二，Schema 不能太复杂，否则模型可能生成很慢或报错。

**实际项目中的做法：** 两种方案配合使用。首选方案二（response_format），不支持的模型降级到方案一（Prompt 约束），代码层兜底始终保留。这就是为什么 StructuredOutputHandler 的 extractJson 方法要处理多种格式——因为不同方案、不同模型返回的格式都不一样。

看右边的代码。StructuredOutputHandler 做三件事：extractJson 从废话中提取 JSON，validate 做 Schema 验证，buildCorrectionPrompt 在失败时构建重试提示词。

Demo 里可以验证：点前端的"结构化输出演示"按钮，看后端控制台日志。会打印 LLM 原始输出、提取出的 JSON、Bean Mapping 后的 Java 对象字段。三个步骤一目了然。

有个踩坑经验：extractJson 方法最初只处理了两种情况——纯 JSON 和代码块包裹。后来发现模型有时会在 JSON 前面加一段中文说明，后面再加一句"希望对你有帮助"。所以加了第三种处理：找第一个 { 到最后一个 }。这三种情况覆盖了大部分 LLM 输出格式。

## Key Emphasis
核心是"给代码用"，不是"给用户看"。两种方案不是二选一，而是配合使用。代码层兜底始终保留——这是工程思维，不是偷懒。

## Transition
结构化输出解决了"怎么拿到干净数据"。但还有一个更基础的问题：怎么让大模型按你的意图行事？这就是 Prompt 工程。
