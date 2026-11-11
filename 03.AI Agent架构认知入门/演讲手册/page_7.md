## Slide Objective
深入解剖 Context Engineering（上下文工程动态组装全景图），打通 Prompt、Memory、RAG、Tool 定义在底层 Token 窗口中的装配机制。

## Suggested Presentation
请大家看第 7 页，这是**智能体系统工程最核心、含金量最高的一张图：Context Engineering 上下文工程动态组装全景图！**
很多人学了 Memory、学了 RAG、学了 Tool Calling，却不知道它们在底层代码里到底是怎么协同工作的。
请看这张解剖图：在每一次把请求发送给大模型 API 的毫秒瞬间，后端的 Agent 运行时其实在做一件极其精密的拼装手术！
系统把一个原本空白的 Context Window，严格按照依赖顺序动态拼接成七大模块：
1. **System Prompt（系统角色基底）**：声明 Agent 身份、核心职责与安全防线；
2. **User Profile（用户画像/长期记忆）**：从 MySQL 异步读取的用户消费偏好、历史特征；
3. **Retrieved Context（RAG 检索知识块）**：从 pgvector 向量库查出的最新商品信息，用 `<retrieved_context>` 隔离标签包裹；
4. **Relevant Memory（动态语义短期记忆）**：从 Redis 窗口与记忆库召回的与当前话题相关的历史事实；
5. **Session History（滑动会话历史）**：用户与 Assistant 最近几轮真实的交互上下文；
6. **Tool Definitions（工具定义/JSON Schema）**：通过 API 参数注入的当前可用工具白名单；
7. **Current User Input（用户当前最新输入）**：本次对话触发点。
**架构师的顿悟：所谓的 Agent 开发，本质上就是一套高并发、低延迟、精准组装高质量 Context 的管道工程（Pipeline Engineering）！**

## Key Q&A / Clarifications
**Q：为什么要把 RAG 检索出来的内容用 <retrieved_context> 标签包裹？**
A：这是为了防止“间接提示词注入（Indirect Prompt Injection）”！外部检索的数据是不可信的，用明确的数据标签隔离，能向大模型明确声明：这是供你参考的事实数据，绝非可执行的系统指令。这是构建工业级防御体系必不可少的标准范式。

## Transition
弄清楚了上下文组装全貌，我们来看第 8 页：企业级 Agent 的核心数学公式与能力分级演进。
