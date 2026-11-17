# 第 05 页：同构提效（二）：JDK 21 虚拟线程与 Spring Boot 4 可观测底座

## Slide Objective
单独、完整地讲清 Spring AI 2.0 依赖 JDK 21 虚拟线程与 Spring Boot 4.0 可观测底座的原因、原理与真实配置/代码（虚拟线程 Demo 已覆盖；OTel 后端对接采集器为仅 PPT 讲解）。

## Suggested Presentation
各位同学，很多人在升级 Spring AI 2.0 时会问：“我就想用个 `ChatClient`，为什么官方非要我升级 JDK 21 和 Spring Boot 4.0 呢？”这一页我们用左右两栏真实配置与代码来回答这个问题。

**先看左栏：JDK 21 虚拟线程（Demo 已覆盖）**。
- **遇到什么问题？** 传统微服务查 MySQL 只要 3 毫秒，而调一次大模型 I/O 阻塞要等 2 到 10 秒。V5 跑在传统 Tomcat 平台线程池（默认 200 线程）上，并发一来线程全部卡死在网络等待上。
- **引入什么概念与原理？** JDK 21 的 **虚拟线程（JEP 444）**。JVM 在用户态做 M:N 轻量调度，当虚拟线程执行大模型 HTTP 请求发生阻塞时，JVM 自动把它挂起并释放底层 OS 载体线程去服务别的请求，单机即可用同步代码扛住数千并发等待。
- **在工程里怎么配、怎么用？** 请看代码框：在 `enterprise-ai-agent-springai2/src/main/resources/application.yml` 中开启 `spring.threads.virtual.enabled: true`，Tomcat 请求就自动跑在虚拟线程上；在 `SpringAi2WorkflowAdapter.java` 第 112 行做异步流式输出时，直接写 `Thread.ofVirtual().start(...)` 即可。

**再看右栏：Spring Boot 4.0 / Micrometer 可观测底座（配置已覆盖 · OTel 对接后端仅 PPT 讲解）**。
- **遇到什么问题？** 第 08 课手写的 `TraceService` 靠人工埋点，很容易漏统 Token 消耗或耗时。
- **原理与怎么配？** Spring Boot 4.0（Framework 7.0）整合 Micrometer `ObservationRegistry`，在 `ChatClient` 外层自动套上观测切面，按 OpenTelemetry GenAI 语义采集延迟和 Token 用量。看下方 `application.yml` 中的 `spring.ai.chat.observations` 配置——生产环境记得把 `log-prompt` 和 `log-completion` 设为 `false`，防止把用户隐私明文打进监控日志；如果要真正导出到 Jaeger，生产上再引入 `spring-boot-starter-opentelemetry` 并配 OTLP 端点即可（这部分后端对接仅在 PPT 讲解，本地 Demo 不依赖 Jaeger）。

## Key Emphasis
1. **文码 100% 对应**：左栏讲虚拟线程即配 `application.yml` + `Thread.ofVirtual()` 代码；右栏讲可观测即配 `spring.ai.chat.observations` + OTLP 扩展说明。
2. **诚实划界**：明确告知学员虚拟线程是 Demo 已覆盖，而对接外部 Jaeger/OTel 采集器为仅 PPT 讲解的架构知识。

## Optional Extension
### Q1: 用了虚拟线程之后，是不是就完全不需要 WebFlux 反应式编程了？
**讲师解析**：如果是常规的“发请求 ➔ 等模型返回 ➔ 存数据库”阻塞型业务，JDK 21 虚拟线程配合同步代码开发心智负担最低；但如果要做多路流合并、背压控制或实时多通道 SSE 推送（如 `Flux<WorkflowStreamEvent>`），Reactor 响应式流依然有不可替代的价值。

## Classroom Interaction
“大家想一想，在 `application.yml` 里把 `log-prompt` 设为 `true` 虽然调试很爽，为什么生产环境一定要关掉？”（防止手机号、订单隐私被直接采集进集中式日志平台）。

## Take Away
JDK 21 虚拟线程解决同步阻塞下的高并发等待，Boot 4 + Micrometer 让模型耗时与 Token 观测自动化，二者构成 V6 的现代运行时底座。

## Why This Slide Matters
把之前一笔带过、没有代码落地的两项升级彻底讲透并配齐配置代码，消除学员“只听概念不知怎么配”的困惑。

## Transition
看完了模型调用与运行底座，接下来 P06 看第 07 课最容易踩坑的地方：V6 是怎么用 `@Tool` 和 `ToolCallingAdvisor` 彻底干掉 V5 脆弱正则表达式的？
