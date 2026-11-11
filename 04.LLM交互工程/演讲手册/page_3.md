## Slide Objective
让学员理解一次 LLM 调用的完整链路，建立"LLM 是 Agent 的 CPU"的认知。

## Suggested Presentation
看这张流程图。你的 Java 代码调 `ChatClient.prompt()`，背后发生了什么？

Spring AI 把你的消息序列化成 JSON——就是第四页那个请求体。然后发一个 HTTP POST 到服务商地址。你可以自己验证：在 application.yml 里把 `base-url` 改成一个不存在的地址，看报错信息——你会看到它尝试连接的就是一个 HTTP 端点。

服务端收到后，把文本拆成 token。Token 是什么？一个汉字通常 1-2 个 token，一个英文单词 1 个。你可以在硅基流动的控制台看到每次调用的 token 消耗数。这是计费单位——输出 token 比输入贵 2-5 倍。所以 Prompt 写得越精简，成本越低。

GPU 推理是瓶颈。Token 编解码是毫秒级，但推理可能几百毫秒到几秒。这就是为什么我们要用 Streaming——不等全部生成完再返回，逐 token 返回，用户立刻看到效果。你在前端聊天界面看到的"打字机效果"，就是 Streaming 的体现。

右边两个卡片：LLM 是 Agent 的 CPU，所有能力都通过它推理；推理是性能瓶颈，所以 Streaming 和模型选型很重要。这两个认知会贯穿后续所有课程。

## Key Emphasis
让学员在 Demo 里验证：改 base-url 看报错、在控制台看 token 消耗、在前端看 Streaming 效果。不是"听"，是"看"。

## Optional Extension
**学员问：一次调用要几秒，是不是网络问题？要不要加机器？**
答：先分清耗时在哪一段，再谈优化。
Demo 做法：在前端分别触发快模型（`agent.model.fast`）和推理模型（`agent.model.reasoning`），对比首包响应速度与生成总耗时差，就知道瓶颈在 GPU 推理而不在网络（在生产级工程中还需引入结构化日志组件记录精确毫秒耗时）。
生产做法：把耗时拆成排队/网络/首 token/生成四段分别上报，配合 token 用量才能定位瓶颈（第十一课可观测性）；首 token 延迟（TTFT）是流式体验的核心指标，要单独监控。

## Classroom Interaction
让学员把 `application.yml` 的 base-url 改成不存在的地址发一次请求，看报错信息里出现的 URL——验证"这就是一个 HTTP POST"，再改回来。

## Take Away
推理是性能瓶颈，Streaming 和模型选型都是从这个事实推出来的工程决策。

## Why This Slide Matters
跳过这页，学员会把慢归咎于代码或机器，做出"加服务器"这类错误决策——实际上 90% 的延迟在模型推理侧，加机器一分钱效果没有。

## Transition
既然是 HTTP 请求，那请求和响应的格式是什么？这就是 OpenAI 接口协议。
