## Slide Objective
澄清Agent内部各个组件的边界，解决"拿着锤子找钉子"的问题。每个辨析都要给出"为什么容易混"和"怎么区分"。

## Suggested Presentation
在实际开发中，最容易让人晕头转向的就是组件的混用。我来讲五个最常见的混淆。

第一个：Memory和Knowledge到底有什么区别？
为什么容易混？因为两者都是给大模型"补充信息"的。但它们补充的信息类型完全不同。
Memory记"人"——这个用户的偏好是什么、上次聊了什么、买过什么东西。它存的是和特定用户相关的历史信息。
Knowledge记"事"——公司的产品手册、退货政策、技术文档。它存的是和业务相关的通用知识。
举个例子：用户说"我上次买的那个红色的还在吗？"——Agent需要Memory知道他上次买了什么（红色的），需要Knowledge知道库存情况（查文档或API）。两者缺一不可，但作用完全不同。

第二个：RAG一定需要向量数据库吗？
不一定。为什么大家觉得一定需要？因为几乎所有RAG教程都在讲向量数据库。但向量数据库解决的是"语义相似"的问题——用户问"笔记本电脑卡顿怎么办"，能匹配到"电脑运行缓慢的解决方案"。如果你的场景是精确匹配——比如用户输入产品编号查规格——关键词搜索就够了，还更快更便宜。向量数据库是好东西，但不是每个场景都需要。

第三个：Tool、MCP、Workflow分别解决什么问题？
这三者是不同层次的东西，不是同一层次的不同选择。
Tool是单个动作——调一个API、查一次数据库。
MCP是Tool的协议标准——统一了Tool的调用格式，让不同框架能复用同一个Tool。
Workflow是多个动作的编排——把几个Tool按顺序和条件串起来。
类比：Tool是一拳，MCP是出拳的标准姿势，Workflow是一套连招。

第四个：Workflow是不是等于Prompt写复杂一点？
完全不是。Prompt是单次LLM调用的指令——你告诉模型"做什么"。Workflow是多次调用的编排——它有状态（记住上一步的结果）、有条件（根据结果决定下一步走哪条路）、有循环（失败了重试）、有人工审批（关键节点让人确认）。Prompt是一句话，Workflow是一套流程。

第五个：Multi-Agent和Workflow有什么本质区别？
Workflow管"怎么做"——步骤一做什么、步骤二做什么，是确定性的流程编排。
Multi-Agent管"谁来做"——每个Agent有独立的大脑（LLM）、独立的Prompt、独立的工具。
关键区别：Workflow的节点是"执行者"（做固定的事），Multi-Agent的节点是"决策者"（能自己判断该做什么）。简单任务用单Agent+Workflow，复杂任务需要多个"决策者"协作时才用Multi-Agent。

## Key Emphasis
强化口诀："Memory记人，Knowledge记事"。
理清层次："Tool是单个动作，MCP是标准，Workflow是编排，Multi-Agent是角色分工"。
每个辨析都要给出具体例子，不要只讲抽象概念。

## Optional Extension
如果有程序员问RAG和Tool能不能混用，明确回答：不建议。Tool拿结构化动态数据（JSON），RAG拿非结构化静态文本（段落）。两者的数据格式和使用场景完全不同。

## Classroom Interaction
无。

## Take Away
组件各司其职：Memory记人、Knowledge记事、Tool做单步、MCP定标准、Workflow排步骤、Multi-Agent排角色。不要拿着锤子找钉子。

## Why This Slide Matters
解决"拿着锤子找钉子"的问题。如果组件边界不清，学员在架构设计时会用错工具——把该用Tool的场景用了RAG，把该用Workflow的场景写了巨无霸Prompt。

## Transition
理清了组件边界，我们再来看更大的层面——Agent和我们公司现有的OA系统、ERP系统，到底是什么关系？谁取代谁？
