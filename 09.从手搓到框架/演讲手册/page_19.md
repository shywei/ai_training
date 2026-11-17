# 第 19 页：框架边界一：复杂领域模型的反射死穴与扁平化 Tool DTO

## Slide Objective
揭示 `@Tool` 自动 JSON Schema 反射的三大技术死穴（多态接口与深层嵌套、泛型擦除与弱类型 Map、JPA 双向关联栈溢出），并确立面向大模型的扁平化 Tool DTO 三项工程准则（Demo 已覆盖 `ToolOrderRequest`）。

## Suggested Presentation
各位同学，进入第三部分《框架的边界》。今天刚学会 `@Tool` 自动生成 JSON Schema 时，很多后端同事图省事，直接把业务 Service 里现成的 `OrderEntity` 或 `PageQuery<T>` 塞进工具方法参数里——**在生产环境下，这会直接踩爆三大反射死穴**！

请看屏幕上方这三张红色警示卡：
1. **死穴 1 · 多态接口与深层嵌套截断**：如果你的入参对象里包含一个 `PaymentStrategy` 接口或抽象父类，框架反射只看得到接口定义，根本不知道运行期要传支付宝还是微信的哪个子类字段，导出的是空对象 `{}`！而且嵌套只要超过 4 层，大模型漏填字段的概率就会急剧飙升；
2. **死穴 2 · 泛型擦除与弱类型 `Map<String, Object>`**：Java 泛型在运行时会被擦除，当你把工具入参写成 `Map<String, Object>` 或 `PageQuery<?>` 时，框架推导出的是一个没有任何字段约束的空 `object`，大模型只能盲猜键名；
3. **死穴 3 · 数据库实体双向关联触发 `StackOverflowError`**：如果直接拿 JPA/Hibernate 实体（`UserEntity` 持有 `List<OrderEntity>`，而 `OrderEntity` 又反向持有 `UserEntity`）去生成 JSON Schema，框架递归反射解析时当场死循环栈溢出，服务都起不来！

怎么破？请看下方三项必须落地的工程准则：
- **① 物理隔离数据库实体**：严禁把 DB Entity 直接暴露给 `@Tool`，必须像我们的 `ToolOrderRequest.java` 一样专门建无循环引用的契约 DTO；
- **② 结构拍平与枚举展开**：把深层树拍平为 1~2 层基本类型字段；多态策略直接改成显式字符串枚举（如 `"ALIPAY" | "WECHAT"`）；
- **③ 适配层完成领域组装**：让大模型只负责填极简的扁平参数，再由 Java `Adapter` 层根据这些值去查库并组装内部复杂领域对象。

## Key Emphasis
1. **三大死穴均有真实代码反模式**：多态字段丢失、泛型擦除退化、双向引用启动即崩溃。
2. **核心工程心法**：把复杂度留在 Java 适配层，把极简确定性留给大模型。

## Optional Extension
### Q1: 既然 `ToolOrderRequest` 拍平了，如果企业原有的 ERP 接口确实需要 30 个嵌套字段怎么办？
**讲师解析**：绝不要把 30 个嵌套字段直接抛给大模型填。通常由大模型传入 3~5 个关键业务标识（如 `userId`、`productId`、`quantity`、`couponCode`），剩下的收货默认地址、发票抬头、税率等确定性信息，由 Java 适配层在调用 ERP 前从企业用户中心或数据库查询补全。

## Classroom Interaction
“大家可以回头检查自己项目里暴露给 AI 的工具接口：有没有直接把 `Map<String, Object>` 或者带 `parent/children` 递归引用的实体类丢给框架生成 Schema？”

## Take Away
框架的自动 Schema 反射不是万能的——隔离数据库实体、拍平结构、在适配层组装复杂领域模型，是开发者的必修防线。

## Why This Slide Matters
防止学员因为盲信框架自动反射而引发工具调用准确率暴跌甚至服务启动崩溃。

## Transition
搞定了工具入参的边界，还有两个更大的坑：风控规则能不能写进 Prompt？挂了 AgentScope `StateStore` 还要不要做三层记忆？请看 P20！
