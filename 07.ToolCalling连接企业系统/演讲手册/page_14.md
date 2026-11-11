## Slide Objective
讲解 Tool vs Resource 的选型决策——什么时候用 Tool、什么时候用 Resource。

## Suggested Presentation
Tool 和 Resource 是 MCP 的两个核心概念，本质区别是"谁发起"。

**Tool** 是 LLM 决定调用的。LLM 看到 function definitions，根据用户意图决定"要不要调、调什么、传什么参数"。适合动态查询、写入操作。类比 POST 接口。

**Resource** 是 Client 主动读取的。Client 知道 URI，直接读取数据。不需要 LLM 决策。适合静态上下文、配置数据。类比 GET 接口。

选型决策树：
- 需要 LLM 决定是否调用？→ Tool
- 数据量大、需要搜索？→ Tool
- 会话开始加载一次？→ Resource
- 数据相对稳定？→ Resource
- 全量加载合理？→ Resource

Demo 中的选型理由：

`search_knowledge` 是 Tool——需要语义搜索，数据量增长，全量加载不现实。`search_memory` 是 Tool——需要语义搜索历史记忆。`queryProductInfo` 是 Tool——LLM 根据用户意图决定是否查价格。`user://{userId}` 是 Resource——会话开始加载一次，数据相对稳定（用户画像不会每分钟变）。画像是从多轮对话中提炼的结构化标签，由专门的抽取系统维护，是"用户是谁"的权威答案。它比 user_memory（对话历史记录）重要得多——画像是结论，记忆是过程。

商品数据虽然只有 26 个，但**不应该作为 Resource**。原因：商品数据需要语义搜索（"推荐轻薄本"不是简单的 ID 查询），Resource 不支持语义搜索。如果做成 Resource，Client 需要全量加载 26 个商品到 context 中，浪费 token。

有个学员可能会问："Resource 不是更简单吗？为什么不用 Resource 加载所有商品？"答案：Resource 是"读数据"，Tool 是"搜索数据"。26 个商品全量加载到 context 中，LLM 要自己从中筛选，效率低、不准确。Tool 让检索系统做筛选，只返回最相关的 3 个，效率高、准确。

## Key Emphasis
选 Tool 还是 Resource，看两个维度：谁发起（LLM 还是 Client）、数据特征（需要搜索还是全量加载）。

## Optional Extension
**学员问：用户画像也不会每分钟变，为什么记忆不也做成 Resource 一次性读回来？**
答：看取数方式是"按 URI 取回"还是"按条件搜出"。画像是一个用户一份、按 ID 取；记忆是几百条里按当前问题语义找相关的几条。
Demo 做法：`user://{userId}` Resource 会话开始读一次（`UserProfileService` 查 MySQL 返回结构化 JSON）；记忆走 `search_memory` Tool（`MemoryService` 在 pgvector 里做相似度搜索）——因为要按当前问题语义检索。
生产做法：画像这类稳定结构化数据可 Resource 预取 + 缓存（TTL 到期再读）；记忆/知识走 Tool + 检索服务，数据量大时加 metadata 前置过滤。取舍：判断错了两个方向都疼——把该搜的做成 Resource，26 个商品全量进 context 浪费 token 且 LLM 筛不准；把该取的做成 Tool，LLM 多一次决策还可能传错 ID。

## Take Away
Tool/Resource 选型就看一个判据：这个数据是"按条件搜出来的"还是"按 URI 取回来的"——搜出来是 Tool，取回来是 Resource，判错的代价是 token 爆炸或检索不准。

## Why This Slide Matters
跳过这页，学员会把需要语义搜索的数据做成 Resource 全量塞 context（token 爆、LLM 筛不准），或把一次读取的配置做成 Tool 让 LLM 白白多一次决策和出错机会。

## Transition
知道了 Tool 和 Resource 的区别。现在看 Function Calling 的完整链路——从 MCP Client 到 LLM 调用。
