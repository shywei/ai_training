## Slide Objective
让学员理解大模型失忆的本质原因，用 Demo 日志和实际操作建立直观认知。

## Suggested Presentation
看左边。LLM API 没有记忆功能。每次请求都是独立的 HTTP POST，模型不保留任何上一次的状态。

Demo 里可以验证。打开前端，不登录直接聊天（其实现在必须登录了，那就登录后直接聊），问"我叫什么"，模型说不知道。然后看 `logs/llm-chat.log`，找到这次请求。你会看到 System Prompt 里只有角色定义，没有任何用户信息。User Prompt 里只有一条当前消息。这就是"失忆"的证据——每次请求的 messages 数组里只有当前这一条，没有历史。

看右边。有人可能会想：能不能把所有历史对话都塞进 messages 数组？理论上可以，但有三个实际问题。第一，Context Window 有限——100 万 token 听起来很多，但如果你的对话有 50 轮，每轮 200 token，加上 System Prompt 和 RAG 结果，很容易接近上限。第二，历史越长，Token 成本越高——推理模型的输出 token 比输入贵 2-5 倍，历史对话会作为输入 token 计费。第三，用户换个浏览器就丢了——Session 是客户端维度的，不是用户维度的。

所以 Memory 必须在服务端实现。模型是无状态的，记忆是有状态的——这个矛盾必须由你的代码来解决。

## Key Emphasis
用 `logs/llm-chat.log` 展示"无记忆"的请求长什么样。让学员亲眼看到 messages 数组里只有当前一条消息。这比任何解释都直观。

## Transition
既然需要在服务端建 Memory，那怎么设计架构？有哪些方案可以选择？
