## Slide Objective
指导学员在真实的配套靶场通用企业级独立演练工程 `demo-opencode-enterprise`（Java 17 + Spring Boot 3）中，亲手落地两大核心基础设施工件：精简高信噪比的根目录 `/AGENTS.md` 与物理阻断测试篡改及私自引包的 PreToolUse 防作弊 Hook，为后续实弹演练筑牢确定性底座。

## Suggested Presentation
各位工程师，现在请大家打开你们的开发机终端，进入我们本次培训统一配套的实战靶场目录：
`cd demo-opencode-enterprise`
这是一个通用的企业级独立演练工程 demo-opencode-enterprise，包含 core-service、order-service、test-suite。

在这一节实操中，我们要在靶场中完成**两大核心 Harness 设施的落地上膛**（对应实操手册《Demo演示步骤.md》中的“演示 1”）：

**工件 1：工程根目录 `/AGENTS.md`（高信噪比全局宪法）**
请看左侧屏幕，这是针对本工程精心裁剪后的全局规约模板，严格控制在 100 行内：
- 第一节给出全模块通用的快速编译命令 `mvn test-compile -T 1C -q`，以及精准到类和方法名的秒级单测运行模板；
- 第二节清晰绘制了三个模块的职责拓扑与边界红线：`core-service`（通用业务逻辑与工具类）、`order-service`（订单领域服务）、`test-suite`（自动化测试套件），明确依赖方向，严禁反向污染；
- 第三节立下企业研发的四大不可妥协铁律：跨方法需求必须 Spec 先行、实现与重构阶段绝对禁止修改 `src/test/` 断言、安全护栏必须 Fail-Closed 严禁吞异常、以及 `pom.xml` 依赖锁死。
这份文件保存后，任何接入该工程的 Agent 都会在第一秒自动建立对工程的敬畏之心。

**工件 2：物理防篡改 Hook 脚本（`.opencode/hooks/protect-tests.sh`）**
光靠文字提醒是防不住大模型作弊的，我们必须给它戴上物理紧箍咒！请看右侧屏幕的 Shell 拦截脚本：
- 我们在工作区配置一个轻量级的 `PreToolUse` Hook。每当 Agent 试图调用文件编辑工具时，Hook 会拦截并解析其入参中的 `file_path`；
- 如果它试图修改 `pom.xml`，脚本立即返回 **Exit Code 2**，在终端打印红色告警：“严禁擅自修改 pom.xml 引入外部依赖！”工具调用当场被物理切断；
- 更绝的是第 69 行：当我们在环境变量中开启 `LOCK_TESTS=1` 时，只要 Agent 试图修改 `src/test/` 目录下的任何测试文件，Hook 立即返回 **Exit Code 2** 并严厉警告：“测试已锁定！严禁篡改测试断言（Reward Hacking），请修复业务实现代码！”
这样，不管大模型推理能力多强、多想通过偷改期望值来骗取绿灯，在确定性的文件拦截面前，一切作弊手段统统失效！

## Key Q&A / Clarifications
**Q：在 Windows 开发机上没有 bash 环境，Hook 脚本如何跨平台执行？**
A：有两种主流解决方案：
1. **利用 Git for Windows 自带的 bash**：在 Windows 安装 Git 后，默认自带了 `bash.exe`，现代 CLI 工具在 Windows 上能直接调度 bash 脚本；
2. **编写 PowerShell 版本的 Hook 脚本**：可以将拦截逻辑等价写成 `.ps1` 脚本，例如通过 `$args` 获取 JSON 字符串并使用 `ConvertFrom-Json` 解析，其退出码 `$LASTEXITCODE = 2` 同样能实现操作系统级的确定性阻断。配套的《Demo演示步骤.md》中提供了双环境的配置指南。

**Q：配置好的 Hook 怎么在开发环境中生效？**
A：在 Cursor 或 VS Code 中，可以通过工作区插件或终端 Pre-commit 钩子实现类似拦截；而在 OpenCode 与 Antigravity 原生架构中，Hooks 是顶层配置项（配置在生命周期拦截器或工作区设置中），工具层在调度文件操作工具前会自动执行钩子命令，拦截发生在调用本地磁盘 API 之前。

## Transition
基础设施已经全部就绪！
接下来，就让我们以真正的两大战役——“PII 敏感数据脱敏新特性开发”与“祖传复杂文本解析器安全重构”，在靶场中展开真刀真枪的实弹对决！请看下一页。
