# Agent与LLM：谁负责什么？

## Slide Objective
明确区分 Agent（系统架构）与 LLM（推理引擎）的职责边界，避免学员把两者混为一谈，同时再次强化 Context（内容）与 Window（容量）的区别。

## Suggested Presentation
很多人到现在还会把 Agent 和大模型混为一谈。
其实它们完全不是一回事。打个比方，Agent 是秘书，LLM 是专家。
秘书（Agent）的作用是什么？它负责去项目里搜索代码，去读你的规则，去跑测试工具，然后把所有这些资料整理好。
专家（LLM）的作用是什么？专家连电脑都不会用，他只负责坐在办公室里，看秘书整理好递过来的那叠资料，看完之后进行思考、推理，然后给出解决方案。
所以大家看左下角：Agent 负责准备信息，LLM 负责理解信息。
右边我们要区分另一个概念：Context（上下文）和 Context Window（上下文窗口）。
Context 是秘书整理出来的内容；Context Window 则是专家一次能看完的最大字数容量。如果资料太多（Context 太大），超过了专家的阅读上限（Window），秘书就必须对资料做裁剪或摘要。
Agent 的核心工作流就是不断地：搜索整理资料 -> 塞给模型看 -> 获取结果去执行 -> 继续循环。

## Key Emphasis
重点讲透“秘书（Agent）与专家（LLM）”的比喻。
厘清“Context（内容）”与“Context Window（物理容量）”的区别。

## Optional Extension
如果学员问为什么有时候 Agent 总是反复犯同一个错误，可以引导：这通常不是专家（LLM）变笨了，而是秘书（Agent）没有把错误日志放进资料堆（Context）里递给专家。

## Classroom Interaction
(无强交互)

## Take Away
Agent 是整理资料、调用工具的“秘书”，LLM 是负责思考推理的“专家”。

## Why This Slide Matters
如果不分清这两者，当生成代码出问题时，学员会一味地去换大模型，而不知道其实是 Agent 在组装上下文或调用工具时出了问题。

## Transition
了解了 Agent 的运作机制后，我们来看看，2026 年的今天，市面上到底有哪些主流的 Agent 产品，它们各有什么特点。
