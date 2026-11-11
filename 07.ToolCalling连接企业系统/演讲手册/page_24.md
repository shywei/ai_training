## Slide Objective
讲清幂等的完整做法：重复调用的四个来源、幂等键怎么设计（生成 / 存放 / 重复返回 / 清理）、读写分开对待。对应本课 `v1.4` Demo 的真实反例是 `mcp-knowledge-server` 的 `MemoryService.save()`（每次直接 `INSERT INTO user_memory` 无幂等去重，以及若扩展 `place_order` 下单接口无幂等键会导致双下单），正例是 `KnowledgeIndexer.index(force)` 的"已有数据跳过"。

## Suggested Presentation
先立前提：**重复调用是常态，不是意外**。四个来源讲清楚——① 超时后重试：调用超时不等于没执行，下游可能已经写入或下单了；② LLM 重复决策：多轮循环里 LLM 看到不确定的结果会再调一次，被注入的提示词还能让它反复调；③ 用户重复提交：页面卡了再点一次；④ 链路重放：消息队列重投、运维手工补单。四条里任何一条都会发生，所以"幂等是可选项"这个念头先打消。

对着本课 `v1.4` 代码看 Demo 现状：
- **真实写操作反例（`save_memory`）**：`mcp-knowledge-server` 的 `MemoryService.save()` 每次都执行 `INSERT INTO user_memory (user_id, memory_type, content, metadata, embedding) VALUES (...)`——没有任何幂等键，同一批对话如果因为超时重试或重复触发沉淀，就会存进多条一模一样的记忆，污染后续语义检索。
- **扩展写操作反例（如下单 `place_order`）**：在 `v1.4` 中 `mcp-product-server` 只暴露了 3 个只读查询 Tool；如果我们接入下单写接口（下一课第八课将引入模拟下单并在真实生产对接订单库），不带幂等键时每次超时重试都会生成一张新订单（双下单事故）。
- **幂等雏形正例（`index_knowledge`）**：反过来看 `KnowledgeIndexer.index(force)`——检测到向量库已有数据就跳过、`force=true` 才清空重建，重跑结果一致。这就是幂等的直观体现："执行两次结果一样"。

幂等键设计四步：**生成**——`sessionId/requestId + toolName + 业务参数哈希`（下一课第八课引入 `TraceService` 与 `WorkflowStep` 后，即可直接用 `traceId + stepNumber + toolName + 参数哈希`），同一笔业务重试沿用同一个键。**存放**——Tool 端去重表（`idempotency_key, tool, args_hash, result, created_at`）或业务表唯一索引（如订单表 `uk_request_id`），插入冲突即视为重复。**重复返回**——不重新执行，**回放首次结果**（含首次的订单号或写入结果），调用方无感知；"什么都不返回"或抛异常不算幂等，调用方会当失败再发一次。**清理**——去重表设 TTL（24~72h），覆盖重试与补单的时间窗。

最后把读写分开：查询天然幂等（`queryProductInfo`/`queryPrice`/`queryStock` 都是 SELECT，重复无副作用，只配超时限流），写操作必须设计。判断标准一句话——**"这个 Tool 执行两次，世界会不会不一样？会，就必须有幂等键。"**

## Key Emphasis
必须讲清：超时 ≠ 失败（所以重试必须配幂等）；幂等键的四步设计；"重复请求回放首次结果"才算幂等。不要展开分布式 ID 生成器（雪花算法等）的细节——键怎么"生成"用业务语义组合就够，别把课带向发号器选型。

## Optional Extension
**学员问：幂等键放请求头（Idempotency-Key）还是放在业务参数里？**
答：看调用链的控制权在谁手里。
Demo 做法：直接放进 Tool 的入参 Schema（比如给 `save_memory` 或新增的 `place_order` 增加 `requestId` 字段），在 `McpServerConfig` 的 `inputSchema` 里声明、Tool 实现里判重——最小改动，学员在 `v1.4` Demo 里能直接动手验证。
生产做法：网关层用标准请求头 `Idempotency-Key` 统一透传（MCP Gateway 收在第十一课支柱一），Tool 端只管判重；跨团队系统约定键的归属方（谁生成谁负责）。取舍：放参数里每个 Tool 自己实现、容易漏；放请求头要网关和 SDK 配套，前期成本高但不会漏。

**学员问：去重表会不会越长越大？能不能用 Redis 就够？**
答：能用 Redis，但先想清楚丢了怎么办。
Demo 做法：Redis SETNX + TTL 存幂等键，快、够用。
生产做法：Redis 做快速判重（热路径），业务库唯一索引做最终兜底（Redis 丢键时数据库还能挡住）；结果存业务表，去重表只存键 + 结果指针。取舍：只用 Redis 意味着极端情况下（主从切换丢键）会漏挡一次重复；只用数据库判重则每个请求多一次写放大。双层是常见的折中。

## Classroom Interaction
让学员查看 `mcp-knowledge-server` 的 `MemoryService.java` 中 `save()` 方法的 SQL，观察连续调用两次 `saveMemory("U001", "summary", "用户预算8000元")` 时 `user_memory` 表会发生什么（插入两条重复记录），再口头设计如何用 `userId + content 哈希` 或 `requestId` 把它改造成幂等写入。

## Take Away
写操作先定幂等键（requestId/traceId+tool+参数哈希），Tool 端唯一索引判重、重复请求回放首次结果——幂等和重试必须成对上，缺一个就是在给自己造重复数据或重复单。

## Why This Slide Matters
跳过这页，学员会放心大胆地给 Tool 加重试、给 LLM 开多轮循环，然后在生产上下出双订单、存出重复记忆——这类事故的修复成本（退款、数据清洗、客诉）远高于实现幂等的成本，而且是上线后才暴露。

## Transition
幂等管"重复执行"，但还有一类事故它管不了——调用一直不返回。看下一页（page_25）超时：为什么超时 ≠ 失败。
