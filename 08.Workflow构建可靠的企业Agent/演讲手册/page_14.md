## Slide Objective
展示 ErrorRecovery 的实现——三级容错在代码中的体现，帮助学员看懂代码。

## Suggested Presentation
左边是 ErrorRecovery.java 的核心代码。三个常量定义了容错参数：MAX_RETRY = 3，BREAKER_THRESHOLD = 5，RECOVERY_MS = 60_000。这些参数不是拍脑袋——上一页讲了为什么这么选。

shouldRetry() 判断是否应该重试——比较 step.getRetryCount() 和 MAX_RETRY。getRetryDelayMs() 计算指数退避延迟——Math.pow(2, retryCount) * 1000。isCircuitBroken() 判断是否熔断——检查 failureCounters 中的失败次数。

failureCounters 是一个 ConcurrentHashMap<String, ToolFailureInfo>，key 是工具名，value 是失败信息（次数、最近失败时间、错误消息）。recordSuccess() 清零，recordFailure() 递增。

右边展示三级容错在代码中的体现。重试：executeToolCall() 中，调用失败 → shouldRetry? → waitBeforeRetry → 重试。这是一个 while(true) 循环——成功就 break，失败就检查是否应该重试。熔断：executeToolCall() 入口检查 isCircuitBroken? → 跳过调用，返回降级消息。降级：返回 null → AgentRuntime 让 LLM 基于已有信息回答。

代码阅读路线：先看 shouldRetry() → getRetryDelayMs() → isCircuitBroken()，理解每一级的触发条件和恢复策略。然后再看 executeToolCall() 中的调用逻辑。

## Key Emphasis
代码要讲"为什么这么写"，不只是"写了什么"。比如 ConcurrentHashMap 是线程安全的——因为多个请求可能同时调用同一个工具。

不要逐行讲解代码——挑关键逻辑讲，剩下的让学员自己读。

## Optional Extension
**学员问：failureCounters 会无限增长吗？**
答：不会——recordSuccess() 会清零，工具恢复后计数器就清了。但如果工具一直失败，计数器会一直增长。生产环境应该加 TTL——失败计数 5 分钟后自动清零。

**学员问：getRetryDelayMs() 的指数退避会不会太慢？**
答：1s、2s、4s 总共 7 秒，对用户来说是可以接受的。如果嫌慢，可以改成线性退避（1s、2s、3s）或者减小基数。但指数退避的好处是"越往后越宽容"——给故障服务更多恢复时间。

## Take Away
三级容错在代码中就是三个判断：shouldRetry、isCircuitBroken、返回 null 降级。逻辑简单，但价值巨大。

## Why This Slide Matters
如果跳过这页，学员只知道"有三级容错"，但不知道"怎么实现的"。代码是教学的核心——学员要能看懂、能改、能扩展。

## Transition
容错解决的是"工具不可靠"的问题。还有一个问题——"操作不安全"。