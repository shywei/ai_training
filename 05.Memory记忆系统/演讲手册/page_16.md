# Page 16 — Demo vs 生产 — 记忆系统的五项差距

## Slide Objective
把本课 Demo（`v1.2.1`）与生产级记忆系统的五项工程差距一次讲到三格粒度（**问题（Demo 现状）/ 不解决会怎样 / 怎么解决 + 取舍**）：Redis 集群与持久化、Session TTL 与淘汰、异步抽取可靠性与失败重试、隐私合规（用户可遗忘权）、跨实例记忆并发与共享。对应 Demo 的 `SessionMemory.java`、`SummaryPipeline.java`、`MemoryManager.java`、`UserProfileExtractor.java`。

## Suggested Presentation
逐项讲，每项都精准对照本课 `v1.2.1` Demo 的真实代码现状，告诉学员生产上该怎么补齐：

**1 · Redis 集群与持久化**
- **问题（Demo 现状）**：本地或单容器单实例 Redis，无主从/Cluster、无 AOF/RDB 持久化配置；`SessionMemory` 的近期消息（`session:{sid}:messages`）与 `SummaryPipeline` 的累积摘要（`summary`）全部存在单点内存中。
- **不解决会怎样**：Redis 一旦宕机或重启，所有活跃会话的近期对话与累积摘要瞬间清零，Agent 对当前正在进行的对话直接失忆；单实例内存成為并发会话容量的硬上限。
- **怎么解决**：生产使用 Redis Cluster 或云原生主从+哨兵自动切换；开启 `AOF everysec` 将故障丢失窗口控制在 1 秒内；同一会话的多 Key 操作使用 Hash Tag `{sid}` 保证落在同一分片。

**2 · Session TTL 与动态触发淘汰**
- **问题（Demo 现状）**：`SessionMemory.addMessage` 只有 24h `EXPIRE`（每次写入刷新 → 活跃会话永不过期）+ 按固定条数滑窗（`MAX = 20`）与满 8 条触发 `SummaryPipeline` 压缩；**明确指出：这种固定条数/轮数切分属于教学演示简化（Educational Simplification）**；Redis 缺乏 `maxmemory` 淘汰策略，且不按 Token 预算动态裁剪。
- **不解决会怎样**：高并发下内存增长触顶导致 OOM 或无差别淘汰活跃会话；"条数 ≠ Token 数"，8 条超长报文照样把上下文窗口撑爆。
- **怎么解决**：配置 `maxmemory` + `volatile-lru`；设置"空闲 TTL + 绝对最大生命周期 TTL"双上限；**生产实践中必须摒弃固定条数，改由 Token 预算阈值（例如占用窗口达到 65%~75%）或任务语义完整性边界（识别用户一个完整话题收尾）动态触发压缩沉淀**。

**3 · 异步抽取可靠性与失败重试**
- **问题（Demo 现状）**：本课 Demo 中 `UserProfileExtractor` 虽然支持 `@Async` 异步抽取（或通过 `/profile/extract` 触发），但仅依赖 JVM 内存线程池执行；且 `extract()` 中一旦遇到网络超时或 LLM 返回非法 JSON，只打印日志并返回 `null`，**无重试机制、无死信队列（DLQ）**。
- **不解决会怎样**：应用重启时内存队列中尚未执行的抽取任务直接丢失；偶发解析失败导致用户画像静默缺失——用户以为偏好被记住了，下次来发现没记住，极难排查。
- **怎么解决**：用持久化消息队列（Redis Stream / RabbitMQ / Kafka）或本地事务任务表替代纯内存 `@Async`；对超时或 JSON 格式错误做 2–3 次指数退避重试（或触发 Self-Correction），仍失败则落入死信队列（DLQ）告警并支持定时重放；按 `(userId + sessionId + messageOffset)` 做幂等防重。

**4 · 隐私合规（用户可遗忘权）**
- **问题（Demo 现状）**：`/api/agent/memory/clear` 接口只清理了 Redis 中的当前会话；MySQL `user_profile` 表以及 `logs/` 目录下的日志文件没有任何级联删除接口；入库前也没有做 PII 脱敏。
- **不解决会怎样**：无法履行 GDPR /《个人信息保护法》要求的"被遗忘权/删除权"——用户要求注销或删除记忆，MySQL 画像与日志原文依然保留；手机号、身份证等敏感 PII 明文入库，合规风险极高。
- **怎么解决**：提供按 `userId` 的级联删除接口（同时清理 MySQL `user_profile` + Redis 会话 + 后续向量库）并留存删除审计记录；支持 30 天宽限期逻辑删后物理清理；在写入 MySQL 和发往 LLM 前加装 PII 正则/NER 脱敏过滤器。

**5 · 跨实例记忆并发与共享**
- **问题（Demo 现状）**：代码隐含单实例假设——`SummaryPipeline`（满 8 条读出、压缩、清空重写 Redis）和 `UserProfileExtractor`（读 MySQL 旧画像 → LLM 合并 → `updateById` 写回）均无分布式锁和乐观锁版本控制。
- **不解决会怎样**：多实例水平扩展后，同一用户并发请求会导致 `SummaryPipeline` 重写 Redis 时覆盖刚写入的新消息；画像合并出现经典的"读-改-写"并发覆盖（Lost Update），丢失画像更新。
- **怎么解决**：按 `(userId, sessionId)` 加 Redis 分布式锁（SETNX + TTL）或按 sessionId 哈希分区队列串行处理；MySQL `user_profile` 表增加乐观锁（基于 `version` 或 `updated_at` CAS 更新），冲突时重读重合并。

## Key Emphasis
五项差距精准对应 `v1.2.1` Demo 的真实代码：单点 Redis、条数滑窗、内存 `@Async` 无重试无 DLQ、无级联删除、无分布式锁与乐观锁。生产三件套 = **高可用存储 + 可靠异步队列(含重试/DLQ) + 级联删除与并发锁闭环**。

## Optional Extension
**学员问：这五项差距如果团队人力有限，先补哪几项？**
答：第一优先级补 **4（PII 脱敏与级联删除闭环）**——合规红线事后无法补救；第二优先级补 **1 & 2（Redis 高可用与 Token 预算淘汰）**；第三优先级补 **3 & 5（可靠异步重试与乐观锁）**。

## Classroom Interaction
让学员对照这五项差距给自己正在做的项目打分（已具备 / 半成品 / 完全缺失），写下最紧迫要补的一项作为课后行动项。

## Take Away
Demo 解决"从 0 到 1 跑通记忆机制"，生产工程解决"不丢数据、不卡请求、并发不乱、删得干净"。

## Why This Slide Matters
把 Demo 原封不动搬上生产的团队，一定会在高并发或合规审计时踩中这五颗雷——提前把三格解决方案给齐，学员回公司就能直接作为架构评审清单。

## Transition
五项生产差距拆解完毕，接下来我们进入第九章（Page 17：Demo 架构 — 组件与日志），看看本课 `v1.2.1` 代码按什么顺序读、哪 6 个核心验证点可以亲手跑通。
