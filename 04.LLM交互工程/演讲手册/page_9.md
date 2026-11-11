## Slide Objective
讲清楚结构化输出的真正目的——不是"展示 JSON"，而是"让 Java 代码能程序化处理 LLM 的输出"。同时辨析三方案（Native Structured Output / Function Calling / JSON mode + Parser）的边界与分层选型。

## Suggested Presentation
这一页的核心问题：LLM 的输出是给人看的，还是给代码用的？

聊天场景下，LLM 返回自然语言够了——用户自己能读。但企业系统不一样。用户说"帮我推荐笔记本"，LLM 返回"推荐联想小新Pro，大概 6000 到 8000"——这段话人能看懂，但 Java 代码怎么提取商品名？价格区间？用正则？格式一变就崩了。

如果 LLM 返回 JSON 呢？`{"products":[{"name":"联想小新Pro","price":"6000-8000"}]}`——Jackson 直接映射为 Java 对象，`result.getProducts().get(0).getName()` 就拿到了商品名。可靠、确定、可编程。

坦诚说：第四课的 Demo 还不能完整体现结构化输出的价值——因为当前没有下游系统。等到第七课 Tool/MCP，LLM 返回 JSON 告诉 Java"要调哪个工具、传什么参数"，Java 解析后调用对应 API；等到第八课 Workflow，LLM 返回 JSON 决策结果，状态机根据字段决定下一步走哪里。那时候你会真正理解为什么需要结构化数据。现在是打基础。

怎么让 LLM 返回 JSON？先看两种最常用的手法，再放到三方案全景里辨析。

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

**三方案辨析（行业共识：互补，不是竞争）。** 上面两种是"让输出变成 JSON"的手法；还有一条路线是把结构化需求藏进工具调用——**Function Calling 的 tool schema 本身就是一种结构化输出**：模型返回 tool_calls（函数名 + 合规参数 JSON），第七课的工具链路全靠它。三者分工：**① Native Structured Output**（response_format / json_schema）——token 级约束、合规率接近 100%，要"100% 可解析"的数据接口首选，代价是部分模型不支持、复杂 Schema 生成变慢；**② Function Calling**（tool schema）——当结构化输出的语义是"调这个工具、参数是这些"时用它，一次往返同时拿到动作和参数，别用 Native 输出一个假的 tool_calls 再自己解析——那是在重造 Function Calling；**③ JSON mode + Parser**（兜底 + repair）——JSON mode 只保证输出是合法 JSON、不保证字段，必须配 extractJson / validate / repair（Self-Correction），所有模型都能跑，是兜底层。选型一句话：要稳数据 → ①，要调工具 → ②，模型不支持或格式不可控 → ③。三者是叠加关系，不是三选一——生产上 ①管数据、②管工具、③永远在最底下接住意外。

看右边的代码。StructuredOutputHandler 做三件事：extractJson 从废话中提取 JSON，validate 做 Schema 验证，buildCorrectionPrompt 在失败时构建重试提示词。

**Self-Correction 自我纠正（Demo 已实现，容错的最后一环）。** validate 拿不到合法 JSON 不代表链路就失败——Demo 里有一条自我纠正路径。看 `ShoppingAgentController` 的 `/api/agent/recommend`：`extractJson()` + `validate()` 都失败后，调 `StructuredOutputHandler.buildCorrectionPrompt(originalOutput, errorMessage)`，把「上次的原始输出 + 解析报错」拼成纠正提示词，再调一次模型，回来再 `validate()` 一次；二次仍失败才返回结构化的 error 字段（带截断后的 rawOutput），不把脏数据交给下游。注意纠正提示词为什么要把上次输出和报错都带上——模型要知道「错在哪」才能改，只说「请重新输出 JSON」，它大概率重复同样的错误。这条链路是三级递进的容错：提取兜底（纯代码，不指望模型）→ 自我纠正（把错误信息反馈给模型再试一次）→ 显式失败（结构化错误码，可被程序处理）。生产上还要补三件：纠正次数设上限（Demo 只纠正 1 次，防成本失控）；纠正率打监控（纠正率升高说明 Prompt 或模型变了，配合成本核算能第一时间发现）；纠正也失败时降级到模板化输出（返回一个空结果加错误标记，好过返回半截 JSON）。高级的 Self-Correction（多轮反思、Critique 模型）在 Demo 之外，属于本课「仅 PPT 讲解」的部分。

Demo 里可以验证：点前端的"结构化输出演示"按钮，看后端控制台日志。会打印 LLM 原始输出、提取出的 JSON、Bean Mapping 后的 Java 对象字段。三个步骤一目了然。

有个踩坑经验：extractJson 方法最初只处理了两种情况——纯 JSON 和代码块包裹。后来发现模型有时会在 JSON 前面加一段中文说明，后面再加一句"希望对你有帮助"。所以加了第三种处理：找第一个 { 到最后一个 }。这三种情况覆盖了大部分 LLM 输出格式。

## Key Emphasis
核心是"给代码用"，不是"给用户看"。两种方案不是二选一，而是配合使用。代码层兜底始终保留——这是工程思维，不是偷懒。三方案（Native / Function Calling / JSON mode+Parser）互补非竞争，按"要数据还是要动作"分层选型。

## Optional Extension
**学员问：有了 response_format 这种强制 Schema 的能力，还需要 extractJson 这种代码兜底吗？**
答：需要，两者是配合关系，不是替代关系。
Demo 做法：`StructuredOutputHandler.validate()` 压根不指望模型守规矩——先 `extractJson()` 四级提取（纯 JSON → json 代码块 → 无标记代码块 → 首个 { 到最后一个 }），Jackson `readTree` 解析失败再 `cleanJson()`（去尾逗号、去注释）重试，全程纯代码，模型输出什么格式都能捞出 JSON。
生产做法：首选方案二（response_format 做 token 级约束，合规率接近 100%），不支持的模型自动降级到方案一（Prompt 约束）+ 代码兜底；兜底仍失败走 Self-Correction 重试，再失败返回结构化错误码——存数据库、传订单系统之前的最后一道闸必须是 validate。取舍：response_format 的 Schema 太复杂时生成变慢甚至报错，关键接口要压测；代码兜底永远保留，因为它同时防御「模型行为漂移」和「换了不支持 Schema 的模型」两种情况。

**学员问：Native Structured Output 和 Function Calling 都能拿到带 Schema 的 JSON，到底选哪个？**
答：看你要的是「数据」还是「动作」。
Demo 做法：Demo 只要数据——`StructuredOutputHandler.extractJson()/validate()` 解析推荐结果、不触发任何工具，走「约束 + 兜底」路线就够；tool_calls 在 Demo 里还没出现（第七课 `MCP工具回调组件` 把 MCP 工具注册进 tools 字段后才会出现在响应里），现在先把两者边界记住。
生产做法：输出是给下游用的数据（存库、传订单）→ Native Structured Output（json_schema，稳定解析）；输出的语义是「我要调这个工具、参数是这些」→ Function Calling（tool schema），参数校验与工具治理（幂等/超时/审计，第七课展开）跟着 tool 链路走；两者都不支持的模型 → JSON mode + Parser + repair。取舍：Native 的 Schema 复杂时生成变慢甚至报错，关键接口要压测；Parser 兜底永远保留，它同时防御「模型行为漂移」和「换了个不支持 Schema 的模型」。

## Classroom Interaction
让学员写三行测试：给 `extractJson()` 各喂①纯 JSON、②带 json 标记的代码块包裹、③前后带一段废话的文本，三种都能提取出同一段 JSON——亲手验证「四级提取」不是过度设计，是被真实输出格式逼出来的。

## Take Away
结构化输出的可靠性 = 模型约束 × 代码兜底 × 自我纠正，三层少一层，脏数据迟早流进数据库。

## Why This Slide Matters
跳过这页，学员会直接拿 LLM 原文做 Bean Mapping——输出格式一变，解析崩、脏 JSON 写坏数据库和订单；更糟的是他们不知道 validate 失败后还有一条「自我纠正」路可走，遇到解析失败就当模型不可用直接放弃。

## Transition
结构化输出解决了"怎么拿到干净数据"。但还有一个更基础的问题：怎么让大模型按你的意图行事？这就是 Prompt 工程。
