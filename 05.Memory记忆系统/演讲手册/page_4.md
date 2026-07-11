## Slide Objective
讲透三层记忆架构的完整知识体系——每层的原理、数据结构、工程细节、常见坑。

## Suggested Presentation
Memory 不是一个东西，是三层。每一层解决不同的问题，用不同的存储，有不同的工程细节。

**Session 短期记忆——存"这轮对话"**

Session 的本质是什么？LLM API 是无状态的 HTTP POST，但业务需要有状态的对话。Session 就是服务端为每次对话维护的一个"上下文容器"。用户发第一条消息时创建 Session，后续每条消息追加进去，调用 LLM 时把整个 Session 的内容塞进 messages 数组。

Session ID 怎么传递？三种方式：URL 参数（`?sessionId=xxx`，Demo 用的）、Cookie（浏览器自动携带）、HTTP Header（`X-Session-Id`）。Demo 用 URL 参数是因为前后端分离，前端 fetch 请求里直接拼参数最简单。实际项目中推荐用 Header，更安全。

Redis 存 Session 的数据结构选择：Demo 用了 List，因为消息是有序的、需要按时间追加。但也可以用 Hash（以消息 ID 为 field，消息内容为 value）——好处是可以按 ID 删除单条消息，坏处是不保序。还可以用 Stream（Redis 5.0+）——天然支持消息队列、消费者组、持久化。Demo 选 List 是因为最简单、够用。

踩坑经验：Redis 的 List 操作是原子的，但如果多个请求同时写同一个 Session（比如用户快速连发两条消息），消息顺序可能乱。Demo 里没有处理这个问题。实际项目中应该用 Redis 的 MULTI/EXEC 事务，或者用消息队列保证顺序。

**Long-term Memory 长期记忆——存"这个用户"**

为什么不用 Redis 存画像？因为画像需要跨 Session 永久生效。用户今天聊完，明天再来，画像还在。Redis 虽然可以设不过期，但重启可能丢数据（除非配了 AOF 持久化）。MySQL 是更可靠的选择。

画像的四个维度不是随意设计的。Fact 事实是客观信息，不会变（姓名、职业）。Preference 偏好是主观喜好，会随时间变化（从喜欢红色变成喜欢银色）。Task 任务是短期需求，完成后应该清除（"找一台写代码的笔记本"买完就不用了）。Constraint 约束是长期限制（"不要游戏本"）。这四种维度的更新策略不一样——Fact 只增不改，Preference 新覆盖旧，Task 完成即删，Constraint 长期保留。

**Knowledge 知识——存"这家公司"**

Knowledge 和 Memory 的本质区别是什么？Memory 是"关于用户的信息"，Knowledge 是"关于世界的知识"。用户问"我上次买了什么"——查 Memory。用户问"这个笔记本的配置是什么"——查 Knowledge。用户问"根据我的偏好推荐笔记本"——需要 Memory（偏好）加 Knowledge（产品信息）配合。

Knowledge 用向量数据库是因为需要语义检索。用户问"笔记本卡顿怎么办"，关键词搜索匹配不到"电脑运行缓慢的解决方案"，但向量检索可以——因为两句话的语义相似度很高。下节课详讲。

## Key Emphasis
这一页信息量很大。重点是让学员理解"每一层的工程细节"，而不是"每一层叫什么名字"。Session 的数据结构选择、画像的更新策略、Memory 和 Knowledge 的本质区别——这些才是实际项目中需要做决策的地方。

## Transition
先看第一层——Session Memory 的方案全景。
