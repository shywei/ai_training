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

`search_knowledge` 是 Tool——需要语义搜索，数据量增长，全量加载不现实。`save_memory` 是 Tool——LLM 主动决策何时保存，不是每次都保存。`queryProductInfo` 是 Tool——LLM 根据用户意图决定是否查价格。`user://{userId}` 是 Resource——会话开始加载一次，数据相对稳定（用户画像不会每分钟变）。

商品数据虽然只有 26 个，但**不应该作为 Resource**。原因：商品数据需要语义搜索（"推荐轻薄本"不是简单的 ID 查询），Resource 不支持语义搜索。如果做成 Resource，Client 需要全量加载 26 个商品到 context 中，浪费 token。

有个学员可能会问："Resource 不是更简单吗？为什么不用 Resource 加载所有商品？"答案：Resource 是"读数据"，Tool 是"搜索数据"。26 个商品全量加载到 context 中，LLM 要自己从中筛选，效率低、不准确。Tool 让检索系统做筛选，只返回最相关的 3 个，效率高、准确。

## Key Emphasis
选 Tool 还是 Resource，看两个维度：谁发起（LLM 还是 Client）、数据特征（需要搜索还是全量加载）。

## Transition
知道了 Tool 和 Resource 的区别。现在看 Function Calling 的完整链路——从 MCP Client 到 LLM 调用。
