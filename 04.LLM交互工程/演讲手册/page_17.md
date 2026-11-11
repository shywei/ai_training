## Slide Objective
总结全课，展示进度图和能力地图，让学员带走结构化的认知。

## Suggested Presentation
今天的一句话：Prompt 决定 AI 怎么思考，API 决定 AI 怎么接入系统。

看左边的进度图。导购 Agent 的五大能力，今天完成了第一个——Prompt。这个绿色的勾不是装饰，它代表你已经写出了可运行的代码。后面四课会逐步完成 Memory、Knowledge、Tool、Workflow。

看右边的能力地图。我们走了一条完整的链路：从 LLM 运行机制开始，理解 OpenAI 协议，掌握 Token 和模型选型，实现模型路由和结构化输出，做好 Prompt 工程和安全防护，最后集成前端。每一层都对应 Demo 里的实际代码。

底部是阶段成果。导购 Agent 完成第一版——能理解用户意图、能输出结构化推荐。但它不记得用户，每次对话都是陌生人。

最后说一下课后练习。我建议你做三件事：第一，自己改 ModelRouter 的关键词数组，加几个你业务场景里常见的复杂问题关键词。第二，改 PromptManager 的 System Prompt，换成你自己的业务角色。第三，试一下结构化输出演示按钮，看后端日志里的完整链路。这三件事加起来不超过半小时，但能帮你巩固今天学的所有内容。

## Key Emphasis
进度图让学员知道"走到哪了"，能力地图让学员知道"这课学了什么结构"。课后练习要具体、可执行。

## Optional Extension
**学员问：课后练习做完了，下一步练什么才能真正内化？**
答：把练习往工程化推一步——改完要能证明没改坏。
Demo 做法：改 `ModelRouter` 关键词数组或 `PromptManager` 的 System Prompt 后，在前端与后端控制台对比改前改后的输出差异，眼见为实。
生产做法：准备一组测试输入（10 条就够），每次改完跑一遍，看结构化合规率、注入拦截率、平均耗时三个指标没退化才提交——这就是 Evaluation 回归的雏形（第六课 RAG Evaluation 会扩展到检索质量，第十一课评测支柱会系统化为 LLM-as-a-judge + CI 自动回归）。取舍：没有回归集，Prompt 优化就是盲改。

## Classroom Interaction
让学员现场说出今天七项生产差距中的三项（page_16 的清单）——说不出来的学员，下课第一件事应该是重看差距页。

## Take Away
今天交付的是能跑的交互层；把它变可靠，靠 page_16 的七件工程件和一套最小回归集。

## Why This Slide Matters
跳过这页，学员没有进度坐标：不知道自己在导购 Agent 五步路线图的哪一格，后四课听到"给 context 加内容"时不知道为什么、加在哪。

## Transition
感谢大家。下一课，我们给导购 Agent 加上记忆——让它记住用户是谁。

## 课后扩展学习引导（Post-Class Deep Dive Guide）
各位开发者与架构师同仁，在完成第四课的基础 API 与 Prompt 贯通后，如果您希望将这套交互层真正搬上企业千万级高并发、严苛安全与高可用生产环境，请务必精读本课根目录下的**《扩展学习指南.md》**！

这份指南遵循 3W 原则（Why-Where-What），直击高并发与模型工程的四大深水区主题：
1. **LLM 接口协议三代演进深度规范**：全面拆解从无状态的 Chat Completions 到状态下沉的 OpenAI Responses API，再到跨厂商 Open Responses 协议，搞懂为什么现代 Agent 网关不再手工拼装长数组；
2. **结构化输出底层机理（FSM 约束解码 vs Spring AI）**：深入有限状态机（DFA/FSM）在自回归采样时的 Logits 掩码机制，理解为什么 OpenAI `strict: true` 与 Outlines 能实现 100% 语法不出错，对比 Spring AI 客户端重试的优劣与开销；
3. **高并发 SSE 流式网关与背压控制**：揭秘 Nginx `proxy_buffering off` 核心参数，以及如何利用 Spring WebFlux 的响应式取消信号（`Cancel Upstream`），在用户关闭浏览器时秒级终止上游模型计费，杜绝 Token 偷跑与 Tomcat 线程饥饿；
4. **大模型安全护栏体系（OWASP Top 10 + NeMo Guardrails）**：深入输入、对话、执行、输出四道纵深防御防线，针对提示词注入（Prompt Injection）与越狱攻击提供工程级解法。

指南文末附带了 3 道硬核生产自测题（约束解码吞吐量权衡、SSE 慢连接背压失控、间接注入防御实战），帮助大家把课堂代码真正炼成生产高可用系统！
