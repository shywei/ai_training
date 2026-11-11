## Slide Objective
深入剖析 2026 最具确定性的护栏武器：Hooks（生命周期钩子）与三大安全执行基石（Tool Permission 授权策略、Approval Policy 人工审批、Sandbox 多维沙箱隔离）的联动机制。深刻理解“确定性代码拦截与沙箱兜底 vs Rules 文字道德劝说”的本质飞跃，掌握在本地和企业开发环境中构建不可逾越安全防线的方法论。

## Suggested Presentation
请大家看第 11 页，这是区别初级开发者与高级架构师的关键一页：Hooks 确定性护栏与三大安全执行基石的联动！

在日常指导团队推行 AI 编程时，很多 Team Leader 非常头疼：
“我明明在 `AGENTS.md` 里写了五遍‘改完代码必须自动跑单测格式化，禁止删除测试断言，禁止执行 `rm -rf`’，为什么 Agent 偶尔还是会偷偷删掉断言骗取绿灯，甚至在终端里误删核心文件？”
原因非常简单：
**写在 `AGENTS.md` 里的文字，在数学上只是给大模型概率采样的‘Soft Constraint（软性道德劝说）’！**
只要对话超过 15 轮、或者上下文膨胀到数万 Token，注意力一分散，大模型完全可能把你的条条框框抛到九霄云外！

凡是关乎系统安全、数据破坏和工程红线的事情，**绝不能寄希望于模型的自觉！必须靠确定性的 Hooks 联动三大安全基石（Hard Constraint）：**
1. **第一切面：`PreToolUse Hook` 联动【Tool Permission】与【Approval Policy】**。
   当 Agent 在脑子里产生了“我要在终端执行命令”的意图时，命令并没有真正跑到操作系统里！
   在落地的毫秒之前，`PreToolUse` 拦截器被唤醒：
   - 检查 **Tool Permission**：这个命令在白名单里吗？如果是 `rm -rf`、`git push --force` 或清空数据库，脚本直接返回 `Exit Code 2`，硬件级阻断执行！
   - 检查 **Approval Policy**：如果是修改生产配置或者调用外部接口，Hook 脚本自动挂起 Agent，在屏幕上向人类工程师弹出审批确认框：“Agent 申请执行该高危操作，是否批准？” 人类点了拒绝，操作当场流产！
   - 检查 **Sandbox 边界**：Agent 试图读取或者写到工作区以外的私密目录（如 `~/.ssh/id_rsa`）？沙箱层直接拦截报错！
2. **第二切面：`PostToolUse Hook` 自动化质量流水线**。
   每当 Agent 通过 `Edit/Write` 修改完代码，**根本不需要大模型操心**，宿主机自动静默执行 `mvn spotless:apply` 代码格式化和增量语法编译。如果编译报错，Hook 直接把报错堆栈塞回上下文，让模型立刻针对性自愈；
3. **第三切面：`Stop / TaskComplete Hook` 验收守门员**。
   当 Agent 试图说“我搞定了，任务已完成”时，触发验收 Hook：自动在后台运行受保护的单元测试套件。**只要单测有一个红灯，立刻强行驳回任务停止请求**，并提示：“单元测试未通过，禁止下班，请继续定位修复 Bug！”

大家看右下角的对比表：
`AGENTS.md` 规则是“软建议”，每轮都在消耗宝贵的上下文 Token；
而 **Hooks + 权限审批 + 沙箱** 是“硬法则”，在本地 Shell 和沙箱中 100% 确定性执行，**消耗 0 个模型 Token！**

作为架构师，我们要确立这样的心智：
**把编码指引和风格规范写进 `AGENTS.md`；但把所有关于破坏性操作、代码防篡改与质量门禁的红线，坚决下沉到 Hooks 与安全基石中！**

## Key Q&A / Clarifications
**Q：在 VS Code / 终端中运行本地 Agent 时，如何快速落地这套安全拦截体系？**
A：主流工具如 Claude Code 官方内置了强大的 Hook 机制（在 `.claude/settings.json` 中配置 `PreToolUse` / `PostToolUse` / `Stop`）；OpenCode 与 Roo Code 支持通过本地脚本和沙箱扩展进行拦截；此外，在企业级工程中，可以在团队根目录放置统一的 `./scripts/guard.sh`，强制要求所有本地 Agent 挂载该脚本执行权限审查。

## Transition
掌握了硬护栏与安全执行基石后，当 Agent 需要执行特定的高阶领域任务（比如 TDD 规范落地、接口文档自动生成、慢 SQL 诊断）时，我们该如何按需给它赋予专业能力而又不撑爆上下文？请看支柱四：Skills。
