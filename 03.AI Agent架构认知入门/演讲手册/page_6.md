## Slide Objective
明确Prompt在Agent系统中的定位，同时让学员了解当前行业主流的Prompt实现方案和工具链。

## Suggested Presentation
我们先来看第一个能力：Prompt。大家可能觉得，Prompt不就是写提示词吗？有什么好讲的？

在Agent系统里，Prompt绝不是聊天框里的那句话。它是整个系统的起点。没有Prompt，大模型就只会闲聊。加上了Prompt，大模型才变成特定角色的专家。

Prompt解决的是"做什么"的问题，后面的Memory、Tool解决的是"怎么做"的问题。方向错了，后面全白搭。

那在工程实践中，Prompt到底长什么样？
大家看左边——一个完整的Prompt系统包含四个部分：System Prompt定义角色和规则，Template是带变量的模板（比如把用户名、订单号动态填进去），Few-shot是给模型看的示例，Output Parser负责把模型的自由文本转成结构化数据（比如JSON）。

再看右边，目前行业里做Prompt管理的主流方案。
Spring AI提供了PromptTemplate，Java开发者可以直接在Spring项目里用模板变量生成Prompt。
LangChain也提供了PromptTemplate，Python生态用得更多。
这两个框架的思路是一样的：把Prompt当代码管理，支持版本控制、变量替换、模板复用。

在实际工程中，Prompt通常会像代码一样被放进Git仓库管理。为什么？因为Prompt改一个词，Agent的行为可能就完全不同。你必须能追溯每次修改，能回滚。

## Key Emphasis
讲清楚Prompt在Agent架构中作为"角色定义"和"行为约束"的底层作用。
重点讲工程系统中的Prompt结构（Template和Parser），不要去讲Prompt Engineering的奇技淫巧。
点出Spring AI和LangChain两个主流方案，让学员知道实际开发中用什么工具。

## Optional Extension
如果学员问Prompt和Fine-tuning的区别，回答：Prompt是在不改变模型的前提下，通过指令引导行为；Fine-tuning是用训练数据修改模型本身。Prompt成本低、见效快，是Agent系统的首选方案。

## Classroom Interaction
无。

## Take Away
Prompt是Agent的"人格定义"，在工程中通过Template和Parser实现结构化管理。

## Why This Slide Matters
如果不把Prompt作为系统的第一块基石，后续的组件就失去了挂载的主体。如果不讲工程实现方案，学员会以为Prompt就是手动打字。

## Transition
现在我们给Agent定义了角色，它知道自己是谁了。但是如果它过一会儿就把用户忘了怎么办？这就需要第二个能力：Memory。
