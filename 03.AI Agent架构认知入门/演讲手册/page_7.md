## Slide Objective
解释Memory的存在意义，区分短期记忆和长期记忆，让学员了解当前行业主流的Memory实现方案。

## Suggested Presentation
你的Agent知道自己是谁了，但它遇到了一个问题：它记性很差。大模型本身是无状态的，它不会记得你上一句说了什么。没有Memory，每次用户来，Agent都觉得是陌生人。

Memory就是把聊天历史、用户偏好存下来，下次用户再来时，悄悄塞给大模型。

这里有一个关键区分：Memory记"人"——这个用户买了什么、偏好什么；Knowledge记"事"——公司有什么产品、政策怎么规定。两者配合，Agent才能既懂用户又懂业务。

那Memory在工程上怎么做？目前行业里有两种主流架构。
第一种是短期记忆，也叫Session Memory。它保存的是当前这轮对话的上下文，通常用Redis实现。快，但用户关掉窗口就没了。
第二种是长期记忆，也叫Long-term Memory。它把用户的历史偏好、重要决策持久化存储，下次对话时自动检索注入。通常用向量数据库（比如PgVector）或者专门的记忆框架实现。

目前行业里做长期记忆比较成熟的开源方案有Mem0和Zep。Mem0的核心思路是把对话中的关键信息自动提取出来，存成结构化的记忆条目，下次按语义检索。Zep的做法类似，但更侧重于企业级场景，支持多用户隔离和记忆衰减。

在Spring AI生态里，Memory的实现方式是把历史对话存储和检索集成到框架里，开发者不需要自己写存储逻辑。

## Key Emphasis
必须清晰界定Memory和Knowledge的边界（记人 vs 记事）。
重点展开短期记忆（Session/Redis）和长期记忆（Mem0/Zep/向量DB）两种架构。
让学员知道实际开发中有哪些工具可以用。

## Optional Extension
如果学员问Memory和Context Window的关系，回答：Context Window是模型一次能读的容量上限，Memory是需要塞进这个容量里的内容。Memory太多会挤占其他信息，所以需要策略性地保留关键记忆、压缩或丢弃旧记忆。

## Classroom Interaction
无。

## Take Away
Memory让Agent记住用户。短期记忆用Session/Redis，长期记忆用Mem0/Zep/向量DB。

## Why This Slide Matters
很多开发者容易把Memory和RAG混淆。这页划清界限，同时给出具体的技术方案，让学员知道回去怎么落地。

## Transition
有了Memory，Agent记住了你是谁。但如果用户问"你们新款路由器怎么配置？"，大模型不知道，因为训练数据里没有。这就需要第三个能力：Knowledge与RAG。
