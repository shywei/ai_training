# 第 18 页：企业级增量（四）：Tool 强契约与自愈纠错循环

## Slide Objective
讲透如何用“事前 Bean Validation 强契约 + 事后 Schema Repair Loop 自愈拦截”两道防线解决大模型传参格式幻觉问题；展示 V6 (`SchemaRepairCallAdvisor`) 与 V7 (`SchemaRepairMiddleware`) 实现，并诚实划定 Demo 覆盖与 PPT 原理边界。

## Suggested Presentation
各位同学，来看企业级 Agent 必须有的第 4 个环节：**工具参数强契约与异常自愈**。

在实际生产中，大模型调 `createOrder` 工具时经常出现各种参数幻觉：商品 ID 写成小写的 `"lenovo-01"`（我们规定是 `NB001`）、购买数量填了个 `0`、手机号少了一位。在 V5 里一旦校验报错，整个工作流就直接抛异常降级了，用户体验突然中断。

在框架体系下，我们用**两道防线**协同解决这个问题：

**第一道防线（事前约束）：`ToolOrderRequest` + Jakarta Bean Validation（Demo 单测已覆盖）**。
看左栏代码：我们在领域契约类 `ToolOrderRequest` 的字段上加上 `@Pattern(regexp="^(NB|PH|ACC)[0-9]{3}$")`、`@Min(1)` 等标准注解。它有双重价值：一方面供框架自动反射推导进 JSON Schema 告诉模型“应该长什么样”；另一方面在工具入口通过 `validator.validate(req)` 把死大门，绝不让脏数据污染下游数据库！

**第二道防线（事后自愈）：Schema Repair Loop 自愈纠错循环（需要开发者基于扩展点编写）**。
很多同学会以为框架自带自动纠错——这里必须澄清：**框架原生只负责“校验失败抛异常”；捕获异常、拼接 Repair Prompt 并限制最多重试 2~3 次的逻辑，必须由开发者通过 Advisor 或 Middleware 扩展实现！**
看右栏两段代码：
- 在 V6 `SchemaRepairCallAdvisor` 里，捕获 `ConstraintViolationException` 后，把具体的违规字段报错拼成 `[系统自愈纠错提示]` 追加进上下文，在 `while (attempt <= 2)` 内驱动模型重新生成合法参数；
- 在 V7 `SchemaRepairMiddleware` 里，通过 `onActing` 链上的 `.onErrorResume(...)` 捕获校验异常并发射 `SchemaRepairTriggered` 自定义事件。

这里如实标注边界：`ToolOrderRequest` 校验拦截与 `CustomEvent` 触发已经在 Demo 与单元测试中完整覆盖；而完整多轮端到端大模型自主重填效果受具体模型智商影响，课堂以 PPT 原理解析为主。

## Key Emphasis
1. **两道防线分工**：事前 Bean Validation 推导 Schema 并守门；事后 Repair Loop 捕获违规并引导模型限次自愈。
2. **不神话框架**：明确指出“捕获异常拼 Repair Prompt”不是框架自动白送的，而是开发者基于 Advisor / Middleware 扩展点自己写的策略。
3. **Demo 边界诚实**：区分单测已覆盖的校验/事件发射与端到端多轮模型重填的演示边界。

## Optional Extension
### Q1: 为什么 `SchemaRepairCallAdvisor` 和 `SchemaRepairMiddleware` 都硬性限制了最大自愈次数（如 `MAX_REPAIR_ATTEMPTS = 2` 或 `3`）？
**讲师解析**：如果没有最大重试上限，当一个能力较弱的小模型始终无法理解复杂的正则规则（比如一直填错某个字段）时，自愈循环就会变成“报错 ➔ 重试 ➔ 再报错”的死循环，瞬间烧光 Token 预算并把请求拖垮。设定 2~3 次上限后，若仍无法自愈则果断熔断抛出异常走兜底降级，这是生产工程的保底红线。

## Classroom Interaction
“如果把自愈次数上限设为无限大，或者不写具体错在哪个字段，只跟模型说‘参数错了请重试’，模型能自己改对吗？”（不能！必须把 `ConstraintViolation` 里具体的字段错误详情喂给模型，模型才知道改哪里）。

## Take Away
用 Jakarta Bean Validation 筑牢事前强契约防线，再用 Advisor / Middleware 扩展点实现限次 Schema Repair 自愈循环。

## Why This Slide Matters
完成了第二部分四大企业级环节的最后一块拼图，同时破除“框架会自动替你修好所有畸变 JSON”的盲目幻想。

## Transition
第一部分我们看了同构效率提升，第二部分我们补齐了四大企业级环节。接下来进入第三部分，也是本课点题的核心——**框架的边界**！有哪些死穴和核心资产，哪怕用了最强的框架也绝对不能交出去？请看 P19。
