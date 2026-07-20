## Slide Objective
具体列出 Demo 和生产的八个差距，每个差距要说清"是什么问题、不解决会怎样、怎么解决"。

## Suggested Presentation
八个具体差距，每个都是学员做项目时一定会遇到的。

**认证**：Demo 用明文密码对比（`user.getPassword().equals(password)`）。生产要 OAuth2 + JWT + Token 刷新 + 密码 BCrypt 哈希。不解决会怎样：密码泄露后直接可用。怎么解决：用 Spring Security + OAuth2 Starter，密码用 BCryptPasswordEncoder 哈希存储。

**权限**：Demo 任何 Agent 能调任何 Tool。生产要 RBAC 权限表——哪些 Agent 能调哪些 Tool。不解决会怎样：一个低权限 Agent 可以调用删除数据的 Tool。怎么解决：MCP Server 端做权限校验，Client 传递 Agent 身份信息。

**幂等**：Demo 不处理，LLM 可能重复调用下单接口。不解决会怎样：用户下一单，LLM 重复调用了 3 次，下了 3 单。怎么解决：每个请求带唯一 request_id，Tool 端做去重。

**超时**：Demo 写死 15 秒（`requestTimeout(Duration.ofSeconds(15))`）。生产要分级超时（查询 3s / 写入 10s / 批量 30s）+ 指数退避重试 + 熔断。不解决会怎样：一个慢查询阻塞整个对话。怎么解决：用 Resilience4j 做熔断，超时后降级返回"系统繁忙"。

**审计**：Demo 只写日志。生产要审计表记录（谁、什么时间、调了什么、参数、结果、耗时）。不解决会怎样：出了问题无法追溯。怎么解决：MCP Client 端加审计拦截器，记录每次 callTool 的完整信息。

**缓存**：Demo 无缓存。生产要语义缓存——相似 query 命中缓存，减少 LLM 调用和数据库查询。不解决会怎样："推荐笔记本"被问 100 次，每次都检索+生成。怎么解决：用 Redis 存 query 的 Embedding + 结果，新 query 先做相似度匹配。

**监控**：Demo 无监控。生产要 Tool 调用成功率、延迟 P99、LLM token 用量、成本归因。不解决会怎样：不知道哪个 Tool 慢、哪个 Tool 调用失败率高。

**安全**：Demo 无输入校验。生产要 SQL 注入防护、PII 脱敏、Tool 参数白名单校验。不解决会怎样：用户输入中嵌入 SQL 注入，Tool 被操控。

## Key Emphasis
这些不是"下一课的内容"，是"学员真正做项目时需要考虑的问题"。每个差距要具体到"是什么问题、不解决会怎样、怎么解决"。

## Transition
来看项目的代码结构和阅读路线图。
