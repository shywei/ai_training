# 第 12 页：企业级增量（一）：为什么 ChatMemory 救不了跨天审批与 Pod 重启？

## Slide Objective
正式切入第二部分（5.2 企业级增量能力），通过真实生产事故场景揭示 V5 内存态的致命局限，破除“`ChatMemory` 存聊天记录 = 断点持久化”的常见误区，清晰定义 **Chat History / Agent State / Execution Checkpointer** 三级存储边界。

## Suggested Presentation
各位同学，欢迎进入第二部分——**企业级 Agent 应当具备、但 V5 手搓极难实现的四大增量环节**！我们来看第一个环节：**Execution Checkpointer（执行栈快照）**。

先看一个极其真实的生产场景：
用户让导购 Agent 买一台 ¥8999 的拯救者笔记本。Agent 已经查完了商品参数、锁好了优惠券，现在走到了第 3 步“创建高额订单”，触发了人工审批挂起——财务主管可能明天早上才点“同意”。更常见的是，就在等待审批的这几个小时里，后端 K8s 发版滚动重启，把原来处理请求的那个 Pod 销毁了！

在 V5 手搓工程里会发生什么？`TraceService` 和 `ConfirmationService` 全靠单机 `ConcurrentHashMap` 存内存，Pod 一销毁，任务直接人间蒸发！
此时很多同学会说：“那我用 Spring AI 的 `ChatMemory` 把聊天记录存进 Redis 不行吗？”

**千万注意：把聊天记录存进数据库，绝对救不了断点续跑！** 请看屏幕中间的三级状态边界划分：
1. **① Chat History（对话历史）**：存的是“我要买电脑”“好的，已为您查询”这类自然语言消息列表，唯一职责是下次请求大模型时拼 Prompt 上下文。光靠几句聊天文本，程序复原不出代码刚才跑到第几步 `stepCursor`、工具返回的商品库存 Map 在哪；
2. **② Agent State（会话业务变量）**：存的是用户偏好品牌、预算上限这类跨轮次低频全局事实，依然不包含单次任务的执行堆栈；
3. **③ Execution Checkpointer（执行栈快照）**：这才是企业级运行时真正需要的“游戏存档”！它持久化保存机器执行现场——当前游标 `stepCursor`、调用栈 `callStack`、存放中间工具结果的草稿本 `scratchpad`、以及待审批表单 `pendingConfirmation`。

看下方的框架对比：Spring AI 2.0 原生只提供第一级 `ChatMemory`，做 Checkpointer 还得自己手搓 150 行适配器；而 AgentScope 2.0 原生提供了 `AgentStateStore` + `AgentState` 状态持久化抽象，天然支撑状态保存与恢复。

## Key Emphasis
1. **核心误区纠偏**：`ChatMemory`（自然语言聊天记录） $\neq$ `Execution Checkpointer`（程序执行现场快照）。
2. **三级隔离清晰**：Chat History 管模型上下文，Agent State 管业务属性，Checkpointer 管断点续跑。

## Optional Extension
### Q1: 在我们的 `enterprise-ai-agent-springai2` 工程中不是也有 `RedisPostgresCheckpointAdapter` 吗？那是不是说明 Spring AI 2.0 也支持 Checkpointer？
**讲师解析**：请注意区分“框架原生提供”和“课程在 V6 里为了对照而手搓补齐”。`RedisPostgresCheckpointAdapter`（229 行）恰恰是我们在 Spring AI 2.0 之上**自己手搓**出来的端口实现，Spring AI 2.0 官方本身并没有 `Checkpointer` 原语；而在 AgentScope 2.0 中，框架原生就提供了 `AgentStateStore` 体系。

## Classroom Interaction
“如果你的 Agent 运行在 3 个副本的 K8s 集群里，第 1 次请求打到 Pod-A 挂起审批，次日审批回调打到 Pod-B，没有 Checkpointer 会发生什么？”（Pod-B 内存里什么都没有，直接报错任务不存在）。

## Take Away
Chat History 只服务于模型上下文；只有把 `stepCursor`、`callStack` 和 `scratchpad` 独立持久化为 Checkpointer，Agent 才真正具备抗重启和跨天挂起能力。

## Why This Slide Matters
为后续讲解快照模型、断点复水和时光倒流打下最坚实的认知底座，让学员明白为什么企业级 Agent 必须引入 Checkpointer。

## Transition
既然知道了 Checkpointer 存什么、为什么重要，那它的数据结构怎么设计、高频写入和快速恢复怎么兼顾、在 AgentScope 里又具体对应哪一块？请看 P13。
