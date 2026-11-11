## Slide Objective
对比三种路由方案,解释选型理由和每种方案的适用场景。

## Suggested Presentation
三种路由方案,从简单到复杂。

**方案一:关键词过滤**。匹配"多少钱"→ TOOL,匹配"推荐"→ KNOWLEDGE,默认 → CHAT。第六课用过。局限很大:"帮我看看那个笔记本"不含"推荐"但需要检索;"这个笔记本怎么样"不含"推荐"但也是知识查询。关键词无法理解语义。

**方案二:Embedding 相似度**。预定义每个意图的描述(如 KNOWLEDGE 的描述是"用户想了解商品信息、寻求推荐"),用户消息 Embedding 后和意图描述做相似度匹配。效果比关键词好,但需要维护意图描述--新增一个意图就要更新描述。

**方案三：LLM 分类**。用快模型一次调用做两件事：① 意图分类（KNOWLEDGE / TOOL / CHAT）② 模型选择（KNOWLEDGE/TOOL 用推理模型，CHAT 用对话模型）。返回 `RouteResult(route, model)`，一次调用同时拿到路由和模型。语义理解，准确率高，不需维护规则。Demo 采用。

选型建议:意图少(< 10)用 LLM 分类,成本可忽略(每次约 100 token);意图多(> 50)考虑 Embedding,避免 LLM 调用成本;规则明确(如"退款"一定是 TOOL)用关键词,最快最准。实际项目中三种方案可以**混合使用**:先关键词快速匹配确定性强的意图,匹配不上再用 LLM。

Demo 用 LLM 分类还有一个好处:路由判断的结果可以直接在日志中看到(`logs/llm-route.log` 的 ROUTE RESPONSE 里 `[Route] KNOWLEDGE` / `[Selected Model] ...`),方便演示和调试。关键词过滤的匹配过程不透明,学员看不到"为什么匹配上了"。

**设计弯路（架构级）：意图路由的真实演进——不是"LLM 退化回规则"**。这段弯路流传最广的转述是"意图路由从 LLM 判定改成了规则判定"，把真实版本捋清后会发现方向说反了，而且改回关键词的是另一个判断。分四步讲。

**当初怎么想**：第六课用关键词过滤决定要不要检索（方案一），沿用到第七课立刻撞墙——"帮我看看那个笔记本"不含"推荐"但需要检索，关键词不理解语义；而 v4.0 架构里 KNOWLEDGE / TOOL / CHAT 三条路径的 context 构建和 Tool 注册方式不同（KNOWLEDGE 才注入 RAG、TOOL 才注册 product tools），**必须先分类才能走分支**。于是把意图判定升级为 LLM 判定：`LlmRouter.route()` 用快模型一次调用同时返回意图和推荐模型（`RouteResult(route, model)`），`withTemperature(0.0)` 保确定性。注意演进方向是**规则 → LLM**，本来就不是 LLM → 规则。

**为什么走不通**：LLM 判意图本身没失败（准确率远高于关键词），走不通的是把"独立意图路由"当成永久架构——它存在的前提是"三条代码路径必须先分叉"。第八课全量 Tool 注册 + ReAct 循环（`AgentRuntime.execute()` 的 while 循环）让 LLM 在 THINK 里自主决定调什么工具，"走哪条路"的语义判断被循环吸收，前提就消失了：独立路由从"必需"退化成纯开销——每次多一跳延迟、约 100 token、还多一个错误源（路由判错，整条路径跟着错）。真正撞墙的是另一种判断：把"要不要规划、要不要确认"这类**控制流判断**也交给 LLM——非确定性（同输入不同判，课堂演示没法复现）、成本延迟（简单任务占大头却每次先付一次分类调用）、判错的代价形态完全不同。

**后来演进到什么（第八课预告）**：在本课 `v1.4`（对应架构演进 v4.0）中，`ShoppingAgentController.chat()` 依靠 `LlmRouter.route()` 划分 `KNOWLEDGE / TOOL / CHAT` 三条路径。到了下一课（第八课 `v1.5` / v5.0），引入多步 ReAct 循环（`AgentRuntime.execute()`）后，需要支持跨路由的复合任务（先检索知识库再查实时价格），因此独立意图路由将被 ReAct 循环吸收，而把"要不要多步规划"和"要不要人工确认"两个**控制流判断**独立为确定性规则（`LlmRouter.route()` 届时保留用于推荐接口的模型选择）。代码级落点在下一页展开。

**为什么现在更对**：沉淀出一条设计原则——**控制流决策用确定性规则，业务语义决策交给 LLM**。控制流判断（要不要规划、要不要确认）要的是同样输入同样行为（课堂可复现）、零成本零延迟、错了代价小且可兜底；业务语义判断（调什么工具、怎么拆步骤、怎么回答）才需要语义理解，交给 LLM。生产做法是混合路由：规则做快路径，没命中用快模型做一次语义分类兜底。

### 🚀 2026 企业大规模工具治理：Tool RAG（Dynamic Tool Discovery 动态工具检索）

当企业引入 MCP Gateway 后，往往汇聚了来自 ERP、CRM、OA、工单系统的 **50~200+ 个 Tool**。如果把所有 Tool 的 JSON Schema 全部塞进每次请求的 `tools` 数组：
1. **Token 爆炸**：每个 Tool Schema 约 200~500 tokens，100 个工具每轮对话固定消耗 **20,000+ tokens**，成本与首字延迟（TTFT）剧增。
2. **注意力稀释与选错工具**：工具过多（尤其存在多个命名相近的查询接口）会导致 LLM 注意力分散，选错 Tool 或传错参数的概率大幅上升。

**生产级解决方案（两级动态挂载）**：
- **第一级 Domain Routing（领域路由）**：本课 `v1.4` Demo 的 `LlmRouter` 就是最简领域路由——判定为 `TOOL` 才挂载 `mcpProductClient` 的商品工具集，其余意图零工具挂载。
- **第二级 Tool RAG（工具向量检索 / Dynamic Tool Discovery）**：将所有 MCP Tool 的 `name + description + 参数摘要` 向量化存入向量库；用户提问时，先拿用户 Query 做 Embedding Search 召回最相关的 **Top-K（如 3~5 个）** Tool，当轮请求只把这 Top-K 个 `FunctionCallback` 动态挂载进 `tools` 参数。既省下 90% 的 Schema Token，又显著提升模型工具调用准确率。

## Key Emphasis
LLM 路由的成本可以忽略--每次约 100 token,用最便宜的模型(Qwen2.5-7B)。准确率远高于关键词。不要因为"多一次 LLM 调用"就不用 LLM 路由。

**但要明确告诉学员:路由不是必须的,但也不是只有 tools 很多才需要。**

标准做法是**所有 tools 直接注册给 LLM,LLM 自己决定调哪个**。OpenAI Function Calling、LangChain、CrewAI 都是这样做的。LLM 天生有"根据用户问题选择正确工具"的能力,不需要额外路由器。

什么时候需要路由器:
- **安全隔离**:某些用户/角色不能访问某些 tools(如普通用户不能调删除接口,实习生不能查薪资数据)。这不是 tools 多不多的问题,是权限问题。
- **成本控制**:tools 多时全部注册给 LLM,每个 tool definition 约 200-500 token。10 个 tools 就是 2000-5000 token,每次对话都带着。路由器用便宜模型预筛,只注册相关的 tools,可以省 50%+ token。即使 tools 不多,高频调用下成本也很可观。
- **context 按需构建**:不同意图需要注入不同的上下文(如 Demo 中 KNOWLEDGE 才注入 RAG,TOOL 才注册 FunctionCallback)。这不是工具选择问题,是上下文管理问题。

Demo 用路由器同时展示了这三个价值:安全(不同路由注册不同 tools)、成本(只在需要时注册)、context 按需构建。学员做项目时,根据实际场景决定是否需要路由器。

## Optional Extension
**学员问：我们 tools 不多，还要不要路由器？直接全注册给 LLM 自己选不行吗？**
答：行，而且这是标准做法——但三种情况必须加路由器，判据不是工具数量。
Demo 做法：`LlmRouter.route()` 用快模型一次调用做三分类（`temperature=0` 保证同消息同路由），结果顺带选模型（KNOWLEDGE/TOOL→reasoning、CHAT→chat）；路由决定注入 RAG、注册 product tools。
生产做法：三种必须路由的场景——安全隔离（角色不能见的 Tool 不要出现在 function definitions 里，注册都不注册）、成本控制（每个 tool definition 200-500 token，高频调用下预筛省一半以上）、context 按需构建。权限的最终落点是网关 RBAC（第十一课支柱一），路由只是第一道筛。取舍：路由是多一跳延迟加一个新错误源，三条红线一条都不占就别加。

**学员问：这和第十课的多 Agent Router 是一回事吗？**
答：名字同源，层次不同。这里的 Router 是单 Agent 内部的意图分流（决定注入什么、注册什么）；第十课的 Router 是多 Agent 协作模式（控制权分流给不同的 Agent），那是编排层的问题。

## Take Away
加不加路由器看三条红线：有没有权限隔离需求、有没有 token 成本压力、要不要按意图裁剪 context——一条都不占就把 tools 全注册给 LLM 自己选。

## Why This Slide Matters
跳过这页，学员会在不需要时硬加路由器（白增延迟和故障点），或在需要权限隔离时把全部 tools 注册给 LLM——普通用户的 function definitions 里出现删除接口，防线等于没有。

## Transition
对比了三种路由方案并了解了大规模工具的 Tool RAG 检索思路，接下来看本课 `v1.4` Demo 中 `LlmRouter.java` 的具体代码实现，以及路由如何影响 context 构建（page_19）。
