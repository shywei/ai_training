# Rules与上下文管理

## Slide Objective
阐述 Rules 的项目级约束作用，并深入讲解 Agent 的上下文构建优先级，帮助学员理解为什么有时候 Agent 会“失控”或“失忆”。

## Suggested Presentation
如果说 Skills 是给 Agent 增加技能，那 Rules 就是给 Agent 画红线。
比如你的项目严禁直接修改 main 分支，或者必须写单元测试，你就可以把这些写在 Rules 里。一个不懂 Rules 的 Agent，就像一个不看团队代码规范的实习生，代码写得再快也是在帮倒忙。
但是，无论是 Rules 还是 Skills，它们最终都要送进大模型的“工作记忆”里，也就是上下文（Context）。
大家看右边这张图，这是非常关键的一张图。
大模型的记忆容量是有限的，Agent 会怎么安排？最上面优先级最高的是系统提示词和 Rules，这些是死规矩，绝对不能忘；中间是当前任务的代码；最下面是过去的对话历史和工具返回的结果。
当信息太多、窗口装不下的时候，Agent 会从最下面开始丢弃信息。这就是为什么有时候聊得太久，Agent 会把之前的约定“忘”了。

## Key Emphasis
说明 Rules 的约束价值（红线）。
重点剖析右侧的“上下文优先级模型”，让学员明白信息是如何被挤占和截断的。

## Optional Extension
如果学员问 Rules 和 System Prompt 有什么区别？可以解释：System Prompt 通常是工具厂商写死的（比如“你是一个AI”），而 Rules 是你针对当前具体项目自定义的规范。

## Classroom Interaction
有没有遇到过让 AI 改一个长文件，结果它改到一半，突然把前面的代码给吞了或者忘了？这就是上下文溢出导致的信息截断。（建立痛点共鸣）

## Take Away
Rules 是不可逾越的项目红线；理解上下文丢弃优先级，才能避免让 Agent “失忆”。

## Why This Slide Matters
解释了由于 Token 限制导致的 Agent“失忆”现象的底层原因，这是排障排错的核心认知。

## Transition
我们把 Prompt、Rules、Skills 和记忆合在一起，就构成了 Agent 真正看到的世界。这其实引出了一个全新的领域——Context Engineering（上下文工程）。
