## Slide Objective

将当前繁杂的AI工具和框架映射到七层架构模型中，帮学员理清生态脉络，指出学习重点。

## Suggested Presentation

现在市面上每天都有新的AI产品冒出来，比如今天出了个Dify，明天出了个LangChain，后天又火了个Cursor。大家是不是觉得学不过来？
其实不用焦虑。我们把这些东西扔到一个“七层架构”里，就一目了然了。

大家看这七层。
最上面是应用层，就是给普通用户用的，比如Cursor、Claude Code。
往下走是Agent Runtime和开发框架，比如LangGraph、Spring AI，这是我们开发人员用来写代码、编排业务流程的地方。
再往下有协议层的MCP，有模型层的GPT、Claude，最底下是算力和数据库（GPU、Vector DB）。

我们今天在座的是什么角色？是开发者。所以大家看屏幕最下方：
最底层的模型训练、GPU运维，那是算法工程师和运维人员干的事，大家“知道就行”。
而我们真正要学的，是中间这一层：怎么用Spring AI写代码？怎么用MCP接系统？怎么做RAG？怎么编排Workflow？这才是我们开发者的主战场。

## Key Emphasis

重点是要帮学员“减负”：明确告诉他们哪些需要深入学，哪些只需要了解。
讲清楚LangChain/Spring AI处于框架层，而Dify/Cursor处于应用层，避免概念混淆。

## Optional Extension

如果被问及Spring AI和LangChain选哪个，可以说明：Java技术栈选Spring AI，Python/TS技术栈选LangChain。

## Classroom Interaction

提问：“大家平时听得最多的一个词，比如GPT-4，在这个架构里的哪一层？”（引导回答模型层）。

## Take Away

开发者不需要学习造大模型，而是要学习如何利用框架（Spring AI/LangGraph）来组装大模型。

## Why This Slide Matters

消除学员面对庞杂AI生态时的焦虑感，为后续的自主学习指明方向。

## Transition

了解了生态，以后碰到新名词就能对号入座了。那如果现在老板丢给你一个需求，你该怎么决定用哪些技术呢？我们来做最后一步：认知收敛，建立你的决策树。
