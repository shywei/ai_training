## Slide Objective
讲透企业研发团队工具组合选型决策树，针对不同网络条件、IDE 习惯与预算给出落地实操方案。

## Suggested Presentation
请看第 17 页：企业研发团队工具组合选型决策树。
很多团队负责人问我：“我们到底该采购哪一款？”答案是：**没有唯一的全能神器，必须按维度分流组合！**
请沿着这棵决策树按顺序问自己三个核心问题：
第一问【网络与合规】：公网访问受限吗？代码能出境吗？
- 如果必须纯内网、数据零出境：选择 **自建开源模型集群（Qwen3-Coder）+ 部署 Roo Code / Cline 插件** 或采购企业私有化版通义灵码；
- 如果允许国内商业模型：直接上 **阿里 Qoder / 腾讯 CodeBuddy 企业版**。
第二问【主开发环境是 IDEA 还是 VS Code？】：
- 坚守 IntelliJ IDEA 的 Java 资深重度团队：推荐 **IDEA 原生插件模式（Qoder 插件 / Roo Code）+ 搭配 Claude Code 终端 Agent** 并行作业；
- 拥抱 VS Code 生态或全栈前端：直接全面拥抱 **Cursor / Antigravity**。
第三问【是否涉及批量自动化与流水线？】：
- 只要涉及批处理、无头运维、GitLab CI/CD：必须为高级工程师配备 **Claude Code / OpenCode CLI 工具**！

## Key Q&A / Clarifications
**Q：推荐个人开发者最高效的“双剑合璧”日常组合是什么？**
A：日常看代码、写单文件用熟悉的 IDE（IDEA 或 Cursor）；遇到跨模块大型重构、写迁移脚本或跑单测，直接在终端里开一个 Claude Code，效率最高！

## Transition
接下来，我们通过第 18 页，快速掌握 Terminal CLI Agent 的极速安装与三种经典运行模式。
