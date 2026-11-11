# Page 3 — 大模型的失忆症

## Slide Objective
让学员理解大模型失忆的本质原因，用 Demo 日志和实际操作建立直观认知。

## Suggested Presentation
看左边。LLM API 没有记忆功能。每次请求都是独立的 HTTP POST，模型不保留任何上一次的状态。

Demo 里可以验证。打开前端登录后直接聊，先问"我叫什么"，模型说不知道。然后看 `logs/llm-interactions.log`（或 `logs/llm-chat.log`），找到这次请求。你会看到 System Prompt 里只有角色定义，没有任何用户信息。User Prompt 里只有一条当前消息。这就是"失忆"的证据——每次请求默认只有当前这一条问题，没有历史。

看右边。有人可能会想：能不能把所有历史对话都塞进请求里？理论上可以，但有三个实际问题。第一，Context Window 有限——如果对话有 50 轮，加上 System Prompt 和 RAG 结果，很容易逼近上限。第二，历史越长，Token 成本越高——历史对话每次都会作为输入 token 重复计费。第三，用户换个浏览器或开启新会话就丢了——Session 是单次会话维度的，不是跨会话用户维度的。

所以 Memory 必须在服务端实现。模型是无状态的，记忆是有状态的——这个矛盾必须由你的服务端代码来解决。

## Key Emphasis
用 `logs/llm-interactions.log` 展示"无记忆"的请求长什么样。让学员亲眼看到初始请求没有任何历史上下文。这比任何解释都直观。

## Optional Extension
**学员问：把所有历史都塞进 messages 不就行了？模型上下文不是越来越大吗？**
答：能塞 ≠ 该塞，三个成本会先到：token 费用、上下文信噪比下降、首字响应延迟。
Demo 做法：本课 Demo（`v1.2.1`）的 `SessionMemory` 设滑动窗口硬上限 20 条，`SummaryPipeline` 在消息超过 8 条时触发累积摘要压缩（保留最近 4 条原文 + 1 条累积摘要），`MemoryManager.buildContext()` 只注入画像 + 摘要与最近对话——打开日志文件能清楚看到每次注入的量。
生产做法：按 token 预算装配 context（系统提示/画像/检索结果/历史各有配额），超预算先裁旧消息、再压摘要；超长历史在下一课引入向量库按需检索。第六课讲长上下文 vs RAG 的边界，第八课 Workflow 讲多轮上下文预算怎么控。

## Classroom Interaction
现场做三步：新会话问"我叫什么"→ 不知道；说"我叫张三"再问 → 同 Session 记得；清空会话再问 → 若未抽取画像又不知道了，抽取画像后跨会话依然记得。三个动作把"无记忆 / Session 记忆 / 跨 Session 长期画像"一次讲完。

## Take Away
记忆必须由服务端状态承担——模型每次调用都是无状态的 HTTP POST，指望模型自己记就是没做设计。

## Why This Slide Matters
跳过这页，学员会把记忆问题推给模型（"换个长上下文模型就好了"），不做服务端的存储、压缩与注入设计——长对话一来就 token 爆炸、成本失控。

## Transition
既然需要在服务端建 Memory，那怎么设计整体架构？我们进入第二章（Page 4：Memory Architecture — 三层记忆架构）。
