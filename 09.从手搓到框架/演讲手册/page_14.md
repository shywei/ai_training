# 第 14 页：Checkpointer 能力落地：断点复水 Rehydration 与时光倒流 Forking

## Slide Objective
讲透 Checkpointer 带来的两项核心生产能力——跨进程断点复水（Rehydration）与时光倒流分支（Time-Travel Forking），展示真实 API 与代码调用，并严格划定与外部事务补偿（Saga）的边界。

## Suggested Presentation
各位同学，有了 `AgentCheckpoint` 快照存储之后，企业级 Agent 就解锁了两项像 Git 一样强大的能力。

**先看左栏：1. 跨进程断点复水（Rehydration，Demo 已覆盖）**。
- **解决什么问题？** 比如主管第二天批准了订单，或者任务跑到一半 Pod 崩溃重启了。此时任意一台新启动的 Pod 实例收到 `POST /rehydrate` 请求（传入 `executionId` 和 `checkpointId`）。
- **怎么工作的？** 请看代码：`rehydrateFromCheckpoint()` 通过 `checkpointStore.loadCheckpoint(execId, cpId)` 从存储中把快照反序列化出来，还原出 `stepCursor` 和 `callStack`，然后**直接从挂起的游标位置继续向下推演**，前面已经查过的商品价格、库存结果直接从 `scratchpad` 读取，**绝不重跑前置步骤**，彻底避免了“重启重跑导致重复下单扣款”的灾难。

**再看右栏：2. 时光倒流分支（Time-Travel Forking，Demo 已覆盖）**。
- **解决什么问题？** 假设 Agent 在 `chk-1` 查出了拯救者 Y9000P，到了 `chk-2` 准备下单 ¥8999 时用户反悔了：“太重了，帮我换成苹果轻薄本吧！”或者线上排查某次故障想从第 2 步换个提示词重跑对比。
- **怎么工作的？** 请看中间的因果版本树和下方 `forkFromCheckpoint()` 代码：系统调用 `POST /fork` 时，**绝不覆盖或删除原有的 `chk-2`**（保留完整审计证据），而是以 `chk-1` 为 `parentCheckpointId`，克隆它的 `scratchpad` 并生成一条全新的 `newExecutionId` 分支继续往前跑！

最后一定要提醒右下角的**重要生产边界（仅 PPT 讲解）**：Time-Travel Forking 重置的是 Agent 内存和数据库里的推演状态；如果 `chk-2` 已经向外部系统发出了不可逆的物理动作（比如调用航司接口锁了机票、或调库存系统预扣了库存），单纯分叉数据库快照是不够的——**必须联动分布式 Saga 逆向补偿机制**先释放外部资源再开新分支（这部分我们会在第 11 课工程交付中深入落地）。

## Key Emphasis
1. **Rehydration 核心价值**：跨进程原位续跑 + 跳过已完成步骤。
2. **Forking 核心价值**：Append-Only 不可变因果树，既能换方案重试，又不破坏审计历史。
3. **诚实划界**：复水与分叉接口在 Demo 中已完整实现（且有单测覆盖）；与外部物理副作用联动的 Saga 补偿为仅 PPT 讲解警示。

## Optional Extension
### Q1: 在分布式系统中，如果用户连续点两次“同意审批”调 `/confirm` 或 `/rehydrate`，怎么防止并发复水造成重复执行？
**讲师解析**：必须在复水与确认入口处结合分布式锁或状态机乐观锁（CAS 更新）：只有当数据库中当前 Checkpoint 处于 `CONFIRMING` / `EXECUTING` 非终态时才允许原子抢占并推进游标，一旦完成立即将状态置为 `COMPLETED` 并校验幂等键 `idempotencyKey`。

## Classroom Interaction
“如果线上有个 5 步的采购 Agent 在第 4 步报错了，排障人员用 `/fork` 从第 3 步分叉复现和调整，原本出错的第 4 步日志还在不在？”（在！Append-Only 因果树完整保留原分支轨迹）。

## Take Away
Rehydration 让长任务具备“跨进程断点续跑”的韧性，Forking 让决策链路具备“可审计的分支重试”能力，但外部物理副作用须配合 Saga 补偿。

## Why This Slide Matters
让学员直观看到 Checkpointer 落地后的 HTTP 接口、代码实现与真实生产边界，把抽象概念变成可直接调用的工程能力。

## Transition
解决了“状态存得住、断了能续跑”的问题，接下来看第二个 V5 极难手搓的环节（P15）：当模型决定调一个高危工具、但还没真正执行的那一瞬间，框架怎么精准把它拦住并无损挂起？
