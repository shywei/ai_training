# 《02B. AI辅助编程进阶（可选深潜）》OpenCode 企业级研发工作流演练指南

本指南供讲师在课堂现场演练或学员课后实操使用。本课演示**完全基于开源终端 Coding Agent —— OpenCode**，使用**独立的示例工程（`demo-opencode-enterprise`）**，演示企业级 SDD 规格驱动开发、祖传代码安全重构与防作弊 Hook 闭环。

> **重要定位说明**：
> 本课为专为**项目 Leader 和高级开发**深入学习的可选深潜实操指南，专注于攻坚 SDD 规格驱动、老代码重构与 Git Worktree 隔离研发流。
> 普通学员在学完第 02 课后已具备单兵核心能力，可跳过本课直接进入《第 03 课：AI Agent架构认知入门》。
> 所有演练均在独立的示例工程（`demo-opencode-enterprise`）中进行，专注于工具进阶使用与规范闭环。

---

## 一、演示环境准备（基于 OpenCode）

1. **终端工具确认**：
   ```bash
   # 全局安装开源终端 Coding Agent OpenCode
   npm install -g opencode-ai
   # 验证 OpenCode 就绪
   opencode --version
   ```

2. **初始化独立的企业演练工程**：
   ```bash
   # 在演练目录下创建独立的演示工程
   mkdir -p demo-opencode-enterprise
   cd demo-opencode-enterprise
   git init
   ```

---

## 二、核心实战演练（共 4 个关键步骤，约 18 分钟）

### 演示 1：配置分层 `AGENTS.md` 与 OpenCode 物理防篡改 Hook（4分钟）

**目标**：演示如何用 OpenCode 原生支持的 `AGENTS.md` 约束工程规范，并用 PreToolUse Hook 物理阻断 AI 篡改测试断言作弊（Reward Hacking）。

1. **配置根目录 `AGENTS.md`（高信噪比全局宪法）**：
   ```markdown
   # Enterprise Engineering Guidelines (OpenCode)
   
   ## 1. 定向编译与单测
   - 快速构建: `mvn test-compile -q`
   - 单测验证: `mvn test -Dtest={TestClassName}`
   
   ## 2. 核心红线 (Red Lines)
   - 严禁未经审批擅自修改 pom.xml 引入外部依赖。
   - 在业务实现阶段，严禁修改、删除或 @Disabled src/test/ 下已锁定的测试断言！
   - 敏感信息（手机号、身份证、密码）在日志中严禁明文打印。
   ```

2. **部署 PreToolUse 防篡改 Hook**：
   在 `.opencode/hooks/protect-tests.sh`（或工作区 hooks 配置）中部署物理拦截脚本：
   ```bash
   #!/usr/bin/env bash
   # 读取 OpenCode 准备执行的工具输入参数
   TOOL_INPUT=$(cat)
   FILE_PATH=$(echo "$TOOL_INPUT" | grep -oE '"file_path":\s*"[^"]+"' | cut -d'"' -f4)
   
   # 规则 1: 物理阻断修改 pom.xml
   if [[ "$FILE_PATH" == *"pom.xml"* ]]; then
       echo "❌ [HOOK BLOCKED] OpenCode 严禁擅自修改 pom.xml 引入第三方依赖！" >&2
       exit 2
   fi
   
   # 规则 2: 当锁定测试时，阻断修改单测断言
   if [[ "${LOCK_TESTS:-0}" == "1" && "$FILE_PATH" == *"src/test/"* ]]; then
       echo "❌ [ANTI-CHEAT BLOCKED] 单测已锁定！请修复业务实现，严禁篡改测试断言 (Reward Hacking)！" >&2
       exit 2
   fi
   exit 0
   ```
   > **注**：在 OpenCode 中，工具执行前的物理拦截可通过工作区插件配置或生命周期拦截器实现，此处以跨平台的标准输入解析脚本展示核心判定逻辑。

3. **现场作弊拦截验证**：
   在终端启动 OpenCode，下达故意诱导指令：“帮我引入最新版 Hutool 依赖”，观察 Hook 瞬间弹出红色拦截告警并强制退出，逼迫 OpenCode 转向手写合法实现。

---

### 演示 2：OpenCode SDD 规格驱动开发闭环（PII 敏感数据脱敏）（5分钟）

**目标**：演示对标 AWS Kiro 的 `requirements.md (EARS)` → `design.md` → `tasks.md` 完整闭环。

1. **Step 1: 只读生成 Spec 工件**：
   在 OpenCode 中执行：
   ```text
   /plan 请在 .specs/pii-masking/ 目录下为当前项目新增一个敏感数据脱敏工具类（PiiMaskUtils），先输出 requirements.md (EARS语法)、design.md、tasks.md
   ```
   - 观察 OpenCode 输出清晰的三件套：
     - `requirements.md`：定义 11 位手机号脱敏为 `138****5678`、18 位身份证中间 8 位掩码、防误伤 SKU 单号；
     - `design.md`：声明预编译正则表达式与零内存拷贝签名；
     - `tasks.md`：按先写单测、再写实现的原子任务切片。

2. **Step 2: 人类审核批准与编码执行**：
   人类确认 Spec 契约无误后，指示 OpenCode：
   ```text
   Spec 已确认通过，请严格按照 tasks.md 推进编码，并在终端执行 mvn test 验证！
   ```
   OpenCode 严格按照验收标准一次性交付全绿单测。

---

### 演示 3：OpenCode 存量代码安全重构（Characterization Testing 黄金特征测试）（5分钟）

**目标**：演示如何接手无文档、圈复杂度极高的老代码，先用黄金特征测试锁死现状，再小步重构。

1. **准备一段含糊有怪癖的祖传解析代码**：
   以包含转义引号、嵌套括号的复杂字符串解析器 `LegacyParser.java` 为例。
2. **命令 OpenCode 反向提取黄金特征测试**：
   ```text
   请针对 LegacyParser.parseText(String) 现有逻辑，提取 8 组刁钻边界用例（含嵌套、未闭合括号、特殊转义字符），生成 LegacyParserCharacterizationTest.java。锁死现状行为！
   ```
3. **开启物理锁定并触发重构**：
   在终端执行 `export LOCK_TESTS=1`，命令 OpenCode：
   ```text
   现在将 LegacyParser 拆解为单一职责的 3 个纯函数，降低圈复杂度，严禁修改测试文件！
   ```
   OpenCode 在单测严格守门下完成重构，所有旧行为零回归偏差。

---

### 演示 4：OpenCode 无头自动化与 `git worktree` 并行工作流（4分钟）

**目标**：演示告别单线程干等，利用 `git worktree` 物理隔离实现多任务并行与 CI/CD 自动化。

1. **创建并行隔离分支**：
   ```bash
   # 为新特性开辟一个物理隔离的工作目录
   git worktree add ../feature-pii-masking -b feat/pii-masking
   cd ../feature-pii-masking
   ```
2. **执行 OpenCode Headless 无头自动化任务**：
   ```bash
   # 非交互式在后台直接跑任务并输出结果
   opencode run "为 StringUtils 补齐边界单测并确保 mvn test 100% 通过"
   ```
3. **主目录不受任何干扰**：
   切回原目录 `demo-opencode-enterprise`，展示两个目录互不冲突、多 Agent 并行推进的高阶工程效率。

---

## 三、现场讲师核心总结口诀

向学员再次强化本课核心心智：
- **规范驱动**：用 Spec 契约和分层规则规范 Agent，而非靠人肉反复纠偏；
- **测试锁死**：存量重构先上黄金特征测试，开启物理 Hook 防篡改；
- **工程提效**：善用 `git worktree` 与无头任务，构建一人指挥多个 Agent 的高确定性研发流！
