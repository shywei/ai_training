## Slide Objective
开场定调。明确本课的定位：从"理解 AI"进入"用 Java 构建 AI 应用"。

## Suggested Presentation
从这节课开始，我们进入实战。前三课建立了认知，今天开始写代码。

这节课的主题是 LLM 交互工程。你可能觉得"调个 API 不就行了"——但实际做起来，你会遇到一连串问题。我举几个真实的例子，都是我们在做 Demo 过程中踩过的坑：

第一个坑：Spring AI 的 base-url 不能包含 `/v1`。我们一开始配置了 `https://api.siliconflow.cn/v1`，结果请求发到了 `/v1/v1/chat/completions`，404。因为 Spring AI 会自动拼接 `/v1/chat/completions`。这个错误在文档里没写，是实际跑出来的。

第二个坑：Spring AI M4 版本的 builder 方法名不是 `model()`，是 `withModel()`。我们一开始按网上教程写的 `model(model)`，编译报错。查源码才发现改名了。这就是实战——框架版本更新，API 跟着变。

第三个坑：硅基流动的模型名格式。一开始我们写的是 `Qwen/DeepSeek-R1-0528-Qwen3-8B`，400 错误。后来发现正确的格式是 `deepseek-ai/DeepSeek-R1-0528-Qwen3-8B`。模型名写错不会报编译错误，只有运行时才会发现。

这些坑，PPT 上不会写，但你做项目时一定会遇到。今天我会把 Demo 里踩过的每一个坑都讲清楚。

## Key Emphasis
不要只讲"我们学什么"，要用真实的踩坑经验建立学员的信任感——"这个讲师是真正做过项目的"。

## Optional Extension
**学员问：这门课跟市面上"三行代码调大模型"的教程有什么区别？**
答：区别在工程闭环——不是调通，是调好、调稳。
Demo 做法：本课把一次调用拆成完整链路——`PromptManager` 构建提示词、`ModelRouter` 选模型、`StructuredOutputHandler` 保证输出可解析、`GuardrailsService` 做入口/出口防护，你能跑通一次带防护、带结构化输出、控制台可回溯的完整调用。
生产做法：Demo 之外还要补七件工程件——Key 管理、成本核算、限流退避、PII 脱敏、超时重试、模型降级、日志留存（本课 page_16 一页讲透），市面上的教程几乎都不讲这些，而这才是上线与否的分界线。

## Classroom Interaction
开场先跑起来：切到 `v1.1.1` 启动 Demo，点一次「结构化输出演示」按钮，看后端控制台打印出完整的五步解析输出——先建立"代码是能跑的"这个共识，再讲原理。

## Take Away
LLM 交互不是"调个 API"，是一条要自己补齐工程件的链路——调通只是第一天，调稳才是工程。

## Why This Slide Matters
跳过这页（不先定调），学员会把本课当 API 教程听，学完只会 curl，遇到解析失败、成本失控、Key 泄露这类真问题时完全没有概念。

## Transition
要和大模型交互，首先得知道一次调用到底发生了什么。
