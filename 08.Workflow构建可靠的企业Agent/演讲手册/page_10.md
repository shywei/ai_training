## Slide Objective
讲解 TaskPlanner——LLM 如何把复杂任务拆成步骤，以及 Agent 和 Chatbot 的本质区别。

## Suggested Presentation
左边展示 TaskPlanner 的实际例子。用户说"帮我对比拯救者和 MacBook 的价格，预算 8000"。TaskPlanner 输出 3 个步骤：Step 1 搜索拯救者价格，Step 2 搜索 MacBook 价格，Step 3 对比分析推荐。

什么时候需要规划？包含"对比""比较""帮我选""推荐几个""然后""同时"等关键词，或者包含多个商品名（≥2）。简单问候、单次查询不规划，直接走 ReAct 循环。

为什么要判断？因为规划本身也是 LLM 调用——有成本和延迟。简单任务不需要规划，直接回答更快。只有多步骤任务才值得花一次 LLM 调用来拆步骤。

右边是核心认知：Agent 和 Chatbot 的本质区别。Chatbot 是用户问一句，LLM 答一句。Agent 是用户说一个目标，LLM 规划步骤，逐步执行。TaskPlanner 是 Agent 的"大脑前额叶"——不是回答问题，而是规划怎么解决问题。

代码展示了 TaskPlanner 的核心逻辑：用 reasoning 模型调用 LLM，输出 JSON 数组，解析为 WorkflowStep 列表。如果规划失败，返回空列表，AgentRuntime 降级为单步执行。

这个降级逻辑很重要——规划失败不能让整个任务失败。我在测试中发现，LLM 有时会输出非 JSON 格式（比如加了代码块标记），导致解析失败。降级为单步执行虽然不完美，但至少能回答用户的问题。

## Key Emphasis
TaskPlanner 是 Agent 和 Chatbot 的分界线。有了 TaskPlanner，Agent 就不只是"回答问题"，而是"解决问题"。

规划失败的降级逻辑要讲清楚——这是"可靠"的体现。

## Optional Extension
**学员问：TaskPlanner 的规划质量怎么保证？**
答：Demo 中用关键词判断是否规划，规划质量靠 Prompt 控制。生产环境应该加规划质量评估——比如检查步骤数是否合理、步骤之间是否有依赖关系。规划结果也可以缓存——同样的任务不需要每次都重新规划。

**学员问：能不能用 LangChain 的 Plan-and-Execute 模式？**
答：那就是 TaskPlanner 的框架版。LangChain 的 Plan-and-Execute 把"规划"和"执行"分开——先用 LLM 生成计划，再逐步执行。原理一样，只是框架帮你封装了。第九课会对比。

## Take Away
Chatbot 回答问题，Agent 规划怎么解决问题。TaskPlanner 是 Agent 的"大脑前额叶"。

## Why This Slide Matters
如果跳过这页，学员不知道 Agent 如何处理复杂任务。TaskPlanner 是"多步执行"的前提——没有规划，ReAct 循环就不知道该做什么。

## Transition
规划完成后，进入 ReAct 循环——THINK、ACT、OBSERVE。