## Slide Objective
讲透 AGENTS.md 开放标准与 Scoped Rules（分目录作用域规则），掌握多工具兼容的规范治理方案。

## Suggested Presentation
请看第 10 页：支柱 ① & ② · AGENTS.md（跨工具开放标准）与 Scoped Rules。
过去，每个工具都有自己的私有规则文件：Cursor 叫 `.cursorrules`，Claude Code 叫 `CLAUDE.md`，Windsurf 叫 `.windsurfrules`。一个团队如果有人用 Cursor 有人用终端，规范就会严重割裂。
2025–2026 年，行业迎来了大一统开放标准：【`AGENTS.md`（agents.md）】！
现在主流工具（Cursor、Claude Code、阿里 Qoder、腾讯 CodeBuddy 等）均原生支持直接读取项目根目录下的 `AGENTS.md`。为了向下兼容，团队只需用软链接将 `CLAUDE.md` 指向 `AGENTS.md` 即可。
更高级的工程实践是【Scoped Rules（分层作用域规则）】：
不要把全公司的巨型规范都堆在根目录一个文件里！那样会撑爆 Token 且引发冲突。
应该采用“全局基础规则 + 目录级精准激活”：
- 根目录 `AGENTS.md`：保持在 150 行以内，只写构建命令、目录拓扑和绝对红线；
- 模块级 `src/api/AGENTS.md`：只约束 RESTful API 规范；
- 文件后缀规则（如改动 `*.sql` 时才激活数据库索引与字段命名规范）。
按需生效，干净高效！

## Key Q&A / Clarifications
**Q：为什么软链接可以在 Windows 上工作吗？**
A：在 Windows 上可以通过管理员终端执行 `mklink CLAUDE.md AGENTS.md` 创建，或者通过 Git 提交软链接。主流 Coding Agent 在 Windows 上均能无缝解析。

## Transition
但写在 AGENTS.md 里的规则再完美，终究只是文字建议。AI 不听怎么办？请看支柱三：Hooks。
