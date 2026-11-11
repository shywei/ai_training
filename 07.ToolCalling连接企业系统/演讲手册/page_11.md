## Slide Objective
【P0 级重点重构】深入讲解 MCP 的三层架构，并建立“协议演进四阶段时间线（2024-11 初始协议 $\to$ 2025-03 Streamable HTTP 引入 $\to$ 2025-11 会话式 Streamable HTTP $\to$ 2026-07 无状态演进）”以及“Demo 实践基线（Spring AI 1.0.0-M4 的 HTTP+SSE 实现）”与“MCP 2026-07-28 前瞻最新无状态标准”的双轨制对比映射，让学员既能看懂现有 Demo 代码并动手抓包调试，又能掌握 2026 最新发布的无状态规范、请求头路由、MRTR 多轮交互与企业生产落地选型。

## Suggested Presentation
各位学员，这一页是第七课最核心的技术演进坐标。请大家重点看左右两块内容：

### 一、MCP 协议演进的四个阶段脉络
1. **2024-11-05（初始协议发布）**：定义了本地 stdio 进程管道与远程网络 HTTP + SSE 双端点模型（`/sse` 建立长连接，`/mcp/message` 接收请求）。这是本课 Demo 所采用的教学基线。
2. **2025-03-26（引入 Streamable HTTP）**：正式推出单端点 Streamable HTTP 规范，并将早期的 HTTP + SSE 标记为进入弃用路线。
3. **2025-11-25（会话式 Streamable HTTP 阶段）**：虽采用 Streamable HTTP，但依然保留了会话机制，强制使用 `initialize` / `initialized` 两阶段握手，并通过 `Mcp-Session-Id` 请求头维护服务端会话状态。
4. **2026-07-28（无状态核心与架构演进，最新规范）**：
   - **全面走向无状态（Stateless Core）**：移除协议级会话依赖与 `Mcp-Session-Id`，取消强制初始化握手，每次 HTTP POST 均为自包含独立认证请求；
   - **统一发现与请求头路由**：引入 `server/discover` 统一 RPC 方法一次性协商元数据与能力清单；通过 `Mcp-Method` 与 `Mcp-Name` 请求头实现 API 网关层无损快速路由；引入 `ttlMs` 与 `cacheScope` 列表结果缓存机制；
   - **Tasks 独立与授权强化**：长任务 Tasks 解耦为独立 Extension；全面支持 OAuth 2.1 强化安全机制与客户端元数据文档（CIMD）；
   - **MRTR 交互机制（Multi Round-Trip Requests）**：服务端在工具执行缺少关键参数或需要确认时，返回 `input_required` 状态，客户端通过 `inputResponses` 提交用户补充答案后重试原请求，提供协议原生的一站式多轮补充交互。

---

### 二、双轨制对比与四层工程实现边界（右侧表格与警示框）
在授课时，讲师要向学员清晰阐述“四层工程实现边界”，防止将“协议规范发布”与“Java 当前代码库支持”混为一谈：
1. **MCP 协议层**：讲清 2025-11-25（会话式 Streamable HTTP，`Mcp-Session-Id`）与 2026-07-28（Stateless Core 无状态核心）两代协议规范的演进脉络；
2. **Java SDK 层**：截至 2026-11-03，官方 Classic SDK 稳定支持 2025-11 路线；而在 SDK 2.2.0 中，Modern Server 仍标注为 **Preview** 状态，当前**暂无 Modern Client**，Tasks 扩展亦尚未在 Java SDK 落地；
3. **Spring AI 层**：Spring AI 2.0.2 GA 提供了 WebMVC、WebFlux 与 Stateless Streamable HTTP Transport 支持，但**这不等同于已经完整实现了 2026-07-28 规范的全部核心特性**，两者的协议语义和握手机制不能直接画等号；
4. **Demo 教学层**：我们的双 MCP Server 采用 2024-11 HTTP+SSE 教学基线，直观展示握手、通信与断线重连，确保学员亲手能够抓包验证。

- **教学结论**：绝不能让学员产生“只要用了 Spring AI 2.0.2，就能直接开箱即用开发出完全符合 MCP 2026 最新规范全部特性的 Client/Server”的误解，必须严格以 Java 生态的真实边界为准！

## Key Emphasis
1. **协议时间线严谨性**：2024-11（stdio / HTTP+SSE） $\to$ 2025-03（Streamable HTTP 引入） $\to$ 2025-11（会话式 Streamable HTTP，Mcp-Session-Id） $\to$ 2026-07（无状态核心、免握手、头路由、MRTR）。
2. **四层实现边界意识**：协议发布 $\neq$ Java SDK 完整实现 $\neq$ Spring AI 原生全覆盖。Modern SDK 仍为 Preview，暂无 Modern Client。
3. **MRTR 核心机制**：MRTR 是通过服务端返回 `input_required` 提出补充要求，客户端通过 `inputResponses` 答复后重试，形成结构化多轮参数收集闭环。
4. **Demo 与前沿双轨并重**：代码工程完全冻结在 Spring AI 1.0.0-M4（保证代码可运行、可复现、可动手），但架构视野必须升级到 MCP 2026 无状态演进标准，绝不能让学员学到过时心智模型。

## Optional Extension
**学员问：如果把 Demo 现有的 HTTP+SSE 升级到 2026 最新的 Streamable HTTP 无状态模式，后端的业务 Tool 代码要重写吗？**
答：完全不需要重写！这就是 MCP 三层架构将 Transport 独立解耦的巨大威力。
- 业务 Tool（如 `queryProductInfo`、`save_memory`）的入参 Schema 和业务逻辑完全独立于网络传输层。
- 升级时，只需将底层 Transport Provider 切换为支持 2026 规范的无状态 HTTP Handler，并在 API 网关层配置 OAuth 2.1 Token 校验即可，业务逻辑代码零感知。这将在第十一课 MCP Gateway 架构中完整验证。

## Take Away
**MCP 2026 演进核心坐标**：从“有状态长连接 + 显式两阶段握手”演进为“无状态请求模型（HTTP POST）+ 免握手 + `server/discover` 动态发现”；Demo 实践提供直观教学基线，企业生产对齐无状态微服务网关标准。

## Why This Slide Matters
跳过这页，学员要么在调试 Demo 时混淆 `/sse` 与 `/mcp/message` 的分工，要么在进入企业后盲目照搬早期有状态长连接方案，导致微服务网关在容器漂移和弹性扩容时爆发大量 Session 失效事故。

## Transition
理解了 MCP 架构与 2026 协议演进，接下来看 page_12：Demo 中的两个 MCP Server 具体如何编写代码暴露 Tool 和 Resource。