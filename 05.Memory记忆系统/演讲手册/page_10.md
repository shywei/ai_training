## Slide Objective
展示 Demo 的用户画像实现。

## Suggested Presentation
看左边的合并更新流程。这是核心设计——旧画像从 MySQL 取出来，新对话从 Redis 取出来，两部分都传给推理模型。LLM 看到旧画像和新对话后，自主决定保留什么、更新什么、新增什么。

为什么用推理模型？因为画像抽取需要输出严格 JSON。小模型格式经常出错——值不加引号、多余逗号。推理模型的 JSON 格式更稳定。配置项是 agent.model.extract，从 yml 读取。

看右边的验证方式。两种触发：用户说"记住"时立即触发，退出登录时兜底触发。验证画像跨 Session 生效：退出再登录，问"我喜欢什么"，Agent 还记得。

日志文件 logs/llm-summary.log 或 llm-profile.log 里可以看到旧画像 JSON、对话内容、LLM 返回的合并结果。

## Transition
Session 和画像都讲完了。context 参数怎么从 null 演进到有内容？
