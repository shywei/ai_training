## Slide Objective
展示 ErrorRecovery 的实现代码，并建立与企业级错误恢复四层协同矩阵（Retry / Circuit Breaker / Replanning / Escalation）的精准映射，帮助学员读懂代码并明确生产扩展方向。

## Suggested Presentation
左边是 ErrorRecovery.java 的核心代码。三个常量定义了容错参数：MAX_RETRY = 3，BREAKER_THRESHOLD = 5，RECOVERY_MS = 60_000。
shouldRetry() 判断是否应该重试——比较 step.getRetryCount() 和 MAX_RETRY。getRetryDelayMs() 计算指数退避延迟——Math.pow(2, retryCount) * 1000。isCircuitBroken() 判断是否熔断——检查 failureCounters 中的失败次数。

右边展示这套代码在企业级四层协同矩阵中的位置：
1. **① 重试（Demo 落地）**：在 executeToolCall() 中，调用失败 → shouldRetry? → 指数退避等待 → 再次调用。
2. **② 熔断与降级（Demo 落地）**：executeToolCall() 入口处先查 isCircuitBroken? → 已熔断则直接跳过，返回 null 或降级提示，驱动 AgentRuntime 让 LLM 基于已有信息兜底回答。
3. **③ 错误重规划（生产扩展）**：当工具抛出明确的业务失败（非网络异常）时，Runtime 拦截并组装错误 Observation，重新激活 TaskPlanner。
4. **④ 人工接管（Demo 协同入口）**：重试耗尽时，状态机可转入 CONFIRMING 或告警工单，配合 ConfirmationService 进行人工介入。

代码阅读路线：先看 shouldRetry() → getRetryDelayMs() → isCircuitBroken()，理解每一级的触发条件和恢复策略。然后再看 executeToolCall() 中的调用逻辑。

## Key Emphasis
代码要讲"为什么这么写"，不只是"写了什么"。比如 ConcurrentHashMap 是线程安全的——因为多个请求可能同时调用同一个工具。

不要逐行讲解代码——挑关键逻辑讲，剩下的让学员自己读。

## Optional Extension
**学员问：failureCounters 会无限增长吗？**
答：不会无限涨——recordSuccess() 会清零，工具恢复后计数就清了；但一直失败会一直涨。**Demo 做法**：failureCounters 是进程内 ConcurrentHashMap，无 TTL，重启清零、多实例各算各的。**生产做法**：失败计数迁 Redis（key=toolName，TTL 5 分钟自动清零），多实例共享熔断状态，恢复时用半开探测（先放一个试探请求，成功再全部放行）。

**学员问：getRetryDelayMs() 的指数退避会不会太慢？**
答：1s、2s、4s 总共 7 秒，对用户来说是可以接受的。如果嫌慢，可以改成线性退避（1s、2s、3s）或者减小基数。但指数退避的好处是"越往后越宽容"——给故障服务更多恢复时间。

## Take Away
看容错代码先核对三个入口：shouldRetry、isCircuitBroken、返回 null 降级——三处都有明确触发条件，缺一处就不叫三级容错。

## Why This Slide Matters
跳过这页，学员会用一个大 try-catch "兜住所有异常"当容错——没有重试计数、没有熔断状态，下游被打垮时还在无限重试。

## Transition
容错解决的是"工具不可靠"的问题。不过在聊"操作不安全"之前，先把三级容错逐级拆开——重试、熔断、降级各自的触发条件、参数取值和失败模式，看下一页。