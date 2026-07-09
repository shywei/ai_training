## Slide Objective
展示 OpenAI 协议定义的全部标准能力，让学员理解"一个协议，覆盖所有场景"。

## Suggested Presentation
一个 POST 请求，通过不同的参数组合，就能调用大模型的全部标准能力。

上面三个是基础能力：Messages 定义对话（system/user/assistant），Streaming 控制返回方式（逐 token 还是一次性），Temperature 控制创造性（0.0 确定、0.7 平衡、1.0+ 随机）。

下面三个是进阶能力，也是后续课程的基础：
Function Calling——你在请求中定义工具列表，模型判断需要调用哪个工具、传什么参数。模型不直接调用，而是返回调用意图，由你的服务端执行。这是第七课 Tool 的基础。
Structured Output——通过 response_format 参数强制输出 JSON Schema，模型保证输出符合你定义的结构。这就是本课后面讲的"方案二"。
Token 计数——响应中返回 usage.prompt_tokens 和 completion_tokens，用于成本监控。企业必须记录这个。

底部这句话记住：一个 HTTP POST + 一套 JSON 参数 = 大模型的全部标准能力。

## Key Emphasis
不要深入讲每个能力的细节（后续课程会讲），重点是让学员建立"协议即能力"的认知。Function Calling 和 Structured Output 点到为止，告诉学员"后面会用到"。

## Take Away
OpenAI 协议不只是对话，它定义了工具调用、结构化输出、Token 计数等全部标准能力。

## Transition
协议讲完了。接下来我们看两个直接影响成本和体验的关键概念：Token 和 Context Window。
