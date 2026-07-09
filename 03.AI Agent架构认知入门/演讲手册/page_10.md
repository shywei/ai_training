## Slide Objective
解释Workflow的存在意义，让学员了解当前行业主流的Workflow编排方案和它们的差异。

## Suggested Presentation
企业的业务是非常复杂的。
比如一个"退货申请"：第一步查订单是否存在，第二步查商品是否符合退货政策，第三步高价值商品要转人工审批，最后才能发起退款。
这种多步骤、有条件分支、需要人工介入的任务，大模型一次思考是搞不定的。我们需要把这些步骤像流水线一样编排起来——这就是Workflow。

Workflow和单次Prompt调用有什么区别？Prompt定义的是"这一步做什么"，Workflow定义的是"这几步怎么串"。
Prompt是一个动作，Workflow是一套连招。

Workflow的关键能力有三个：条件路由（根据上一步的结果决定走哪条分支）、循环重试（某一步失败了自动重试）、人工审批（Human Checkpoint，关键节点让人确认后再继续）。

那行业里有哪些主流的Workflow方案？
LangGraph是目前最灵活的Workflow框架，它把流程建模成一张"图"——节点是Agent或工具调用，边是条件路由。灵活但学习曲线陡，适合需要高度定制的场景。
Dify是国内用得最多的低代码AI平台，它提供了可视化的画布拖拽编排Workflow，上手非常快，适合快速搭建原型和不太复杂的业务流程。
n8n是一个开源的自动化平台，它不是专门给AI用的，但可以和LLM结合，适合需要连接大量第三方服务的场景。
Flowise和Dify类似，也是可视化编排，更侧重于RAG和对话流的编排。
Spring AI也在逐步集成Workflow能力，Java开发者可以关注。

选型建议：如果你的团队是Java技术栈、且需要深度定制，选LangGraph或Spring AI。如果需要快速验证想法，选Dify。如果主要是连接外部服务做自动化，看n8n。

## Key Emphasis
重点展开Workflow的三个关键能力：条件路由、循环重试、人工审批。
逐个介绍主流方案（LangGraph/Dify/n8n/Flowise/Spring AI）的定位和适用场景。
给出选型建议，不要只列名字。

## Optional Extension
如果学员问Workflow和传统的BPM（业务流程管理）有什么区别，回答：传统BPM的节点是确定性的代码逻辑，Workflow的节点可以是LLM——判断逻辑是语义级的，不是简单的数值比较。这就是为什么AI Workflow比传统BPM更灵活，但也更不可控。

## Classroom Interaction
无。

## Take Away
Workflow把复杂任务拆成可控的执行流水线。LangGraph最灵活，Dify最易用，按需选型。

## Why This Slide Matters
很多开发者容易把Agent做成一个巨无霸Prompt。Workflow教他们用工程化思维拆解流程，是Agent从"玩具"变成"工具"的关键一步。

## Transition
如果一个任务复杂到每个步骤都需要专门的Agent来负责——一个写代码、一个做测试、一个做审查——单Agent搞不定了。这时候就需要把多个Agent组合成一个团队，这就是Multi-Agent。
