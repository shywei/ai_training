## Slide Objective
解释Tool与MCP的存在意义，让学员了解Function Calling机制和MCP协议的工程实现。

## Suggested Presentation
前面讲了，Agent通过RAG查文档。但如果用户要查今天北京的天气，或者查他在京东的快递到哪了，查文档有用吗？没用。这些是实时数据，存在外部系统里。

这时候就需要Tool——给Agent一双"手"，让它能调用外部API。

Tool的核心机制叫Function Calling。开发者先告诉大模型"你有这些工具可以用"，并描述每个工具的功能和参数。大模型根据用户的意图，自己决定该调哪个工具、传什么参数。调完之后，把结果拿回来继续推理。

这个过程对用户是透明的——用户只看到Agent"帮他查了快递"，不知道中间调了几次API。

但这里有一个工程痛点。你们公司可能有几十个微服务，每个都有API。难道要为每个API写一套对接代码？如果明天换了一个Agent框架，是不是全部重写？

为了解决这个问题，Anthropic在2024年推出了MCP——模型上下文协议。MCP的思路很像Type-C接口：你的后端系统实现一个MCP Server，暴露标准格式的工具描述；任何支持MCP的Agent框架都能直接调用。一次开发，处处可用。

目前行业里的实现方案：
Function Calling方面，OpenAI、Claude、DeepSeek都原生支持，Spring AI通过@Tool注解可以直接把Java方法暴露为可调用工具。
MCP Server方面，官方提供了Python和TypeScript的SDK，Spring AI也集成了MCP支持。
MCP Client方面，Claude Code、Cursor、Claude Desktop都内置了MCP客户端。

实际选型建议：如果工具少于3个，直接用Function Calling就够了，简单直接。工具多了、或者需要跨框架复用时，再引入MCP。

## Key Emphasis
讲清楚Tool和Knowledge的区别：Tool调API拿结构化动态数据，Knowledge查向量DB拿文本段落。
重点展开Function Calling机制和MCP协议，给出具体的技术方案（Spring AI @Tool、MCP SDK）。
给出选型建议：少于3个工具用Function Calling，多了用MCP。

## Optional Extension
如果学员问MCP和传统的微服务API有什么区别，回答：MCP在API之上加了一层"语义描述"，让大模型能理解每个工具是干什么的、需要什么参数。传统API是给人看文档调的，MCP是给AI理解调的。

## Classroom Interaction
无。

## Take Away
Tool让Agent能调外部系统。Function Calling是机制，MCP是标准。工具少用Function Calling，工具多用MCP。

## Why This Slide Matters
MCP是当前Agent工程化落地的关键标准。理解它能帮企业避免大量重复开发，也是后续Workflow和Multi-Agent的基础。

## Transition
有了Tool，Agent可以做单步动作了。但如果一个任务涉及多个步骤——查订单、查政策、人工审批、发起退款——一次工具调用搞不定。需要把这些步骤串成流水线，这就是Workflow。
