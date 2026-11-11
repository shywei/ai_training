## Slide Objective
把全课知识点落成八道动手题，每题锁一个核心知识点并指到真实代码：手写 function definition（协议）、加 Tool 验证自动发现（MCP）、路由扩一类（LLM 路由）、幂等键设计（幂等）、上限流（限流）、超时与补偿设计（超时+补偿）、导出审计记录（审计）、五件套综合大作业。练习不是"复习题"，是补上"听完"和"会做"之间的距离——尤其 4/5/6/7 直接对应治理五件套的空白格。

## Suggested Presentation
逐题讲清"做什么、对照哪段代码、答什么问题"。**练习 1 · 手写 function definition**：不看课件给 `queryStock` 手写一份 function definition JSON（name / description / parameters，含 productId 的 JSON Schema），写完对照 `mcp-product-server` 的 `McpServerConfig.java` 里 `productIdSchema` 与 Tool 注册，逐字段说清对应关系——目的不是背格式，是理解"Schema 就是 LLM 看到的函数签名"。**练习 2 · 加一个 Tool**：在 product-server 加 `queryCategory`（改 `McpServerConfig` + `ProductRepository`），重启后在 agent-runtime 控制台确认被自动发现并包装——验证"加 Tool 不用改 Client"；追问题：Schema 写错时 LLM 会怎么传错参数？**练习 3 · 路由扩一类**：给 `LlmRouter` 的三分类加一类（如 LOGISTICS 查物流），改分类 Prompt 与 `Route` 枚举，用 `POST /api/agent/recommend` 验证、看 `logs/llm-route.log`；必答题：为什么 temperature 必须是 0（路由必须稳定——同一句话走不同路由就是不同的注入内容和模型，答案和成本都不可预测）。

**练习 4 · 幂等键**：对照 `mcp-knowledge-server` 的 `save_memory`（`MemoryService.save()` 裸 INSERT）或假设在 `mcp-product-server` 新增一个 `place_order` 下单 Tool，设计完整幂等方案——键的构成（`requestId/sessionId+tool+参数哈希`）、去重表 DDL（或唯一索引）、重复请求返回什么（回放首次结果）；要求写出"超时后重试"的完整时序，标清键在哪一步生成、哪一步判重。**练习 5 · 限流**：用 Guava RateLimiter（或 Redis+Lua）给 `queryProductInfo` 配 10 次/分钟，写循环调用测试，观察第 11 次返回什么；排队/降级/429 三选一里选一个，写清理由与取舍。**练习 6 · 超时重连实验与补偿设计**：把 `McpProductClient` 的 `requestTimeout` 改成 1 秒并停掉 product-server，观察调用报错与 `ensureConnected()` 10 秒冷却重连日志；再为"下单 → 扣库存 → 发通知"设计补偿链——哪些有反向操作、哪些转人工、对账比什么。**练习 7 · 从现有日志拼出一条审计记录**：执行一次商品价格查询后，从控制台 `[MCP] 调用 Tool` 日志与 `logs/llm-chat.log` 拼出一条审计记录（谁/何时/调了什么/参数/结果/耗时），对照审计表字段清单指出 `v1.4` 现有日志还缺什么、为什么不能用文本日志代替审计表。**练习 8 · 综合大作业**（课后一周内交）：选一个企业里真实的 API（订单/审批/工单都行），按五件套写治理设计——限流配额（粒度+算法+超限策略）、幂等键（构成+判重点）、超时档位、补偿（正向/补偿 Tool 对+对账）、审计字段，**每项写"不做的后果"**。布置时把话说明白：这门课的重点不是怎么调 Tool，是怎么管 Tool——练习 4~8 全在练"管"。

## Key Emphasis
必须讲清：① 每题对应的工程结论（如练习 1 的"Schema 即函数签名"、练习 3 的"路由必须确定性"）；② 练习 4/5/6/7 就是把五件套的空白格亲手补一遍。不要在课堂上把所有题都讲完——课堂只讲"做什么+看哪段代码"，动手留课后；练习 2 的 Schema 容错、练习 5 的三选一讨论可以在课上先起头。

## Optional Extension
**学员问：练习 1 手写 JSON Schema 是不是过时的基本功？现在框架都有 `@Tool` 了。**
答：基本功不过时，写法会变。
Demo 做法：手写 `queryStock` 的 function definition 并对照 `McpServerConfig.java` 的 `productIdSchema`——写一遍就明白 `@Tool`/声明式注解帮你生成的是什么，以及生成错了怎么排查。
生产做法：Spring AI 2.0 的 `@Tool` 注解、AgentScope Java 2.0（第九课重构用它）的 Tool 声明都在做同一件事——从代码声明生成协议定义；升级映射表里"手写 Schema → 声明式生成"正是第九课要讲的"框架接管了什么"。取舍：只用声明式不懂协议，MCP 跨团队联调时（对方 Server 的 Schema 不是你生成的）就看不懂；懂协议的人排查快一个量级。

**学员问：练习 8 选的 API 涉密/拿不到内部细节，怎么写治理设计？**
答：治理设计不需要源码，需要的是接口契约和事故想象力。
Demo 做法：先用 Demo 里的 `place_order`/`queryProductInfo` 练一遍五件套的写法（练习 4~7 已给出骨架），把格式跑熟。
生产做法：对着 API 文档（入参/出参/SLA/错误码）写：从下游 SLA 反推限流配额、从"执行两次世界会不会不一样"判幂等必要性、从操作可逆性定补偿对、按审计五问列字段——写不出代码级细节没关系，要写的是决策与取舍。取舍：拿脱敏后的真实接口写最有价值；实在不行拿公开 API（银行转账/审批流）代入，关键是每项都写出"不做的后果"。

## Classroom Interaction
课上留 10 分钟现场做练习 1 的前半：人手一份 queryStock 的 JSON Schema 空模板，填 name/description/parameters，前后桌互相对照 `McpServerConfig.java` 的 `productIdSchema` 打分——哪一字段写错会怎么误导 LLM。起个头，课后才做得动。

## Take Away
八道题的验收标准是同一个：能对着真实代码说清"这个治理动作在哪实现、不做的事故是什么"——尤其幂等、限流、超时、补偿、审计五题写完，你对企业该不该放行 Agent 接生产系统就有判断力了。

## Why This Slide Matters
跳过练习，学员的真实水平会停在"听得懂"：手写不出一份 Schema、加 Tool 时不知道 Schema 错在哪、给写接口加重试时想不起幂等键、评审别人方案时说不出"不做的后果"。八道题就是把这些"以为会了"变成"真的会了"。

## Transition
练习布置完，回头收束全课——第七课给了 Agent 三个核心能力，对照能力地图看自己走到哪，然后看第八课要往哪走。
