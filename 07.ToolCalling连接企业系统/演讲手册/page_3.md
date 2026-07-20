## Slide Objective
建立 Function Calling 的认知基础——LLM 天生不能"做事"，需要外部机制。

## Suggested Presentation
LLM 的本质是文本生成器。给它一段上下文，它生成下一段文本。它不能访问互联网、不能查数据库、不能调 API。它的"知识"来自训练数据，有截止日期。

但企业场景需要的能力远超"生成文本"：查实时价格（数据库查询）、查库存（数据库查询）、下单（调用订单 API）、发邮件（调用邮件服务）、更新 CRM（调用 CRM API）。这些 LLM 都做不到。

解决方案是 Function Calling：LLM 不直接执行操作，而是返回一个"调用指令"——"我要调用什么函数、传什么参数"。系统执行函数，把结果回传 LLM。LLM 基于结果生成最终回复。

类比：LLM 是大脑，Tool 是手脚，Function Calling 是大脑告诉手脚"做什么"的指令。大脑不需要知道手脚怎么动肌肉，只需要说"拿杯子"。

这个架构的关键是分离"决策"和"执行"。LLM 负责决策（调什么、传什么参数），系统负责执行（查数据库、调 API）。LLM 不需要知道执行细节，只需要知道有哪些函数可用、每个函数做什么。

## Key Emphasis
Function Calling 不是 LLM 的"内置能力"，而是"外部协议"。LLM 本身不会调用函数，是系统告诉 LLM "你有这些函数可以用"，LLM 返回"我要调这个"，系统再去执行。

## Transition
具体怎么告诉 LLM "你有这些函数可以用"？看 OpenAI 的 Function Calling 协议格式。
