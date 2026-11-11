# Page 10 — 用户画像 — Demo 实现

## Slide Objective
展示本课 Demo（`v1.2.1`）的用户画像抽取与合并实现（`UserProfileExtractor.java`）。

## Suggested Presentation
看左边的合并更新流程。这是 `UserProfileExtractor.java` 的核心设计：
1. 从 MySQL `user_profile` 表查出当前用户的**旧画像**（`existing`）；
2. 从 Redis `SessionMemory` 取出当前会话的**新对话**（`messages`）；
3. 将旧画像 JSON 与新对话一起拼入 Prompt，交给**推理模型**（`agent.model.reasoning`），Prompt 明确规定："保留旧画像中仍然有效的信息；用新对话中的信息补充或修正；新信息优先级高于旧信息；没有新信息的字段保持原值；只输出更新后的 JSON"；
4. 解析返回的 JSON，非空字段更新至 `UserProfile` 实体，通过 MyBatis Plus（`UserProfileMapper`）写回 MySQL `user_profile` 表。

为什么必须用推理模型（`agent.model.reasoning`）？因为画像抽取需要输出严格的四字段 JSON（`facts / preferences / tasks / constraints`）。小模型格式经常出错（漏双引号、多逗号），推理模型的 JSON 遵循度稳定得多。

看右边的 Demo 验证方式：
- **触发 1**：对话后通过 `@Async` 异步执行画像抽取，或调用 `POST /api/agent/profile/extract` 触发，不阻塞主对话响应；
- **触发 2**：会话结束 / 清空会话（`/api/agent/memory/clear`）时兜底触发一次画像抽取；
- **跨 Session 验证**：清空会话或重新登录，问"根据我的偏好推荐一台笔记本"，Agent 依然能从 MySQL 读取画像并精准推荐；
- **独立日志**：打开 `logs/llm-profile.log`，可以完整看到 `PROFILE EXTRACT REQUEST`（含旧画像 JSON 与新对话）和 `PROFILE EXTRACT RESPONSE`（LLM 返回的合并后 JSON）。

## Key Emphasis
合并更新是本页核心——旧画像 + 新对话一起交给推理模型（`agent.model.reasoning`），新信息修正旧结论、空值不覆盖。验证路径讲全：触发抽取 → 查看 `logs/llm-profile.log` 的合并过程 → 查 MySQL `user_profile` 表结果 → 清空会话验证跨 Session 生效。

## Optional Extension
**学员问：LLM 把画像抽错了怎么办？比如用户说"我在帮朋友看轻薄本"被记成用户本人的偏好。**
答：抽取错误不可避免，生产系统要设计"高置信度过滤 + 用户可更正"的路径。
Demo 做法：Demo 直接合并落库，靠 `logs/llm-profile.log` 观察核对——教学演示直观清晰。
生产做法：Prompt 中增加置信度判断（排除帮亲友代看等临时语境），并在前端提供"我的画像标签"管理界面，允许用户查看、修改或删除标签（同时满足 Page 12 的合规要求）。

## Classroom Interaction
现场走一遍闭环：发送包含职业、预算、偏好的对话（或调 `/profile/extract`）→ 打开 `logs/llm-profile.log` 看合并前后的 JSON → 查 MySQL `SELECT * FROM user_profile WHERE user_id='zhangsan'` → 点击清空会话再问"根据我的偏好推荐笔记本"。四个动作 2 分钟，本课长期记忆核心机制全部验完。

## Take Away
画像是"合并演进的结构化结论"，不是一次性抽取的文本——合并规则（新信息优先、空值不覆盖）写进抽取 Prompt，并通过 `logs/llm-profile.log` 全程可观测。

## Why This Slide Matters
用覆盖式写画像（每次只抽当前对话、不传旧画像）的系统会不断丢失历史结论；不看 `logs/llm-profile.log` 的人出了 JSON 解析失败也无法定位。

## Transition
Session 短期记忆和 MySQL 长期画像都讲完了。这两部分数据是怎么拼进 Prompt 传给大模型的？我们进入第六章（Page 11：context 注入 — 贯穿后续课程的主线）。
