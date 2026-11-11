# 《02. AI辅助编程入门》OpenCode 工具现场演示指南

本指南供讲师在课堂现场演练或学员课后实操使用。本课演示**纯粹聚焦于 AI 编程工具 OpenCode 的安装、配置与单兵交互技巧**，采用**独立的演练示例工程**，专注于工具链基础使用与规范闭环。

---

## 一、演示环境准备（基于 OpenCode）

1. **安装 OpenCode 终端工具**：
   ```bash
   # 全局安装开源终端 Coding Agent: OpenCode
   npm install -g opencode-ai
   # 校验安装
   opencode --version
   ```

2. **创建独立的演练工作区**（与后端实战项目完全隔离）：
   ```bash
   # 在临时或演练目录下创建一个独立的演示项目（以简单 Spring Boot / Java 工具服务为例）
   mkdir -p demo-opencode-lab
   cd demo-opencode-lab
   git init
   ```

---

## 二、核心演示环节（共 4 个关键步骤，约 12 分钟）

### 演示 1：OpenCode 快速启动与 `/init` 自动初始化（3分钟）

**目标**：向学员展示如何用 OpenCode 30 秒上手一个新项目，并自动生成项目级规范。

1. **启动 OpenCode 交互终端**：
   ```bash
   opencode
   ```
2. **执行 `/init` 自动化扫描**：
   在 OpenCode 提示符下输入：
   ```text
   /init
   ```
   - 观察 OpenCode 自动检测当前技术栈与文件树，并在项目根目录生成标准规范文件 `AGENTS.md`。

---

### 演示 2：配置 `AGENTS.md` 规则与 `/cost` 透明透视（3分钟）

**目标**：演示项目级规则约束生效机制以及 Token 消耗透视。

1. **在 `AGENTS.md` 中写入团队基本纪律**：
   ```markdown
   # Project Guidelines
   
   - 所有生成的 Java 类必须使用 UTF-8 编码，包名统一为 com.example.demo。
   - 严禁引入任何未经核准的第三方工具包。
   - 实现业务逻辑前必须包含对应功能的单元测试用例。
   ```

2. **透视 Token 与缓存状态**：
   在会话中敲击：
   ```text
   /cost
   ```
   向学员展示当前已使用的输入/输出 Token 数量，以及前缀缓存（Prefix Cache）的命中情况，印证 PPT 中的 Token 经济学原理。

---

### 演示 3：OpenCode 结构化提示词与单测闭环实操（3分钟）

**目标**：演示“锚点 + 契约 + 负面红线 + 验证命令”四要素在独立小型需求中的执行。

1. **下达结构化指令**：
   在 OpenCode 中输入：
   ```text
   请在 src/main/java/com/example/demo/ 中创建一个 StringUtils 工具类：
   1. [契约] 实现 isNullOrBlank(String str) 与 maskPhone(String phone) 手机号脱敏方法；
   2. [负面红线] 严禁依赖任何第三方类库（如 commons-lang 或 hutool），纯手写实现；
   3. [验证] 在 src/test/ 中创建对应的单元测试，并执行 mvn test 确保所有断言通过！
   ```

2. **观察 OpenCode 自动闭环**：
   - OpenCode 自动规划并编写代码与单测试卷；
   - 自动在终端调度执行 `mvn test`；
   - 遇到编译或测试报错自动反思自愈，最终交付全绿结果。

---

### 演示 4：OpenCode `/plan` 规划模式与 Git 秒级止损（3分钟）

**目标**：演示 Plan Mode 先行原则以及会话断舍离。

1. **切换 Plan Mode（只读规划模式）**：
   敲击 `/plan` 命令：
   ```text
   /plan 请评估为当前项目新增一个 JWT 令牌解析模块的架构方案与改动清单
   ```
   观察 OpenCode 仅输出只读分析步骤与 Task List，不擅自修改任何文件。

2. **会话瘦身与清空**：
   - 敲击 `/compact`：演示如何将长篇历史会话无损压缩为精炼记忆摘要；
   - 敲击 `/clear`：演示在一个任务交付后，清空历史污染、开启下一个干净 Session 的标准操作。
3. **Git 快速止损**：
   展示在终端中执行 `git reset --hard HEAD`，向学员印证“Git 快照是面对 AI 走偏时最强的一秒止损武器”。

---

## 三、现场讲师特别提示

- **演练边界说明**：
  必须向学员强调：本演示纯粹用于学习 **OpenCode 终端工具的操作手势与 Harness 规范配置**，在独立的示例工程中进行，帮助工程师建立清晰、高确定性的 AI 原生单兵研发心智。
