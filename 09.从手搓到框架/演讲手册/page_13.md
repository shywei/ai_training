# 第 13 页：Checkpointer 原理与实现：数据模型、存储轨道与混合快照

## Slide Objective
深入讲透 Checkpointer 的数据模型设计（`AgentCheckpoint`）、Full Snapshot vs State Delta 读写权衡、热冷双轨存储架构，以及在 AgentScope 2.0 中的桥接实现与调用方式（如实标注 Demo 已覆盖与仅 PPT 讲解边界）。

## Suggested Presentation
各位同学，明确了 Checkpointer 的职责后，我们来看看它的底层原理和框架落地。

请看左栏的 **1. 纯数据快照模型 `AgentCheckpoint.java`**：
一个可跨进程恢复的快照必须包含四组关键字段：
- **血缘坐标**：`executionId`（单次任务链路 ID）、`checkpointId`（快照版本 ID）、`parentCheckpointId`（指向父快照，串联成因果版本树）；
- **状态机位置**：`state`（如 `EXECUTING / CONFIRMING`）与 `stepCursor`（当前子步骤游标）；
- **机器运行时草稿区**：`callStack` 和 `scratchpad`（专门用来放已查到的商品 JSON 等中间结果——**物理隔离不塞进聊天记录，避免撑爆 Prompt Token 窗口**）；
- **审批挂起信息**：`pendingConfirmation`。
这里有一条**序列化铁律**：快照里**严禁**塞入 `HttpClient`、数据库连接或线程闭包，只能是纯数据 DTO，所有客户端连接在恢复时由 Spring 容器重新注入。

接下来看 **2. 快照捕获策略的工程权衡**：
- 如果每一步都存**全量快照（Full Snapshot）**，恢复时只需查一次（读 $O(1)$），但长链路写放大严重；
- 如果每一步只记**增量差分（State Delta）**，写入极轻（写 $O(\Delta)$），但恢复时必须沿 `parentCheckpointId` 往回一路回放合并（读 $O(N)$）。
- 所以生产级常用**混合策略**：在进入 `CONFIRMING` 挂起或任务完成等关键跃迁点落全量快照，中间常规步骤只记增量。（注意：在我们的 Demo 里，全量快照、Delta 接口与 Redis+PG 落库已经覆盖，但超长步数的全自动增量回放合并引擎属于**仅 PPT 讲解**的架构知识。）

最后看右栏：**在 AgentScope 里对应哪一块、怎么对接？**
在 `AgentScopeConfig` 中，我们通过 `.stateStore(checkpointStore.getAgentScopeStateStore())` 把 `AgentScopeCheckpointStore` 挂载进 `ReActAgent`。它一方面实现了课程统一的 `CheckpointStorePort` 端口，另一方面在 `saveSnapshot` 时同步把 `curIter` 等状态回写给 AgentScope 原生的 `AgentStateStore`，再配合热轨道 Redis + 冷轨道 PostgreSQL `JSONB`，兼顾亚毫秒恢复和跨周合规审计。

## Key Emphasis
1. **物理隔离红利**：`scratchpad` 与聊天历史分离，既省 Token 又能精准还原程序现场。
2. **Demo 边界诚实标明**：基础快照、Delta 接口与 Redis/PG 适配 Demo 已覆盖；大规模自动增量回放合并引擎为仅 PPT 讲解。

## Optional Extension
### Q1: 为什么在演示工程里 `AgentScopeCheckpointStore` 底层默认包的是 `InMemoryAgentStateStore`，而 `springai2` 里写了 `RedisPostgresCheckpointAdapter`？
**讲师解析**：两个工程都实现了同一套 `CheckpointStorePort` 接口，目的就是展示六边形端口解耦：V6 重点演示热缓存 Redis + 冷存储 PostgreSQL JSONB 的双写落库实现；V7 重点演示与 AgentScope 原生 `AgentStateStore` 的桥接适配（并且通过配置文件也预留了向 Redis/File 切换的扩展位）。在生产中只需替换端口实现即可共享这套热冷双轨存储。

## Classroom Interaction
“如果有人把 `ChatClient` 对象直接放进了 `scratchpad` 里做快照序列化，启动或运行到保存快照时会发生什么？”（Jackson 序列化直接抛出 `InvalidDefinitionException` 崩溃）。

## Take Away
用纯数据 `AgentCheckpoint` 承载执行现场，以“关键节点全量 + 中间步增量”和 Redis/PG 双轨存储平衡写入开销与恢复速度。

## Why This Slide Matters
把 Checkpointer 从抽象名词拆成落地的表结构、序列化约束和真实框架桥接代码，让学员真正具备设计有状态 Agent 存储的能力。

## Transition
快照存进去了，那当服务重启或用户改变主意时，怎么把快照“唤醒继续跑”，甚至“回退到某一步分叉重试”呢？请看 P14。
