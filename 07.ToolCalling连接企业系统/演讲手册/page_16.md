## Slide Objective
把 Agent 互操作协议的版图立起来：MCP / A2A / AG-UI / ACP 各管哪一段、怎么组合、各自的关键事实与演进方向。纠正"Agent 互操作 = MCP 一个协议"的单点认知——本课 page_9~15 教的 MCP 只是"模型↔工具"这一段，Agent↔Agent（A2A）和前端↔Agent（AG-UI）各有专门协议。对应 Demo 的锚点是 MCP 这一段：`tool/McpProductClient`、`tool/McpKnowledgeClient`、`tool/McpToolCallback` 与两个 MCP Server（`mcp-product-server` / `mcp-knowledge-server`）；A2A / AG-UI / ACP 是仅 PPT 讲解的坐标系内容。

## Suggested Presentation
先给结论：**Agent 互操作已经三足鼎立，MCP 只是其中一足**。学员在本课学完 MCP 很容易形成"MCP = 全部"的印象，等到要让两个 Agent 协作、要在前端展示 Agent 过程时，才发现 MCP 根本不管这两段——企业选型时就会缺判断。

**MCP 管"模型↔工具/资源/上下文"**——就是本课 page_9 到 page_15 的全部内容：`McpProductClient.init()` 里 `listTools()` 发现工具、`McpToolCallback` 桥接成 FunctionCallback，这就是 MCP 在 Demo 里的实体。补三个演进事实，都是架构级的：Anthropic 2024-11 发布它；2026-07 协议内核大重构，核心转向**无状态请求模型**（标准 HTTP POST 传递 JSON-RPC，移除强制 initialize 握手与 Mcp-Session-Id 会话头，升级统一的 `server/discover` 动态发现端点，解耦 Tasks 为独立扩展，引入 OAuth 2.1 鉴权）——服务端不用再扛长连接会话，运维成本大降，这是"协议为了进企业微服务而改"的典型；MCP 2.0（2026-09 草案）方向是 Agent 联邦（跨 Agent 共享工具/资源目录）和远程资源订阅（变化推送）——也就是说 MCP 自己也在从"单模型接口"向"Agent 网络的资源层"长。

**A2A 管"Agent↔Agent"**：任务委派与信任基座。一个 Agent 接到干不完的活，要发现别的 Agent、声明能力、委派任务、跟踪生命周期、拿回结果——这套语义 MCP 不管。关键事实必须准确：Google 发起，v1.0 2026-03-12；2026-08-17 捐入 Linux 基金会（LF AI & Data）——协议进了中立基金会，企业可以放心押，不怕一家厂商说了算。这就是第十课 Multi-Agent 的协议语境：Router / Supervisor-Worker / Handoff 这些协作模式，委派动作最终要落在某个协议上，今天知道它叫 A2A。

**AG-UI 管"前端↔Agent"**：交互渲染与流式状态同步，相当于"UI 事件总线"。把思考、工具调用、中间状态以标准事件推给前端，用户"看得上"过程、能中途干预，而不是只等一个最终答案。没有它，每接一个 Agent 前端就重写一遍流式适配。第十一课企业平台的统一体验就落在这层。

**ACP** 一笔带过：另一套 Agent↔Agent 消息/会话互通协议，与 A2A 同赛道、生态不同。给学员的判断是"选型看生态与治理归属"，不是背两套协议细节。

**组合而非替代**，看右下角那条链：前端 —AG-UI→ Agent A —MCP→ 工具/资源，Agent A —A2A→ Agent B —MCP→ 工具/资源。垂直接工具、水平接 Agent、面向用户接前端，三段各就各位。一个企业应用里三个协议同时在跑是常态——这就是"各管一段"的含义。

区分覆盖范围：本课 Demo 只实现了 MCP 段；A2A / AG-UI / ACP 是坐标系，讲边界和选型判断，不展开实现。Demo 侧能验证的只有 MCP 段（page_31 的验证点）。

## Key Emphasis
必须讲清：① 一句话分工（接得上工具 / 连得上 Agent / 看得上过程）；② A2A 的治理事实（v1.0 2026-03-12、2026-08-17 捐 Linux 基金会 LF AI & Data）——这是"敢不敢押"的判断依据；③ MCP 的演进方向（2026-07 无状态重构、MCP 2.0 草案的 Agent 联邦与远程资源订阅）；④ 组合而非替代——三个协议会在同一个应用里同时出现。
不要展开：A2A 的 Agent Card 报文结构、AG-UI 的事件类型清单、ACP 与 A2A 的字段级差异——协议细读留给课后，课上只给边界和选型判断。

## Optional Extension
**学员问：A2A 出现以后，MCP 是不是要被淘汰了？**
答：不构成替代关系，管的不是同一段。MCP 解决"模型怎么接工具"，A2A 解决"Agent 怎么委派 Agent"——一个垂直一个水平。
Demo 做法：本课 Demo 就是纯 MCP 段——`McpToolCallback` 把 MCP Tool 桥成 FunctionCallback 给模型调，全程没有 Agent 间对话，所以根本用不到 A2A；用不上不等于不存在，是这段链路里没有"Agent↔Agent"的边。
生产做法：一个客服 Agent 要把"退换货"委派给售后 Agent，委派走 A2A（能力发现 + 任务生命周期），两个 Agent 各自继续用 MCP 接自己的工具——两层协议叠着用。取舍：只用 MCP 硬凑 Agent 间调用，会把委派语义（进度、重试、信任）揉进业务代码，多 Agent 一上规模就散架。第十课展开协作编排。

**学员问：企业现在要不要上 A2A / AG-UI？**
答：按链路缺口上，不按技术热情上。你的系统缺哪段的标准化，就补哪段。
Demo 做法：Demo 是单 Agent + 无独立前端过程展示，MCP 一段够用——不要为了"用协议"而加协议。
生产做法：先盘点三段现状——工具接入乱不乱（乱→MCP 网关化，第十一课 MCP Gateway）、有没有跨团队 Agent 互调（有→评估 A2A，它已进 Linux 基金会、v1.0 稳定）、前端是否在为多个 Agent 重复写流式适配（是→AG-UI）。三段独立决策，互不捆绑。取舍：过早上 A2A，单 Agent 场景纯属背成本；不上 AG-UI，前端体验会一直绑死在某个 Agent 的私有流式格式上。

## Classroom Interaction
给三个场景让学员贴协议标签："模型要查 ERP 库存"（MCP）、"售前 Agent 把退换货工单交给售后 Agent"（A2A）、"前端要显示 Agent 正在调工具的步骤条"（AG-UI）。让学员说出为什么第二个不是 MCP——多数错在把"Agent 调工具"和"Agent 调 Agent"混成一件事。

## Take Away
按"垂直接工具= MCP、水平接 Agent = A2A、面向用户 = AG-UI"三段给系统贴协议标签，缺哪段补哪段；选协议看治理归属（基金会/厂商）和生态，不要赌单一协议包打天下。

## Why This Slide Matters
跳过这页，学员会把 MCP 当成 Agent 互操作的全部：做 Multi-Agent 时拿 MCP 硬凑 Agent 间调用、做前端过程展示时私造流式格式，等第十课、第十一课要用 A2A / AG-UI 时才发现地基没打——届时改造的是调用链和前端两头。

## Transition
协议版图立住了：MCP 是本课的主战场，A2A / AG-UI 是后面两课的地基。回到本课收尾——这些能力落地之后，Agent 的架构怎么变：page_17 从能力到应用。
