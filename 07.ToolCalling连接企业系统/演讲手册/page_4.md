## Slide Objective
展示 Function Calling 的完整 6 步流程和实际例子。

## Suggested Presentation
完整流程 6 步：

**① 定义函数**：每个函数需要三个要素——名称（如 `queryProductInfo`）、描述（"查询商品价格和库存"）、参数 Schema（JSON Schema 格式，定义参数类型和必填项）。Schema 是关键，它告诉 LLM 这个函数需要什么参数。

**② 注册到 LLM 请求**：在 OpenAI API 的请求中，有一个 `functions` 参数，把所有可用函数的定义传进去。LLM 收到请求后，知道"我有这些工具可以用"。

**③ 用户提问**：用户说"ThinkPad X1 多少钱"。

**④ LLM 返回 tool_call**：LLM 不直接回复价格（它不知道实时价格），而是返回一个 tool_call：`{name: "queryProductInfo", arguments: {"productId": "NB001"}}`。注意此时 `content` 字段为 null——LLM 不生成文本回复，只返回调用指令。

**⑤ 系统执行函数**：Spring AI 拦截 tool_call，找到对应的 FunctionCallback，执行函数。函数查 MySQL，返回"价格 ¥9999，库存 58 件，在售"。

**⑥ 结果回传 LLM**：把函数执行结果作为 `role=tool` 的消息追加到对话历史，LLM 基于结果生成最终回复："ThinkPad X1 Carbon 当前售价 ¥9999，库存 58 件，在售。"

实际例子中还有一个细节：LLM 自动把"ThinkPad X1"映射到商品 ID "NB001"。这个映射是 LLM 通过函数描述和上下文推断出来的，不需要我们写映射规则。

## Key Emphasis
Schema 的质量直接决定 LLM 调用的准确性。description 写得清楚，LLM 就能正确判断"什么时候该调这个函数"和"传什么参数"。这是 Function Calling 最核心的工程细节。

## Transition
既然了解了这6步，那在底层的网络通信中，这个协议的 JSON 请求和响应体到底长什么样呢？我们看下一页：协议底层格式。
