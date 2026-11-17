# 第 21 页：防框架套牢（一）：为什么要做六边形隔离？

## Slide Objective
通过 Spring AI 1.0 → 2.0 接口断代重构的真实教训，讲透“框架 API 入侵核心业务层”的套牢灾难，阐明六边形架构（Ports & Adapters）依赖倒置隔离 AI 框架的核心原理。

## Suggested Presentation
各位同学，在传统 Java 开发里，Spring Boot 升级很少让业务代码大改；但 AI 生态完全不同——迭代快到令人发指！

就拿我们亲眼见到的 Spring AI 来说：从 1.0-M4 到 2.0.2 GA，短短时间里连最核心的 `CallAroundAdvisor` 和 `AdvisedRequest` 都被官方彻底重构成了 `CallAdvisor` 和 `ChatClientRequest`！
请看左栏的**耦合反模式**：很多团队图快，直接在核心的 `OrderServiceImpl` 上面打 `@org.springframework.ai.tool.annotation.Tool`，方法参数直接接收 Spring AI 的 `Prompt` 和 `ToolContext`，返回值直接返回 `Message`。
后果是什么？一旦框架大版本断代升级，或者团队想从 Spring AI 切换到 AgentScope，你所有的核心业务 Service 连同单元测试全部编译报错，只能推倒重写！

怎么彻底治本？引入经典软件工程的**六边形架构（Ports & Adapters，见右栏）**：
1. **核心原则是依赖倒置（DIP）**：把整个系统切分为三圈，最核心的 **`② 领域核心与端口层（Domain & Ports）`** 保持 **100% 纯 Java POJO**——里面只有 `ShoppingDomainService`、`ToolOrderRequest`、`AgentCheckpoint` 以及抽象接口 `ShoppingAgentPort` / `CheckpointStorePort` / `EnterpriseToolPort`，**严禁导入任何 `org.springframework.ai.*` 或 `io.agentscope.*` 包**！
2. **所有 AI 框架全被关进外围的 `① 入站适配器（adapter/）`**：无论是 `ChatClient`、`@Tool`、`ReActAgent` 还是 `MiddlewareBase`，全部只允许出现在 `adapter/` 包里，去实现中心定义的抽象端口。

这样一来，核心业务不再依赖具体 AI 框架，反过来是 AI 框架适配器依赖业务定义的端口契约！

## Key Emphasis
1. **痛点真实**：以本次我们亲自验证的 Spring AI 接口更名重构作为鲜活教材。
2. **核心边界**：`domain/` 和 `port/` 对具体 AI 框架零依赖。

## Optional Extension
### Q1: 在六边形架构下，给大模型暴露工具的 `@Tool` 注解应该打在哪个类上？
**讲师解析**：必须打在专门的适配器类上（如 V6 的 `SpringAi2ToolAdapter` 或 V7 的 `ShoppingAgentTools`），由适配器去调用抽象的 `EnterpriseToolPort` 或纯领域服务 `ShoppingDomainService`。绝不能直接把 `@Tool` 打在领域服务类本身上。

## Classroom Interaction
“请问在座的同学，如果把 `ShoppingDomainService` 放到一个完全不引入 Spring AI 和 AgentScope 依赖的独立 Maven 模块里，它还能编译通过并且毫秒级跑完单元测试吗？”（可以！这就是纯 POJO 领域核的威力）。

## Take Away
AI 框架是易耗品，业务领域逻辑才是长期资产；用 Ports & Adapters 把框架锁死在边缘适配器层。

## Why This Slide Matters
从架构设计源头解决团队最头疼的“被单一 AI 框架或版本升级绑死”的风险。

## Transition
道理讲通了，这到底是不是纸上谈兵？我们直接拿 8083（V6）和 8084（V7）两套真实工程的源码目录来做实证对比！请看 P22。
