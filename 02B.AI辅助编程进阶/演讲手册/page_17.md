## Slide Objective
深入讲解基于 Git Worktree 的多 Agent 并行研发工作流实战，掌握“一人指挥多个独立 Agent 并发开发”的高阶工程手法。同时严肃澄清 Git Worktree 的隔离边界：明确 Worktree 只提供工作区与分支隔离，不等于独立的数据库、端口或容器运行环境，并辨析研发态 Multi-Agent 与运行时业务 Multi-Agent 的范式定位。

## Suggested Presentation
我们来到第五章：高阶提效：Git Worktree 并行与 CI/CD 无头 Agent。请看第 17 页。
很多工程师用 AI 编程时经常遇到这样的痛点：
你让 Agent 按照 SDD tasks 重构代码、跑全套单测，由于测试较多，Agent 需要执行 3 到 5 分钟。
在这 5 分钟里，你的 IDE 工作区被占满了，你根本不敢动代码，只能坐在椅子上刷手机发呆；
如果你强行在同一个目录下打开另一个终端、让另一个 Agent 去修一个紧急线上 Bug，两个 Agent 会瞬间在同一个文件系统里相互覆盖代码、引发毁灭性的踩踏灾难！

怎么破解？**Git 原生最强大的隐秘武器——`git worktree` 登场！**
Git Worktree 允许你在本地磁盘上，从同一个 `.git` 仓库秒级挂载出多个完全独立的物理文件夹（如 `../wt-feat-pii`、`../wt-hotfix`、`../wt-exp`），每个文件夹分别绑定一个独立的 Git 分支！
这样，你一个人就能同时指挥 3 个并发 Agent：
- **Agent #1 在 Worktree 1 推进主线特性**；
- **Agent #2 在 Worktree 2 紧急排查修复线上 Bug**，无需任何 `git stash`，修完直接提 PR；
- **Agent #3 在 Worktree 3 做激进的性能重构实验**，跑 Benchmark 竞标，好用就合入主线，不好用直接 `git worktree remove` 一键销毁！

**但是，请各位架构师务必清醒认识到底部的【架构师警钟】：**
**“Git Worktree 隔离，绝不等于运行环境隔离！”**
很多团队以为开了 Worktree 就高枕无忧了，结果 3 个 Agent 一跑单测，控制台一片报错：
1. **端口冲突**：Agent #1 和 Agent #2 的单测同时启动了本地 Web 服务，两个进程死抢 `:8080` 端口，导致端口占用报错；
2. **数据库脏写污染**：多个 Agent 并发读写本地同一个物理 MySQL 测试库，A 刚写入的数据被 B 删除了，导致测试偶发红灯！

**企业级正确实践方案：**
- **单测解耦物理端口与数据库**：配置 Spring Boot 动态随机端口（`server.port=0`），单测使用内存库 H2 或 Testcontainers 独立临时容器；
- **环境沙箱化**：结合 Docker Compose，为每个 Worktree 分配独立的容器网络与测试数据库实例；
- **范式定位切分**：请大家牢记，这种基于 Worktree 的多 Agent 并行是**“编码阶段 Multi-Agent（软件研发任务并行）”**，负责提效开发；它与第 10 课生产环境中的**“运行时业务 Multi-Agent（多智能体微服务协作）”**有着完全不同的架构边界与协作协议！

## Key Q&A / Clarifications
**Q：Git Worktree 和直接 `git clone` 出 3 个独立的仓库目录相比，有什么优势？**
A：优势在于磁盘占用与对象共享！`git clone` 复制 3 次需要把全部 `.git` 历史对象重复下载 3 遍，浪费数十 GB 磁盘且同步繁琐；而 `git worktree` 共享底层同一个 `.git` 目录，所有提交对象、分支信息秒级互通，创建新工作区耗时不到 1 秒，内存和磁盘开销几乎为零，是最优雅的本地并行利器。

## Transition
本地多 Agent 并行跑顺畅了，那么当工程师下班后，能不能让无头 Agent（Headless Agent）直接驻守在 GitHub Actions 或 GitLab CI/CD 流水线里，7×24 小时自动修复依赖漏洞与跑自动化评审？请看第 18 页：Headless Agent 融入 CI/CD 研发流水线。
