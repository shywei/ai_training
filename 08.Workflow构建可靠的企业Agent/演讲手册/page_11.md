## Slide Objective
展示 THINK→ACT→OBSERVE 循环的实际执行过程和代码实现，以及为什么用手搓而不用原生 Function Calling。

## Suggested Presentation
左边是一个完整的执行例子。用户说"帮我对比拯救者和 MacBook 的价格"。

THINK 1："需要查拯救者价格" → ACT 1：调用 search_knowledge("拯救者笔记本") → OBSERVE 1：拯救者 Y9000P ¥8999, i7/16GB/512GB。

THINK 2："还需要查 MacBook" → ACT 2：调用 search_knowledge("MacBook Pro") → OBSERVE 2：MacBook Pro 14 ¥14999, M3/18GB/512GB。

THINK 3："信息足够，直接回答" → 最终回复："拯救者 ¥8999 更便宜..."。

注意——LLM 在 THINK 3 中决定"不需要再调工具了，直接回答"。这就是 ReAct 循环的退出条件：LLM 输出中没有 TOOL_CALL 标记，就退出循环。不是 Runtime 决定"该结束了"，而是 LLM 说"我做完了"。

LLM 如何表达"我要调工具"？Prompt 约定输出格式：TOOL_CALL:工具名(参数)。AgentRuntime 用正则提取，路由到对应 MCP Client。为什么用文本协议？因为手搓 ReAct 用文本协议是白盒——THINK/ACT/OBSERVE 全部可见，学员能理解循环的每一步。第七课用的是 Spring AI 的 Function Calling——黑盒，框架自动处理 tool_call，你看不到 LLM 的思考过程。

这不是技术倒退。生产环境可以直接替换为原生 Function Calling——原理是一样的，都是"LLM 说要调什么，系统执行"。区别在于可见性：手搓是教学工具，Function Calling 是生产力工具。

右边是 AgentRuntime.java 的核心代码。while 循环控制最大轮次。THINK 阶段调用 LLM。extractToolCall() 提取工具调用标记。如果有 TOOL_CALL → ACT + OBSERVE。如果没有 → 直接回答，退出循环。

## Key Emphasis
LLM 如何表达"我要调工具"是这页的核心。TOOL_CALL:工具名(参数) 这个约定要讲清楚——它是 THINK 和 ACT 之间的"协议"。

为什么不用原生 Function Calling 要解释清楚——不是技术问题，是教学选择。白盒让学员看到循环的每一步。

## Optional Extension
**学员问：extractToolCall() 的正则靠谱吗？**
答：不完美。LLM 输出可能有变体——多换行、多空格、中文标点。Demo 的正则做了多种格式兼容，但不如原生 Function Calling 可靠。生产环境应该用 OpenAI 的 function_call 协议（结构化输出），不靠 Prompt 约定。

**学员问：THINK 阶段的输出会展示给用户吗？**
答：不会。THINK 是 Agent 的内部思考，只记录在 Trace 里。用户只看到最终回复。但讲师可以通过 Trace 查看 THINK 内容——这就是"可观测性"的价值。

## Take Away
THINK 决定做什么，ACT 执行操作，OBSERVE 获取结果。LLM 说"我做完了"就退出循环。

## Why This Slide Matters
如果跳过这页，学员不知道 Agent 如何"多步执行"。THINK→ACT→OBSERVE 是所有 Agent 框架的底层模式——理解了它，就理解了 LangChain、LangGraph 的核心。

## Transition
Runtime 的六个组件中，还有一个重要的能力——模型路由。