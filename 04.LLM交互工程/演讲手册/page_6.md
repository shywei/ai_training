## Slide Objective
解耦**模型供应商 API 协议演进**与**企业级 LLM 网关架构演进**两条主线。讲清供应商协议从 Completion 到 Chat Completions 再到 2025/2026 Responses API 的演化路线，以及企业内部从直连 SDK 到统一框架抽象、再到独立 LLM 网关治理的演进闭环；严正警示“兼容协议 ≠ 完全兼容”的方言陷阱。对应 Demo：Spring AI `ChatClient` 走 `/v1/chat/completions` 作为“兼容基线与代码抽象样本”，企业网关架构作为架构师进阶讲解。

## Suggested Presentation
讲这页时，先帮学员建立一个极关键的架构思维：**把“外部供应商的协议演进”与“企业内部的网关架构演进”严格解耦**。很多团队把两者混为一谈，导致外部 API 一改版，企业内部几十个微服务鸡飞狗跳。

### 轨道一：模型厂商 API 协议演进（Provider API Evolution）
看左侧时间线，厂商的 API 接口经历了清晰的三代跃迁：

1. **早期 Completion API（2020，GPT-3 时代）：** 
   - 接口是 `POST /v1/completions`，入参只有一串无结构的 `prompt` 字符串。
   - 缺点是没有角色（role）概念，多轮对话只能靠程序员手工拼接 `Human: xxx \nAssistant: xxx`，格式极易漂移，System 约束经常被用户输入冲垮。
2. **Chat Completions API（2023，工业基线）：**
   - 接口是 `POST /v1/chat/completions`，奠定了 `messages: [system, user, assistant]` 的结构化角色范式，并标准化了 Tools / Function Calling 机制。
   - **行业地位：** 全行业事实上的“最大公约数与兼容基线”。vLLM、Ollama、硅基流动、DeepSeek、Qwen 等开源及国产模型全量支持。本课 Demo 全部基于该基线构建，保证最大的通用性。
3. **Responses API（2025/2026，OpenAI 原生 Agent 规范）：**
   - 接口是 `POST /v1/responses`，是 Chat 的**语义超集**。
   - 核心变革在于将文本、工具调用、思考流（Reasoning）原生抽象为统一的**输出项（items）**，支持状态可续接与内置工具协同。
   - **架构红线：** OpenAI 官方已于 2025-08-26 废弃 Assistants API，并将于 2026-08-26 彻底关停。Threads/Runs 等能力全面收敛至 Responses API。严禁在 2026 年还基于 Assistants API 规划新系统！
4. **旁系与开放规范：**
   - Anthropic Messages API（`/v1/messages`）自成一派，Claude 系默认采用；
   - 社区正在推进 **Open Responses** 开放中立规范（LiteLLM / OpenRouter 吸纳），致力于打破单一厂商绑定。

---

### 轨道二：企业级 LLM 网关架构演进（Enterprise Gateway Evolution）
再看右侧，企业自身的接入架构经历了三个阶段：

1. **阶段一：厂商专用 SDK 直连（点对点混杂）：**
   - 每个项目分别引入 `openai-java`、`zhipu-sdk` 等，硬编码 API Key 和客户端。
   - 后果：供应商完全锁死，更换模型需要重构业务代码，缺乏统一鉴权、日志、配额与熔断。
2. **阶段二：应用内统一客户端抽象（Spring AI / LangChain）：**
   - 应用层通过统一的 `ChatClient` 或 `ChatModel` 接口编程（本课 Demo 所处阶段）。
   - 收益：换模型只需修改 `application.yml` 的 `base-url` 与模型名。
   - 局限：属于单体进程内的代码级抽象。在微服务架构下，数十个服务各自持有 Key、各自算 Token，无法做到全局配额控制、跨服务负载均衡与统一安全合规审计。
3. **阶段三：企业级独立 LLM 网关（架构终局）：**
   - 独立部署集中式网关（如自研网关、OneAPI、LiteLLM 企业版等），承担四大核心职责：
     - **统一协议封装：** 对内暴露统一标准的 OpenAPI 协议。
     - **协议方言适配：** 动态抹平各家厂商的格式差异。
     - **高性能流式代理：** 处理 SSE 长连接、数据缓冲、心跳与客户端断连清理。
     - **治理矩阵：** 多维动态路由、Token 计费配额、多级安全护栏、健康探测与故障熔断。

---

### 🚨 严正警示：OpenAI 兼容协议 ≠ 100% 完全兼容！
特别提醒学员注意右下角的红线：
很多团队以为“只要厂商声称兼容 OpenAI，改个 Base URL 就能无缝切换”，这是生产环境最常见的坑！
- **Tool Calling 差异：** 部分模型不支持并行工具调用（Parallel Tool Calls），或者参数返回的 JSON 缺乏严格 Schema 约束；
- **思考流字段方言：** 推理模型流式输出时，DeepSeek 返回 `delta.reasoning_content`，某些平台返回 `delta.reasoning`，还有的用 `<think>` 标签包裹在普通 `content` 中；
- **结构化输出（Structured Outputs）：** 只有部分厂商支持原生的 `response_format: json_schema` 严格模式，其他厂商传该参数会直接报 400 错误；
- **多模态入参：** 图片 Base64 与 URL 传参字段各厂商规范未完全对齐。
**因此，企业级 LLM 网关的第一要务，就是通过协议适配器（Protocol Adapter）抹平这些方言差异！**

## Key Emphasis
1. 建立双轨解耦认知：不要因为供应商出了新 API 就推翻内部业务架构；内部网关把协议统一，外部适配层单独维护。
2. 记住三代 API 的演化逻辑：文本补全（Completion）→ 角色对话（Chat Completions）→ Agent 输出项原生化（Responses）。
3. 牢记“兼容协议不等于完全兼容”：在网关层做方言转换，是避免业务层踩坑的核心手段。

## Optional Extension
**学员问：既然 Responses API 是未来，为什么我们 Demo 还用 Chat Completions？**
答：这是经过深思熟虑的工程选型决策。
1. **兼容性最大化：** Chat Completions 是当前整个 AI 生态的事实基线。学员在本地跑 Ollama、vLLM，或者接国内各大云厂商的模型，100% 都支持 `/v1/chat/completions`。如果 Demo 绑定最新的 Responses API，学员本地的开源模型几乎全都无法运行。
2. **抽象相通性：** 无论是 Chat 还是 Responses，其底层的 messages 组装、Prompt 静态前缀隔离、流式解析、结构化约束等核心交互机制完全一致。在 Demo 中掌握了 Chat Completions 的工程原理，接入网关或切到 Responses 只需换接口适配器，业务层核心资产 100% 复用。

## Classroom Interaction
现场提问：“如果明天公司要求把主模型从通义千问切到本地私有化部署的 DeepSeek-R1，如果按照阶段一的 SDK 直连方案要改几天？阶段二要改几行？阶段三的网关要怎么操作？”
引导学员说出：阶段一要改业务代码；阶段二改配置文件；阶段三只需要在网关后台配置一条路由规则，业务服务零感知零重启。

## Take Away
API 协议在变，网关治理永恒。以 Chat Completions 为兼容基线，以独立企业网关承载方言适配与多维治理，是抵御技术栈动荡的唯一正道。

## Transition
接口和网关的关系理清了，接下来看另一个企业级核心组件：当面对十几种不同价格、不同性能的模型时，网关如何做出最优决策？来看第 8 页的“多维模型路由矩阵”。
