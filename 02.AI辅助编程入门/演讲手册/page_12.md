## Slide Objective
讲清 Skills（渐进式披露技能包）的运行原理与 skills.sh 开源生态，掌握避免 Token 爆炸的渐进式加载心法。

## Suggested Presentation
请看第 12 页：支柱 ④ · Skills（渐进式按需技能包）原理与生态。
如果把所有领域知识（Spring Cloud 配置、Kubernetes 运维、Docker 部署、MySQL 调优）全塞进 System Prompt，上下文不仅会迅速突破限制，更会造成严重的注意力稀释。
【Skills（技能系统）】采用了经典的【Progressive Disclosure（渐进式披露）】设计模式：
每个 Skill 是一个独立的文件夹，里面包含一个核心的 `SKILL.md`。
在会话启动时，Agent 只在上下文中加载这个 Skill 的**元数据与简要描述（仅占 30 个 Token）**：“当用户需要排查 JVM 内存溢出或使用 JStack 时，激活此技能”。
只有当用户真正发出相关指令时，Agent 才会自主发起内部工具调用，把该 Skill 详细的操作手册、排查步骤脚本动态加载进上下文！
在 2026 年，开源社区已经形成了庞大的 `skills.sh` 技能市场。你可以像用 `npm install` 一样，一键安装来自全球顶尖工程师沉淀的微服务排障、安全审计与数据库优化技能包！

## Key Q&A / Clarifications
**Q：Skill 和传统脚本有什么本质不同？**
A：传统脚本只能机械执行固定流程；而 Skill 是把“人类专家的 SOP 思维逻辑、边界经验、典型故障排查树”与“自动化辅助脚本”打包在一起，指导 Agent 在运行时自主决策和动态应对。

## Transition
我们通过一个真实的企业级 Java SKILL.md 文件，手把手看懂它的内部解剖结构。
