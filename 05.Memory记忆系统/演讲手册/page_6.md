## Slide Objective
展示 Demo 的 Session Memory 实现，讲解关键代码和踩坑经验。

## Suggested Presentation
Demo 选了 Redis 方案。看左边的 Redis 结构。Key 是 `session:{sessionId}:messages`，List 结构。每条消息格式是 `role|content`，用 `|` 分隔角色和内容。为什么用 `|` 分隔而不是存 JSON？因为 List 的每个元素是字符串，用 `|` 分隔最简单，读取时 split 一下就行。踩坑经验：如果用户消息里包含 `|` 字符怎么办？Demo 里没有处理这个边界情况。实际项目中应该用 JSON 序列化，或者用不会出现在用户消息里的分隔符。

看右边的 SessionMemory.java 代码。addMessage 方法做三件事：RPUSH 追加、EXPIRE 设 24 小时过期、超过 MAX_MESSAGES 时 LPOP 裁剪。

滑动窗口的 MAX_MESSAGES 设为 20。但 Demo 里为了方便演示，MemoryManager 里检查的是 8 条就触发压缩。这两个数字不一样——20 是硬上限（超过就裁），8 是压缩阈值（超过就压缩）。实际项目中这两个数字应该更大，比如 50 和 30。

踩坑经验：一开始我把 MAX_MESSAGES 设为 8，结果每次聊 4 轮就触发压缩，压缩本身要调一次 LLM，用户体验很差——每次发消息都要等两秒。后来改成 20，压缩阈值设为 8，问题解决了。所以阈值设置要考虑"压缩本身的延迟"。

验证方法：用 `redis-cli LRANGE session:xxx:messages 0 -1` 可以直接查看当前对话内容。把 sessionId 替换成前端控制台里的实际值。

## Key Emphasis
代码讲解要具体到方法名和参数。踩坑经验要真实——不是假设的，是做 Demo 时真正遇到的。

## Transition
滑动窗口会丢失信息。怎么在控制长度的同时保留关键信息？
