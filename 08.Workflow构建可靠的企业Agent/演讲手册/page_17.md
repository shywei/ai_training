## Slide Objective
展示 context 演进——从第四课到第八课，context 参数如何逐步增长，以及第八课的变化。

## Suggested Presentation
左边展示 context 的演进过程。第四课：context = null——纯 Prompt，什么都不注入。第五课：context = 对话历史 + 用户画像——Agent 记住了用户。第六课：+ RAG 检索结果——Agent 能查商品知识了。第七课：+ Tool 调用结果——Agent 能查实时数据了。第八课：+ Agent Runtime 编排——context 在多轮循环中逐步积累。

注意第八课没有新增 context 内容——画像、记忆、对话历史、RAG、Tool 结果，这些第七课都有了。第八课改变的是 context 的使用方式：从一次性使用变成多轮循环使用。

什么意思？第七课是"一次调用"——构建 context → 调 LLM → 返回。第八课是"多轮循环"——第 1 轮 THINK 时 context 只有画像+记忆+RAG，调了 Tool 之后，第 2 轮 THINK 时 context 多了第 1 轮的工具结果。第 3 轮又多了第 2 轮的结果。context 在循环中逐步积累。

右边展示 v5.0 context 的完整形态。在 AgentRuntime.execute() 中构建：① 用户画像（MCP Resource）② 用户记忆（MCP Tool 语义检索）③ 对话历史（Redis）④ RAG 检索（MCP Tool）⑤ 多轮工具结果（ReAct 循环积累）。

前四个是一次性构建的，第五个是循环中逐步积累的。这就是第八课和第七课的本质区别——context 从"静态"变成"动态"。

## Key Emphasis
context 演进是贯穿整个课程的主线。每课新增一层 context，Agent 的能力就增强一层。第八课的变化是"使用方式"，不是"内容"。

多轮循环中的 context 积累是核心概念——每轮 THINK 都能看到之前所有工具调用的结果。

## Optional Extension
**学员问：context 会不会越来越大？**
答：会。多轮循环中，conversationHistory 会不断增长。生产环境需要 token 预算管理——限制 context 总长度，超长时截断最早的内容。第六课讲过 token 预算分配（画像 10% + 历史 30% + RAG 40% + 留余量 20%）。

**学员问：能不能把 context 缓存起来复用？**
答：可以。画像和记忆可以缓存——同一个用户的前几轮对话不需要每次都查。RAG 结果可以缓存——同样的 query 不需要每次都检索。这是 Demo vs 生产的差距之一。

## Take Away
第八课没有新增 context 内容，而是改变了使用方式——从一次性变成多轮循环逐步积累。

## Why This Slide Matters
如果跳过这页，学员不知道第八课和第七课在 context 层面的区别。context 演进是课程的主线——每课的变化都体现在 context 上。

## Transition
接下来看 Demo 的知识体系覆盖情况——哪些 Demo 做了，哪些只在 PPT 里讲。