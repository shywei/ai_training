## Slide Objective
展示 Function Calling 的底层协议格式，让学员理解"自动封装"背后发生了什么。

## Suggested Presentation
Function Calling 不是魔法，底层就是 JSON。

**请求格式**：OpenAI API 标准请求中的 `tools` 参数是一个数组（2023 年早期的顶层 `"functions"` 字段早已废弃，现行标准统一为 `"tools"` + `"tool_calls"`），每个元素包含 `"type": "function"` 和 `"function": {"name", "description", "strict", "parameters"}`。`parameters` 是标准的 JSON Schema，定义了参数类型、描述、必填项。开启 `"strict": true`（Structured Outputs for Tools，需配合 `"additionalProperties": false`）后，模型在解码层受 CFG 约束，保证生成的 `arguments` 100% 符合 Schema、不会缺必填项或编造字段；顶层还可传 `"parallel_tool_calls": true` 允许模型单轮并发调用多个工具。这段 JSON 由 Spring AI 自动组装——本地函数走 `FunctionCallbackWrapper` 时反射生成 Schema；本课 `v1.4` Demo 的 MCP Tool 走独立的 `McpToolCallback`，Schema 直接透传 MCP Server `listTools()` 返回的 `inputSchema`。

**响应格式**：LLM 返回的不是普通文本，而是一个包含 `tool_calls` 数组的消息。`content` 为 null，`tool_calls` 是一个数组（启用 `parallel_tool_calls` 时一次请求可同时返回多个工具调用）。每个元素有 `id`（如 `call_abc123`，用于关联回传结果）、`type: "function"`、`function.name`（函数名）、`function.arguments`（JSON 字符串格式的参数）。
**特别强调执行权力边界**：这个响应包只是**大模型输出的结构化决策意图**，模型本身**没有任何实际执行代码的物理能力**。执行权、入参白名单校验、沙箱运行与异常捕获全部唯一归属于宿主环境（Agent Runtime）。

**多轮对话**：执行完函数后，结果作为 `role=tool` 的消息追加到对话历史。tool_call_id 用于关联——"这个结果是哪个 tool_call 的执行结果"。LLM 看到 tool 消息后，基于结果生成最终回复。

Demo 中这些 JSON 是 Spring AI 自动生成的，开发者不需要手写。但理解底层格式很重要。调试时看到 LLM 响应中 content=null、tool_calls 非空，你知道这是正常的 Function Calling 流程，不是报错。很多学员第一次看到这个响应会以为 LLM 出了问题。

**顺带把三个容易混淆的"结构化"方案辨析清楚（页底三卡）——它们是互补，不是竞争。**
① **Native Structured Output**（response_format + json_schema 约束解码）：输出在解码层就被硬约束成 schema，字段类型、必填项由约束保证，不用事后修。适合"最终产物就是结构化数据"：信息抽取、分类、结构化报表。
② **Function Calling**（tool schema → tool_calls）：结构化的不是最终答案，而是"调用意图"（函数名 + 参数），动作交给系统执行。适合要触发 Tool / MCP 的场景。它和 ① 不冲突：① 约束"答什么"，② 约束"做什么"。
③ **JSON mode + Parser**：只保证输出是合法 JSON，不保证合 schema——Parser 校验、解析失败重试、repair 修复链兜底。适合模型不支持约束解码、schema 复杂多变的情况。
选型一行：结构化数据产出→①；触发动作→②；兜底→③+校验修复。生产里三者常串在一起：② 拿参数 → 执行 → ① 把结果生成结构化产出。Demo 现状只用了 ②，最终回复是自由文本——若要结构化产出，在 `PromptManager` 的输出规则之上加 json_schema 约束。

## Key Emphasis
content=null + tool_calls 非空 = LLM 请求调用函数。这不是错误，是 Function Calling 的正常流程。这是调试时最常见的"误以为报错"。
三方案辨析只要记一句：Native 管"答什么"、Function Calling 管"做什么"、JSON mode+Parser 是兜底——按场景分层选，不要把它们当竞品二选一。

## Optional Extension
**学员问：调试时看到 content=null、tool_calls 非空，怎么确认是正常流程而不是模型故障？**
答：对链路标识，不对文本。tool_call 里的 id 是协议层的关联键。
Demo 做法：看 `LlmInteractionLogger` 落在 `logs/llm-chat.log` 的完整请求/响应——`tool_calls[].id` 能和后续 `role=tool` 消息的 `tool_call_id` 对上、函数名在注册表里存在，就是正常流程；对不上或函数名不存在才是协议/注册故障。
生产做法：把 `tool_call_id` 串进调用链追踪（下一课第八课我们将手写 `TraceService.recordToolCall` 实现内存追踪，生产环境接 OpenTelemetry/Jaeger），协议故障（注册缺失、Schema 不合法）和业务失败（函数查不到数据）分开告警——前者归平台，后者归业务。取舍：多存一列关联 id 的成本很低，换来的是分布式调用能拼回完整链路。

**学员问：我要 LLM 输出一个固定结构的 JSON，该用 Native Structured Output 还是 Function Calling？**
答：看你要的是"数据"还是"动作"。产出数据选 Native，触发动作选 Function Calling。
Demo 做法：Demo 要的是动作（查商品、查知识），所以走 Function Calling——`McpToolCallback` 桥 MCP Tool，tool_call 的 arguments 就是结构化的"意图"；最终回复是自由文本。如果要让它输出结构化商品对比表，就在 `PromptManager` 的输出规则上加 json_schema 约束（Native），而不是伪装成一个"假 Tool"骗模型输出 JSON。
生产做法：抽取/分类类任务直接上 Native（约束解码，省掉整条解析-重试-修复链）；工具链路用 Function Calling；老模型或 schema 多变的场景用 JSON mode + Parser + 校验重试（repair）。取舍：伪装成 Function Calling 的"假 Tool"能跑，但语义是"调用"不是"作答"，会污染审计与重试逻辑——骗模型的写法迟早反噬。

## Classroom Interaction
给三个需求让学员当场选方案："把用户这句话抽成{品牌、预算、用途}三字段"（Native）、"查一下 NB001 的库存"（Function Calling）、"模型只保证输出 JSON 但字段老变"（JSON mode+Parser+校验修复）。让学员说出第一个为什么不推荐用"假 Tool"——多数人习惯万物皆 Function Calling。

## Take Away
判别 Function Calling 异常先对三个字段：content 是否为 null、tool_calls[].id 是否有对应的 tool 回执、函数名是否在注册表——三者都对是正常流程，缺哪个就修哪一层。结构化产出选 Native、触发动作选 Function Calling、兜底走 JSON mode+Parser+校验修复——三者组合，不搞二选一。

## Why This Slide Matters
跳过这页，学员会把正常的 tool_call 响应当模型坏了去"修"提示词，或反过来把注册缺失当正常重试——故障在协议层、模型层、业务层之间来回空转，定位时间翻倍。

## Transition
Spring AI 提供了两种方式来定义函数——@Tool 注解和 FunctionCallbackWrapper。用哪种取决于 Spring AI 版本。
