## Slide Objective
演示 Terminal Coding Agent 的快速上手路径，掌握 TUI 交互、Headless 自动化与管道批处理三种工作模式。

## Suggested Presentation
请看第 18 页：Terminal Coding Agent 快速上手与三种工作模式。
终端 Agent 看起来像黑客终端，但上手其实只要一分钟。
比如 Claude Code（`npm i -g @anthropic-ai/claude-code`）或开源的 OpenCode（`pip install opencode`），配置好 API Key 后，在任何代码仓库根目录敲 `claude` 就能立刻进入工作状态。
它有三种无可替代的运行模式：
1. **TUI 全交互模式**：像聊天一样与终端对话，实时查看它调用的 Git、Bash 和文件编辑 Diff，敲回车即可授权执行；
2. **Headless 非交互自动化模式**：通过参数 `-p` 或 `run`，直接把任务当成命令行脚本执行！例如在 CI 脚本中一行命令：`opencode run "为 PaymentService 补齐边界单测并跑通 mvn test"`，执行成功返回 0，失败返回 1；
3. **Unix 管道模式**：把其他系统输出直接喂给它，例如：`tail -n 200 error.log | claude -p "定位 NPE 根因并给出修复补丁"`！
这种极客级灵活性，是任何图形化 IDE 插件都无法比拟的生产力杀手锏。

## Key Q&A / Clarifications
**Q：在 Windows PowerShell 下使用 Terminal CLI 会有路径或编码问题吗？**
A：现代 CLI（如 Claude Code 2026 版）对 Windows 路径均有良好原生兼容；推荐在 Windows 终端中确保编码为 UTF-8（`chcp 65001`），或在 WSL2 环境中运行，体验更佳。

## Transition
到这里，我们已经掌握了工具和 Harness。现在我们要直面最关键的日常痛点：第六章，为什么 AI 越聊越笨？
