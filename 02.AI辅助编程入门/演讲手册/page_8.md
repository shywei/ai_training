## Slide Objective
通过 Spring Boot 退款功能真实用例的正反面对比，生动展示结构化提示词如何直接消灭 Bug 和反复拉扯。

## Suggested Presentation
请大家看第 8 页：编程提示词实战正反面对比。
这是一个电商退款接口的真实开发任务。
左边是【反面示例：模糊对话流】：
指令只有一句话：“帮我给 OrderController 加一个退款接口，退款成功后发邮件通知用户。”
结果 Agent 自由发挥：把数据库事务注在 Controller 层、手写了原生 JDBC 查库、随便引入了一个未审批的第三方邮件库，最可怕的是把退款状态从 `REFUNDED` 误写成了 `REFUND_SUCCESS`，直接把下游支付履约系统搞崩了！
右边是【正面示例：四要素结构化指令】：
- **锚点**：明确关联 `@OrderController.java`、`@RefundService.java`、`@OrderStatusEnum.java`；
- **契约**：要求入参使用 `RefundRequestDTO`，增加 `@Valid` 校验；状态流转严格遵循现有枚举；
- **负面红线**：严禁在 Controller 编写业务逻辑，禁止修改已有的 `payOrder` 逻辑；
- **验证**：执行 `mvn test -Dtest=RefundServiceTest` 并校验响应状态码。
结构化指令一次成型，零歧义，零返工！

## Key Q&A / Clarifications
**Q：每次提问都要手写这么多字吗？会不会太麻烦？**
A：完全不用！像“禁止改公共依赖”、“Controller 不写业务”这些通用红线，我们只需写在一次性的项目级 `AGENTS.md` 或 `Rules` 文件里，模型每次都会自动加载！这就是接下来第四章要讲的 Harness 体系。

## Transition
现在，我们进入本课最硬核、含金量最高的第四章：Harness 驾驭层“六件套”详解！
