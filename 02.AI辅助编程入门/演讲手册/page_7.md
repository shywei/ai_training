## Slide Objective
淘汰 2023 年无效套话，确立面向 Coding Agent 的结构化提示词四要素（锚点、契约、负面红线、验证命令）。

## Suggested Presentation
我们来到第三章：面向 Coding Agent 的提示词工程。请看第 7 页。
很多人还在用 2023 年的老套路：“你是一个 Java 专家，请一步步思考，帮我写个用户注册功能……”
这些鸡汤套话在面对全自主 Coding Agent 时完全是浪费 Token，且毫无约束力！
现代面向 Coding Agent 的提示词，本质是**“可执行的工程任务单”**，必须包含【确定性四要素】：
1. **精确上下文锚点（Context Anchors）**：使用 `@` 语法显式指定关联文件（如 `@OrderService.java`、`@schema.sql`），不让 Agent 在全仓几十万行代码里盲猜；
2. **输入输出与边界契约（Contract & Signature）**：明确接口入参 DTO、返回值格式、异常抛出类型与数据库幂等要求；
3. **负面禁改边界（Negative Constraints / Guardrails）**：明确红线，比如“严禁修改公共表结构”、“禁止引入新第三方依赖”、“不可改动现有单测已有断言”；
4. **确定性验证命令（Verification Command）**：指定任务验收标准，如“修改完毕后必须执行 `mvn test -Dtest=OrderServiceTest`，确保全部单测通过后停止”。

## Key Q&A / Clarifications
**Q：为什么必须明确指定“验证命令”？**
A：因为没有验证命令，Agent 在自以为改好后就会停下来邀功；而明确要求跑测试，Agent 就会在遇到编译错误时自动闭环修复，直到跑通为止。

## Transition
我们通过一个真实企业 Java 业务场景的正反面对比，直观感受四要素的威力。
