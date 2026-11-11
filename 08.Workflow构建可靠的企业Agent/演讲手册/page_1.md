## Slide Objective
开场定调：从"能做事"进入"可靠地做事"。建立第八课的认知框架——Agent 需要一个"操作系统"来管理自己的行为。

## Suggested Presentation
上节课让导购 Agent 连接了企业系统——能查价格、查库存、调用 MCP Tool。LLM 路由判断意图，Function Calling 执行工具调用，MCP 协议统一数据访问。Agent 从"只能说话"变成了"能做事"。

但"能做事"和"可靠地做事"是两回事。我在做 Demo 的时候发现了一个真实的问题：当我让 Agent 查两个商品的价格做对比时，它一次只能调一个 Tool。为什么？因为第七课的 ShoppingAgentController 是一问一答模式——用户说一句话，Spring AI 框架自动处理一次 tool_call 就结束了。要查两个商品，用户得问两次。

还有一个更严重的问题。我写了一个"帮我下单"的测试用例，Agent 直接调了下单接口。没有确认，没有拦截。在企业环境里这是灾难——万一 LLM 理解错了呢？用户说"帮我看看那个笔记本"，LLM 理解成"帮我买那个笔记本"呢？

Tool 调用超时了会怎样？直接报错。MCP Client 写死 15 秒超时，超时就抛异常，前端显示"抱歉，处理过程中出现了问题"。没有重试，没有降级。用户只能重新问一次。

最让我头疼的是调试。用户投诉"推荐了不合适的商品"，我打开日志一看——什么都没有。没有执行记录，没有推理过程。我完全不知道 LLM 为什么做了这个决定。这是我决定做第八课的直接原因。

这四个问题的根源是一样的：Agent 没有"运行时管理"。它只有 LLM 调用能力，没有执行控制能力。第八课要给 Agent 装一个"操作系统"——Workflow。

## Key Emphasis
四个问题要讲具体，不要抽象地说"不够好"。每个问题都要有真实场景：多步查询做不了、下单没确认、超时没重试、出错没记录。学员需要建立痛感。

不要在这一页就讲解决方案——这页只讲问题。解决方案从下一章开始。

## Optional Extension
**学员问：为什么不直接用 Spring AI 的 Function Calling 多调几次？**
答：Function Calling 一次调用就结束了。Spring AI 框架自动处理 tool_call，你没法控制"接下来再调另一个 Tool"。要多步执行，必须自己控制循环。这就是第八课 AgentRuntime 的意义。**Demo 做法**：AgentRuntime 手写 while 循环（最多 10 轮），用 TOOL_CALL 文本约定自己提取工具调用；**生产做法**：工具协议换原生 Function Calling 的结构化 tool_call，但循环控制权仍在 Runtime——第九课用 AgentScope Java 2.0 重构会并排对比框架接管到哪一层。

**学员问：这些问题在第七课的 Demo 里没暴露吗？**
答：暴露了，但第七课聚焦"怎么调 Tool"，没关注"怎么可靠地调"。Demo 是逐课演进的，第七课先让 Agent 能做事，第八课再让它可靠地做事。

## Take Away
判断 Agent 能不能上生产，看可靠性工程要素：多步执行（7状态机与ReAct循环）、高风险确认（人工接管HITL）、三级自动容错（Retry + Circuit Breaker + Fallback）与任务错误重规划、执行可追溯（Tracing）——缺一件就不叫企业可靠。

## Why This Slide Matters
跳过这页，学员会把 Workflow 排成"可选增强"后置——先上裸 Agent，多步做不了、误下单、超时崩、无法审计四个坑上线后逐个踩，再回头补的成本翻倍。

## Transition
先看今天这堂课的完整目录（P2）——八个章节如何把 Agent 从"能做事"升级到"可靠地做事"。