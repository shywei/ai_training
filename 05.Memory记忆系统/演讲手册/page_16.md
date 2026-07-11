## Slide Objective
总结全课，明确学员应该带走的认知。

## Suggested Presentation
今天的一句话：Memory 不是聊天记录，而是企业为 AI 建立的记忆系统。

看左边的进度图。导购 Agent 的五大能力，今天完成了第二个——Memory。现在它能记住用户了。

看右边的能力地图。从三层架构开始，Session Memory（Redis + 滑动窗口 + 累积压缩），用户画像（四维度 + 触发机制 + 合并更新），context 注入（null → 画像+对话 → +RAG → +Tool），安全隐私、存储选型、生产环境。

但最重要的一点：Demo 是最基础的示例，不是可以直接用到生产环境的方案。你今天学到了原理和基础架构，但真正做项目时，还需要自己补上 PII 脱敏、Session 并发控制、画像准确性验证、压缩质量监控、成本监控、多租户隔离这些工程细节。Demo 教你怎么走路，生产需要你自己跑起来。

课后练习建议：第一，登录后聊几轮，说"记住我喜欢银色"，看 `logs/llm-summary.log 或 llm-profile.log` 理解画像抽取的完整过程。第二，退出再登录，问"我喜欢什么"，验证画像跨 Session 生效。第三，聊超过 8 轮，看压缩日志，理解累积摘要的机制。第四，读一遍 MemoryManager.java 的 buildContext 方法，理解 context 是怎么构建的。

## Key Emphasis
学员带走的不只是"Memory 是什么"，还有"Demo 和生产之间的差距在哪里"。

## Transition
感谢大家。下一课，Knowledge——让 Agent 了解企业知识。
