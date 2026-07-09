## Slide Objective

将复杂的Agent系统抽象为“认知”与“执行”两层，帮助学员在脑海中建立结构化的知识抽屉。

## Suggested Presentation

刚才我们看了一张很复杂的系统图，现在我们把它简化一下。
无论Agent多么复杂，我们其实都可以把它拆分成两大类能力：认知层和执行层。

什么叫认知层？就是让Agent“知道什么”。这部分包括：Prompt（它要知道自己是谁，有什么规矩）、Memory（它要记住你是谁，你聊过什么）、Knowledge（它要理解企业的内部文档和知识）。有了这三者，这个大脑才不是一片空白。

那什么叫执行层呢？就是让Agent去“做什么”。光想是不够的，它还得干活。怎么干活？通过Tool去调外部API，通过MCP去统一接口，或者通过Workflow去编排多步任务。如果任务太复杂，可能还需要多个Agent（Multi-Agent）一起协作来完成。

大家记住这个公式：认知（思考）+ 执行（行动）= Agent。大模型就是连接这两层的桥梁。而且，不是每个Agent都需要所有的能力，我们需要什么就装什么。

## Key Emphasis

必须清晰界定“认知层”和“执行层”的概念。
强调Agent是按需组合的，不要让学员产生“必须包含所有组件才是Agent”的误解。

## Optional Extension

如果有人觉得这就是简单的API堆砌，可以提醒他们：大模型在其中扮演的是“大脑”的作用，它负责在认知和执行之间做推理和决策。

## Classroom Interaction

无。

## Take Away

Agent能力分为认知层（Prompt+Memory+Knowledge）和执行层（Tool+MCP+Workflow+Multi-Agent）。

## Why This Slide Matters

这是承上启下的关键，把复杂的架构图化繁为简，降低了学员的认知负荷，让后续的学习有了清晰的分类。

## Transition

有了这个全景图，接下来我们就要逐一攻克这些能力。首当其冲的，就是最基础也是最核心的组件：Prompt。
