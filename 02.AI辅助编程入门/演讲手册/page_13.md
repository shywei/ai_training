## Slide Objective
手把手解剖一个生产级 Java SKILL.md 文件，掌握 YAML 元数据、触发条件、执行 SOP 与避坑清单的标准结构。

## Suggested Presentation
请看第 13 页：支柱 ④ 实战 · 手把手剖析一个企业级 Java SKILL.md。
大家看屏幕上这段标准 YAML + Markdown 代码，一个标准的技能文件必须具备三大解剖学结构：
1. **头部 YAML Frontmatter**：
   - `name: java-concurrency-audit`：技能唯一命名；
   - `description`：极其关键！这是 Agent 判断“何时该加载我”的唯一索引。必须写明适用场景（如并发编程、ThreadLocal 泄漏排查、锁升级）；
2. **Standard Operating Procedure（标准化执行 SOP）**：
   - 规定 Agent 必须按步推进：第一步，扫描代码中的 `synchronized`、`ReentrantLock` 与线程池配置；第二步，检查是否在异步任务中使用了 `RequestContextHolder`；第三步，运行并发压测单测；
3. **Common Pitfalls（避坑红线指南）**：
   - 明文列出高频暗坑，比如“严禁使用 `Executors.newFixedThreadPool`，必须手写 `ThreadPoolExecutor` 并指定有界阻塞队列”。
有了这样一份 Skill，即使是一个普通的实习生或新加入团队的 AI Agent，也能瞬间拥有十年资深架构师的并发审查功底！

## Key Q&A / Clarifications
**Q：如何让团队共享这套 Skills 资产？**
A：将通用 Skills 提交到团队的代码规范仓库，或者放在项目根目录的 `.skills/` 目录下，随 Git 仓库全团队统一分发生效。

## Transition
当 Agent 需要访问外部的企业内部系统（如 Jira、数据库、内部 Wiki）时，我们该如何连接？看支柱五：MCP 与 CLI。
