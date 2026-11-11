# Page 5 — Session Memory — 方案全景

## Slide Objective
讲透 Session Memory 的方案全景——每种方案的原理、数据结构、适用场景、工程细节。

## Suggested Presentation
Session Memory 有三种主流存储方案，每种都有明确的适用场景。

**方案一：内存存储（ConcurrentHashMap / Caffeine）**

最简单。数据存在 JVM 堆里，读写零网络延迟。ConcurrentHashMap 是 JDK 自带的线程安全 Map，Caffeine 是高性能本地缓存库，支持过期策略与容量上限。

但有两个致命问题。第一，重启丢失——JVM 一停，所有 Session 消失。第二，不支持多实例——部署 3 个实例时，用户的请求被负载均衡到不同实例，每个实例只存了部分对话。

适用场景：原型开发、单机部署、对数据丢失不敏感的内部工具。

**方案二：Redis（Demo 选择，主流方案）**

Redis 是生产环境的主流选择。核心优势：List 结构天然适合消息队列——RPUSH 追加、LRANGE 读取、LPOP 裁剪。自带 EXPIRE 过期机制，不用自己写清理逻辑。支持集群部署，多实例共享同一个 Redis。

踩坑经验：Redis 的 EXPIRE 是从最后一次写入开始计时的。如果设了 24 小时过期，用户每发一条消息，过期时间就重置为 24 小时后。这意味着只要用户持续对话，Session 永远不过期；只有停止对话 24 小时后才会自动清理。

**方案三：数据库（MySQL / PostgreSQL）**

读写慢但持久化、可查询。适合需要审计和合规的场景——比如金融行业的客服系统，每条对话都要留痕，监管机构随时可能要求调取。

**Token 爆炸问题——所有方案都要面对**

不管用哪种存储，对话越来越长都会导致 Token 成本越来越高。两种解决策略：滑动窗口（裁掉旧消息，简单但丢失信息）和后台压缩（旧消息变摘要，保留信息但需要调用 LLM）。

**Memory 生命周期**

企业必须回答的问题：Memory 会无限增长吗？答案是不能。必须有生命周期管理：Message（新消息）→ Session（当前对话）→ Summary（压缩后的摘要）→ Archive（归档到冷存储或向量库）→ Delete（最终删除）。本课 Demo（`v1.2.1`）实现了 Message → Session → Summary（`SummaryPipeline` 累积摘要），后续第六、七课将进一步演进出向量化归档沉淀。

## Key Emphasis
每种方案的"为什么"比"是什么"更重要。不是"Redis 能存 Session"，而是"为什么选 Redis 而不是本地内存或关系型数据库"。

## Optional Extension
**学员问：为什么 Demo 用 Redis List 不用 Stream 或 Hash？**
答：看操作模式，不是看新旧。
Demo 做法：`SessionMemory.java` 只需要三种操作——RPUSH 追加、LRANGE 全读、LPOP 裁剪，List 天然对应，几行代码搞定。
生产做法：要消费者组、断线续传、多读者就上 Stream（Redis 5.0+）；要按消息 ID 删单条就用 Hash；并发写同一 Session 的顺序问题用 MULTI/EXEC 或分布式锁兜住。

## Classroom Interaction
让学员 `redis-cli LRANGE session:default:messages 0 -1` 和 `redis-cli TTL session:default:messages` 各跑一次——看到消息列表和 24 小时倒计时，方案全景就不是纸上谈兵了。

## Take Away
Session 存储按"读写模式 + 生命周期"选型；EXPIRE 是滑动的——活跃会话永不过期，容量必须另有兜底（滑动窗口 + 摘要压缩，以及 Page 16 第 2 项）。

## Why This Slide Matters
直接选 MySQL 存高频会话的，高并发下会撞延迟墙；直接用 JVM 内存的，一重启或多实例部署就丢会话——都是没做这页的方案对比就动手。

## Transition
看完了方案全景，Demo 选了 Redis。具体怎么实现的？请看下一页（Page 6：Session Memory — Demo 实现）。
