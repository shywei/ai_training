# 第 22 页：防框架套牢（二）：Ports & Adapters 实战与零修改平替验证

## Slide Objective
以 8083（V6）与 8084（V7）两套真实工程源码为证，展示 `domain/` 与 `port/` 100% 共享、仅替换 `adapter/` 实现的零侵入平替效果（Demo 已覆盖），并补充介绍生产级 CI 架构守护工具 `ArchUnit`（仅 PPT 讲解）。

## Suggested Presentation
各位同学，请看屏幕上半部分的真实代码对比——左边是 V6 的 `SpringAi2WorkflowAdapter`，右边是 V7 的 `AgentScopeWorkflowAdapter`：
- 你会发现两个类**实现了完全相同的入站端口接口 `ShoppingAgentPort`**；
- 对于上层调用它们的 Web 控制器来说，它只注入 `ShoppingAgentPort`，完全不知道底层跑的是 Spring AI `ChatClient` 还是 AgentScope `ReActAgent`！

再看左下角的目录同构实证：如果你用 `diff` 工具去对比 `enterprise-ai-agent-springai2` 和 `enterprise-ai-agent-agentscope` 这两套工程的源码：
- **`domain/` 目录下 8 个纯 Java 类**：0 行修改！
- **`port/` 目录下 3 个端口接口**（`ShoppingAgentPort`、`CheckpointStorePort`、`EnterpriseToolPort`）：0 行修改！
- 唯一根据框架改变的，只有 `adapter/springai2/` 和 `adapter/agentscope/` 这一个边缘包！

最后看右下角一个非常实用的**生产进阶技巧（仅 PPT 讲解）**：在多人协作的大团队里，光靠口头约定“别在 `domain/` 里引框架包”很容易被新人破戒。生产上可以把这条边界写进 **`ArchUnit` 自动化架构测试**里，跑在 CI 流水线中——只要有人敢在 `..domain..` 里 `import org.springframework.ai..` 或 `io.agentscope..`，构建直接红灯打断！

## Key Emphasis
1. **实证直观**：两套工程 `domain/` 与 `port/` 源码 100% 一致，证明框架可插拔不是口号。
2. **诚实划界**：零改动平替已在两套 Demo 工程中落地验证；`ArchUnit` 静态规则测试作为团队规范扩展仅在 PPT 讲解。

## Optional Extension
### Q1: 既然用了六边形架构，那如果未来 Spring AI 和 AgentScope 之外又冒出了一个新框架（比如第 10 课的 Embabel），迁移成本大概是多少？
**讲师解析**：所有领域风控、选品打分、状态快照模型、Tool 契约 DTO 和入出站端口接口全部不用动（0 行改动）；Controller 也不用动；你只需要新建一个 `adapter/embabel/` 包实现 `ShoppingAgentPort`，再跑一遍现有的领域单元测试和接口回归测试即可。

## Classroom Interaction
“大家在课后可以直接打开 IDE 对比两个工程下的 `domain/` 和 `port/` 包，亲自验证是不是逐字一致；同时跑一遍 `mvn test` 看离线单测结果。”

## Take Away
把框架切换的影响范围牢牢锁死在 `adapter/` 层，保持 `domain/` 和 `port/` 0 行修改，并用 ArchUnit 在 CI 中自动化守护。

## Why This Slide Matters
把六边形架构从理论图变成可见、可验证的真实分包结构与代码，彻底闭环“防套牢”命题。

## Transition
掌握了同构提效、企业级能力增量、框架边界与防套牢架构后，最后在 P23，我们站到架构师全局视角，梳理清楚 2026 Java/AI 生态的三层坐标与最终选型决策树。
