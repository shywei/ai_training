## Slide Objective
系统呈现 02B 进阶选修课程的六大核心模块布局，理清“规格先行 → 存量重构 → 验证护栏 → 团队治理 → 并行流水线 → 靶场演练”的演进逻辑，引导学员建立企业级 AI 原生工程体系全貌认知。

## Suggested Presentation
请大家看屏幕上的课程全景路线图。作为面向项目 Leader 与高级开发的可选深潜课程，围绕“十万行企业存量工程的可控交付”这个核心命题，我们把整堂课切分为层层递进的六大章节：

**第一章【范式跃迁：Spec-Driven Development 企业级闭环】（P3–P6）**
我们将彻底剖析为什么“边聊边改”的 Vibe Coding 会在企业存量项目中暴毙。引入 2026 业界前沿的 SDD 三阶段工件——`requirements.md`、`design.md` 和 `tasks.md`，通过工业级 EARS 语法与 Spring Boot 订单幂等实战，展示如何把人类把关前移到 Spec 阶段。

**第二章【存量复杂工程（Brownfield）破局与安全重构】（P7–P10）**
直面 90% 没有文档的“屎山”真实战场。掌握利用只读 Subagent 逆向测绘三层 Repo Wiki 地图的方法；学习基于 Michael Feathers 经典的 Characterization Testing（黄金特征回归测试），在不触碰老逻辑的前提下给祖传代码穿上防弹衣；并建立跨模块复杂 Bug 的 RCA 科学根因排查四步法。

**第三章【Agentic TDD 验证闭环与防作弊纪律】（P11–P13）**
单测是 Agent 的眼睛。本章剖析 2 秒级方法定向验证的三道门禁，并揭露大模型为了“让单测变绿”而不择手段的三大专属恶习——篡改断言（Reward Hacking）、静默吞异常（Silent Fallback）与幻觉投毒包（Slopsquatting），传授物理级 Hook 拦截硬招与独立 Code Review 审查清单。

**第四章【团队级 Context 治理与工程免疫飞轮】（P14–P16）**
破解“3000 行巨型 AGENTS.md 导致模型迷失失灵”的企业痛点。推行“根目录全局宪法 + 子模块就近动态挂载 + Glob Rules 按需触发”的分层上下文架构；建立 Compound Engineering（复利工程）飞轮，把每次踩坑瞬间固化为全团队共享的代码疫苗。

**第五章【高阶提效：git worktree 并行与 CI/CD 无头 Agent】（P17–P18）**
打破单 Agent 串行等待瓶颈。利用 Git 原生 `worktree` 机制在 1 秒内挂载多个物理隔离目录，实现一人指挥 3 个 Agent 并发突击；并将 Headless 无头 Agent（`claude -p` / `opencode run`）挂载到 GitLab / GitHub CI 流水线，打造 7×24 小时自动合规审查与单测增量补齐流水线。

**第六章【企业靶场端到端实战演练与总结】（P19–P21）**
进入真刀真枪的演练环节。我们在真实的 `OpenCode 独立演练项目` 靶场中，现场编写分层 AGENTS.md、配置防作弊 Hook，手把手跑通 PII 敏感数据脱敏的 SDD 闭环与 JSON 解析器的特征测试重构，最后交付 AI 原生架构师的能力跃迁地图。

## Key Q&A / Clarifications
**Q：如果团队平时不写单元测试，能直接跑通 SDD 流程吗？**
A：可以跑通前半截（生成 requirements 与 design），但无法保证最终代码落地的可靠性。AI 辅助编程最大的催化效应，正是把“补写自动化测试”的边际成本从数小时降到了几十秒。本课第二、三章会手把手教大家如何让 AI 先自动生成黄金回归测试（Characterization Testing），先补全测试安全网，再开展重构或特性开发。

**Q：整个工作流中，人类工程师最不可替代的核心环节是什么？**
A：是**规格与架构评审（Step 2: Review Spec）**以及**最终合并把关（PR Review）**。AI 可以秒级生成设计初稿、切片单测和实现代码，但“方案是否契合企业长期演进方向、是否复用了公共二方资产、状态机回滚是否严密”，必须由具备系统所有权心智的人类架构师做最终裁决。

## Transition
明确了全课的进阶路径后，我们马上进入第一章：首先来看一看，为什么近年来被海外个人开发者捧上天的“Vibe Coding”，一放到企业存量项目中就会变成灾难现场？
