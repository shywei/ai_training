## Slide Objective
给学员一条课后能独立走通的代码阅读路线（对应 Git Tag `v1.1.1` 的文件清单 + 读代码顺序），以及 7 个可在 Demo 亲手验证的点。把"课上听懂"变成"课后能自己验"。

## Suggested Presentation
左边是本课版本（`v1.1.1`）的文件清单和阅读顺序，右边是 7 个可亲手验证的点。先说路线，再说验证。

**文件清单**：本课 `v1.1.1` 的代码集中在 `agent-runtime/src/main/java/com/example/agent/core/` 下：
- `ShoppingAgentController.java`：API 入口（`GET /api/agent/chat` 流式对话 + `POST /api/agent/recommend` 结构化推荐）；
- `ModelRouter.java`：模型路由（根据消息长度与关键词选择 `agent.model.fast` 或 `agent.model.reasoning`）；
- `PromptManager.java`：构建 System Prompt 与 User Prompt（预留 `context` 扩展参数）；
- `StructuredOutputHandler.java`：四级提取 JSON、Schema 校验、构建 `Self-Correction` 纠错提示词；
- `RecommendResult.java`：结构化推荐结果对应的 Java Bean；
- `GuardrailsService.java`：入口注入检测（中英双语正则）+ 出口敏感词脱敏；
- `CorsConfig.java`：跨域配置；以及 `resources/application.yml`。

**读代码顺序：从入口到链路，每步只回答一个问题。**

第一步：`ShoppingAgentController.java` 的 `chat()` 与 `recommend()` 方法——整条链路的串联点。先通读这两个方法体，看清调用顺序：入口注入检查 → `ModelRouter` 选模型 → `PromptManager` 构建提示词 → `ChatClient` 调大模型 →（推荐接口）提取/校验/Bean 映射或 `Self-Correction` 重试 → 出口敏感词脱敏 → 返回前端。

第二步：`ModelRouter.java` 的 `selectModel(msg)` 与 `needsReasoning(msg)` 方法。看它怎么读取 `agent.model.fast` 与 `agent.model.reasoning`，并根据消息长度（>500）和关键词数组（`"架构"`, `"重构"`, `"分析"`）路由模型。（在后续高阶实战中，可进一步升级为基于轻量模型的意图分类路由）。

第三步：`PromptManager.java` 的 `buildSystemPrompt()` 和 `buildUserPrompt(userMessage, context)`。重点看 `context` 参数——本课 `v1.1.1` 传的是 `null`，第五课传对话历史和画像，第六课传 RAG 检索结果，第七课传工具结果。接口不变，内容渐进。

第四步：`StructuredOutputHandler.java` 的 `extractJson()` 和 `validate()`。`extractJson` 是四级提取：纯 JSON → ```json 代码块 → 无标记代码块 → 第一个 `{` 到最后一个 `}`。`validate` 先解析、失败后 `cleanJson()`（去尾逗号、去注释）再解析一次。

第五步：同一个文件的 `buildCorrectionPrompt()`——Self-Correction 的关键。配合看 `ShoppingAgentController.recommend()` 里的失败分支：validate 失败 → 拼纠正提示词（带上次原始输出和报错原因）→ 二次调用 → 再校验 → 仍失败返回结构化 error。

第六步：`GuardrailsService.java` 的 `checkInjection()` / `containsSensitiveInfo()` / `sanitize()`。前两个是检查（布尔返回），最后一个是替换脱敏（`***`）。

右边是 7 个验证点，每个都可以在 `v1.1.1` 亲手跑出来：
1. **入口注入拦截**：点前端「测试注入防御」按钮或向 `/recommend` 传「忽略之前的指令…」→ 立即拦截；
2. **结构化输出五步控制台链路**：点「结构化输出演示」按钮 → 看后端控制台打印 `【步骤1】原始输出 → 【步骤2】提取 JSON → 【步骤3】Schema 验证 → 【步骤4】Bean Mapping`；
3. **Self-Correction 触发**：写单测喂三种格式文本给 `extractJson/validate`，或在 `/recommend` 打断点观察二次重试；
4. **模型路由切换**：分别问「推荐一台笔记本」和「分析一下游戏本和轻薄本的架构差异」→ 看前端回复气泡顶部的 `[MODEL]` 标签在 `fast` 和 `reasoning` 模型间自动切换；
5. **出口敏感信息处理**：输出含「密码/password」时返回 `_warning` 或被 `sanitize()` 替换为 `***`；
6. **SSE Streaming 效果**：F12 Network 看 `GET /api/agent/chat` 的 `text/event-stream` 逐 token 返回；
7. **context 扩展点预留**：确认当前 `buildUserPrompt(message, null)` 传的是 `null`，为第五课注入记忆留好基准。

## Key Emphasis
阅读顺序是"从入口到链路"，不是按文件名字母序。每个验证点都要在 `v1.1.1` 控制台或浏览器 Network 面板亲眼确认。

## Optional Extension
**学员问：读代码总是读不进去，读着读着就迷路了，有什么方法？**
答：迷路的原因是按文件读，不按调用链读。
Demo 做法：照这页的 6 步顺序，每步只回答一个问题（这行代码解决什么问题），从 `ShoppingAgentController` 一路跟到 `callLLM()`，再对照后端控制台打印的五步输出把代码行对上。
生产做法：读生产代码要多加一条——找治理点：超时配在哪、重试配在哪、脱敏在哪一步、Key 从哪来。本课的 Demo 里这些点几乎不存在，正好用 page_16 的七项清单当"找什么"的提纲。

## Classroom Interaction
让学员现在就做验证点 1 和验证点 4：点「测试注入防御」看拦截，再点「分析对比」看气泡头部的 `[MODEL]` 标签切换为推理模型。

## Take Away
读 Demo 按调用链走——入口 → 路由 → 构建 → 校验/纠正 → 防护，每一步都能在控制台和前端看到对应证据。

## Why This Slide Matters
跳过这页，学员课后打不开代码：只知道"有这些文件"，不知道从哪个方法开始读、读到哪算懂。下节课起每课都会在 Demo 上加新模块，没有阅读路线的学员会从第四课开始越落越远，最后只能看讲师演示、自己动不了手。

## Transition
路线给完了，验证点也给了。但学员跑完 Demo 一定会问：这个 Demo 拿去生产能用吗？接下来一页把差距一次讲清——七项生产工程件，每项讲到"怎么解决"。
