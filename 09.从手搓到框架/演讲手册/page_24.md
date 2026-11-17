# 第 24 页：全课核心总结：框架的边界与工程落地准则

## Slide Objective
完整收束第 09 课全部核心知识点（两条演进主线定性、基础设施 vs 应用框架选型速查、开发者必须守住的三条工程边界），干净闭环全课内容，不添加下一课预告，并提示学员课后通过 `扩展学习指南.md` 深入源码与进阶资料。

## Suggested Presentation
各位同学，我们来到第 09 课的最后一页。让我们用屏幕上的三张卡片，把今天这堂课最硬核的架构收获完整打包带走：

**第一栏：看清两条截然不同的演进主线**。
以后再和团队讨论“要不要上某个 AI 框架”时，先用这两把尺子量一量：
1. **主线 1 · 同构效率提升（V5 ➔ V6 ➔ V7）**：`ChatClient`、`@Tool`、`Advisor`、`ReActAgent` 替我们干的，是把第 04~08 课手搓过的功能换一种更优雅的框架写法。Agent 的架构本质没有变，价值在于把 1834 行底层协议和循环胶水压缩到 ~500 行；
2. **主线 2 · 企业级关键环节补齐**：**Checkpointer 断点复水与分支**、**认知生命周期 Middleware 挂起 HITL**、**多通道类型化事件流**、**Tool 强契约与自愈循环**——这 4 件事才是 V5 手搓架构极难做好、必须依靠运行时框架能力补齐的企业生产刚需。

**第二栏：基础设施 vs 应用框架选型速查**。
- **Spring AI 2.0.2 GA（Servlet API 级通信底座）**：搞定模型调用、`@Tool` 反射、内置 MCP 与单次请求 `Advisor` 链。适合单轮问答、RAG 和 1~2 步工具查询；
- **AgentScope 2.0.0 GA（Spring MVC 级应用框架）**：接管 `ReActAgent` 多步循环、五阶段 `MiddlewareBase`、多通道 `streamEvents` 与 `AgentStateStore`。适合动态多步规划、高危审批挂起与长链路断点恢复。

**第三栏：开发者永远不能交出去的三条工程边界**。
1. **扁平化 Tool DTO 与扩展自愈**：避开多态/泛型/双向关联三大反射死穴，用 Jakarta Bean Validation 配合 Advisor / Middleware 扩展点实现限次自愈重试；
2. **确定性业务风控与三层记忆主权**：满 ¥5000 审批与选品打分留在纯 Java 的 `ShoppingDomainService`；框架 `StateStore` 管运行断点，不替代 L2 用户画像与 L3 知识库；
3. **六边形 Ports & Adapters 隔离**：保持 `domain/` 与 `port/` 100% 纯 Java 零框架依赖，把所有 AI 框架全部关在可插拔的 `adapter/` 边缘。

课后大家可以按键盘 **`E` 键**打开本课的《扩展学习指南》，深入走读 Spring AI 2.0 Advisor 源码、AgentScope Middleware 时序、Checkpointer 表结构与 ArchUnit 红线测试，也可以对照运行 `8080 / 8083 / 8084` 三套工程源码。第 09 课到此圆满结束，感谢大家！

## Key Emphasis
1. **双主线认知彻底固化**：不再混淆“换框架写得短”与“补齐企业级运行能力”。
2. **三条工程底线清晰**：扁平 DTO + 扩展自愈、确定性风控与三层记忆、六边形防套牢。
3. **纯净收官**：专注于第 09 课自身的知识闭环，不展开后续课程内容。

## Optional Extension
### Q1: 课后本地运行 8083（Spring AI 2.0）和 8084（AgentScope 2.0）对照工程时，如果没装 Docker 或不想启动 PostgreSQL/Redis，能跑吗？
**讲师解析**：两套工程在设计时都考虑了教学演示的便利性：`AgentScopeCheckpointStore` 默认支持内存运行、`RedisPostgresCheckpointAdapter` 带有本地内存兜底、`SpringAi2ToolAdapter` 与 `McpEnterpriseToolAdapter` 也都内置了本地 Mock 商品和知识库回退。即便只配一个大模型 API Key（甚至纯跑离线 `mvn test` 单元测试），也能流畅验证核心逻辑。

## Classroom Interaction
结课收官提问：“请用一句话向团队成员总结今天这堂课最大的收获——什么时候用框架，什么东西绝不能交给框架？”（邀请 1~2 位学员发言总结）。

## Take Away
手搓理解原理，框架提升效率；分清同构提效与企业级能力补齐，用六边形架构守住业务边界、告别框架套牢。

## Why This Slide Matters
将全课 24 页的理论对比、源码实证与避坑经验浓缩为一页可随时翻查的架构师行动指南，实现高质量课程闭环。

## Transition
（第 09 课圆满收官，引导学员按 `E` 键查阅《扩展学习指南.md》并对照《Demo演示步骤.md》动手运行验证。）
