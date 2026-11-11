## Slide Objective
全景展示 Harness 驾驭层架构体系，建立“用确定性工程体系与安全底座死死约束大模型”的系统性世界观。重点突出 Coding Agent 的三大安全执行基石：Tool Permission（工具授权策略）、Approval Policy（人工审批机制）与 Sandbox（多维沙箱隔离），并阐明从任务规划、上下文装配到安全执行与测试反馈的完整闭环。

## Suggested Presentation
请大家打起十二分精神，看第 9 页：Harness 驾驭层全景架构。
这是 2026 年高水平 AI 架构师最核心的知识护城河。

很多非专业开发者以为 Coding Agent 就是“大模型帮我写代码”。
但如果你作为技术 Leader，在企业团队里放任模型在没有防护的情况下跑，不出三天就会发生删库删代码的重大生产事故！
大模型就像一匹烈马，智商极高但天生具有概率性和不可控性。这套给大模型套上的“缰绳、马鞍、围栏与操作系统”，就叫做 **Harness（驾驭层）**！

今天我们必须首先确立顶部的**三大安全执行基石（Three Security Foundations）**：
任何没有安全基石的 Coding Agent，本质上等同于一个拥有 `rm -rf` 和 `curl` 权限的高危脚本！
1. **第一基石：Tool Permission（授权策略）**。
   工具必须实行细粒度分级：读取文件（Read/Grep）是只读安全操作，可以自动放行；但写文件、执行 Shell、调用外部系统必须遵循白名单授权，默认禁止任何未受控的提权行为；
2. **第二基石：Approval Policy（人工审批机制 / HITL）**。
   凡是高危不可逆的操作（比如 `git push`、修改生产环境变量 `.env`、执行数据库清空操作），系统必须物理阻断并向人类工程师弹出审批确认框（Human-in-the-Loop），只有人点了允许，Agent 才能落地执行；
3. **第三基石：Sandbox（多维沙箱隔离）**。
   Agent 的文件系统访问必须被死死限制在当前项目的工作区（Workspace）内，严禁翻阅宿主机的 `~/.ssh/id_rsa` 或其他系统路径；Shell 必须在非特权沙箱中运行；网络出口（Egress）必须设防火墙，杜绝任何将企业私有代码向公网外联泄密的风险！

在三大安全基石的托底下，Harness 展开了**六大确定性支柱**：
- **System Prompt**：基座行为协议与工具格式（类似 OS 内核）；
- **AGENTS.md / Rules**：项目常驻工程宪法与代码红线；
- **Hooks（2026 核心升级）**：在工具调用前后毫秒级执行的确定性脚本门禁；
- **Skills**：按需渐进加载的专业领域工作流；
- **MCP & CLI**：连接外部系统与数据库的标准化客户端总线；
- **Subagents & Context**：主智能体与只读子智能体分工，实现上下文物理隔离。

大家看底部的**闭环执行流**：
用户下发需求 $\rightarrow$ 结合上下文装配 Prompt $\rightarrow$ 大模型输出调用工具的意图 $\rightarrow$ **三大基石与 Hooks 强力拦截校验** $\rightarrow$ 在沙箱中执行落地 $\rightarrow$ 提取单测和编译器报错反哺给模型自愈迭代！
这就是工业级 Coding Agent 可靠运行的真正秘密。

## Key Q&A / Clarifications
**Q：如果我已经配置了 AGENTS.md 告诉模型“不要删除文件”，还需要 Tool Permission 和 Sandbox 吗？**
A：绝对需要！`AGENTS.md` 只是写在 Prompt 里的“道德劝说（Soft Constraint）”。在长对话、上下文膨胀或遇到复杂异常时，大模型有一定概率产生注意力漂移从而违背规则。凡是关乎系统安全、不可逆破坏的操作，必须通过 Tool Permission、Approval Policy 和 Sandbox 进行底层“物理级阻断（Hard Constraint）”，绝不能寄希望于模型的自觉！

## Transition
在 Harness 体系中，到底如何用代码写出 100% 强制生效的确定性门禁？为什么说 Hooks 是对抗模型遗忘的终极武器？下一页，我们深入剖析支柱三：Hooks 确定性工程护栏。
