# Skills详解：怎么写、怎么用、去哪找

## Slide Objective
通过展示真实的 Skill 文件，剥掉 Skills 的神秘外衣，让学员认识到它只是一个包含特定结构的 Markdown 文件，并学会如何获取和使用开源 Skills。

## Suggested Presentation
上一页我们说 Skills 是能力模块，那它到底长什么样？
大家看左边，这是一个真实用来做 TDD（测试驱动开发）的 Skill。
它其实就是一个纯文本的 Markdown 文件，叫 `SKILL.md`。最上面是名字和描述，下面就是具体的规矩：比如告诉你先写测试，然后再写代码。
这完全不是什么高深的黑科技，对吧？它就是把一个高级程序员做事的“套路”写了下来，然后告诉 Agent：“以后凡是遇到 TDD 任务，就按这个套路做”。
那我们怎么用呢？现在社区已经非常成熟了，像 OpenCode 或者 Claude Code，都可以直接用命令行一键安装别人写好的 Skills。
大家不用从零开始写，GitHub 上、skills.sh 这个网站上，有几十万个别人写好的开源技能。这就相当于你一键就能把全球顶级工程师的经验，安装到你的 Agent 脑子里。

## Key Emphasis
重点说明 Skill 的本质：固化经验的文本文件。
展示从哪里找、怎么安装，降低学员对“编写技能”的畏难情绪。

## Optional Extension
如果学员问为什么文件名一定要叫 SKILL.md，可以解释：这是目前各大 Agent 框架约定俗成的标准协议，就像项目的 README.md 一样，Agent 会自动扫描特定目录下的这个文件。

## Classroom Interaction
(无强交互)

## Take Away
Skill 的本质是把专家经验固化成文本文件，它极大降低了知识复用的门槛。

## Why This Slide Matters
如果不展示具体文件，学员会觉得 Skill 是某种需要编码开发的复杂插件。展示代码能立刻打破这种错觉，激发他们复用开源生态的动力。

## Transition
Skills 告诉 Agent “该怎么做”，但有时候我们更需要告诉 Agent “绝对不能做什么”，这就需要用到 Rules。同时，所有的规矩和技能，最终都要塞进上下文里。来看下一页。
