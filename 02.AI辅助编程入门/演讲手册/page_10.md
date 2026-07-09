# Skills与MCP详解

## Slide Objective
深入解释 Skills（技能）和 MCP（模型上下文协议）的价值，让学员明白如何通过这两个机制大幅扩展 Agent 的能力边界。

## Suggested Presentation
我们来单独看看 Skills 和 MCP。
Skills 是预定义的能力模块。它其实就是一个 `SKILL.md` 文件。在这个文件里，你写好了任务说明、示例和工具用法。Agent 只要读了这个文件，就相当于加载了一个技能包。
比如，团队里经常需要做特定的代码审查，你可以写一个“安全审查规范”的 Skill 放进项目里，Agent 以后就会自动按照这个规范去审查代码。
但如果 Agent 需要去读数据库，或者去调 Jira 接口呢？这时候就需要 MCP 了。
MCP 是一个极其重要的标准化协议。过去，我们要让 AI 连数据库，得专门给它写一套接口；要连 Git，又要写一套。现在有了 MCP，外部工具只要实现一次 MCP 协议，所有的 Agent（比如 Cursor, Claude Code）就都能直接调用它了。
大家看下方的例子，通过 MCP，Agent 可以操作文件系统、执行 SQL 语句、甚至通过 SSH 管理服务器。
Skills 给了 Agent 做事的方法，而 MCP 给了 Agent 触达外部世界的手脚。

## Key Emphasis
重点讲清 Skills 的“文本定义”本质（就是一个 Markdown 文件），以及它的团队复用价值。
重点讲清 MCP 的“标准化接口”价值，它是连接大模型与外部基础设施的桥梁。不需演示具体 MCP 协议的 JSON 格式代码。

## Optional Extension
如果学员问现在有哪些现成的 MCP Server 可以用？可以回答：GitHub 上已经有大量开源的 MCP Server，比如操作 PostgreSQL 的、搜索网页的、看 Slack 消息的，甚至自己用 Python 几百行代码就能写一个。

## Classroom Interaction
(无强交互)

## Take Away
Skills 定义了做事的方法，MCP 提供了连接外部系统的标准化手脚。

## Why This Slide Matters
这两个概念是目前高阶 AI 辅助编程最前沿、最具想象力的部分。理解了它们，学员就具备了自行扩展 Agent 功能的架构思维。

## Transition
明白了 Agent 的底层逻辑，那市面上到底有哪些成熟的 Coding Agent 工具呢？我们该选哪一个？这就是我们第五章要揭晓的全景图。
