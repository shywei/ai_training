## Slide Objective
讲透 Embedding 的原理、模型选型、维度概念，以及踩坑经验。学员需要理解"Embedding 是 RAG 的核心能力，但不是万能的"。

## Suggested Presentation
Embedding 就是把文本变成一组数字（向量）。语义相近的文本，数字也相近。这不是魔法，是数学——通过大量文本训练出来的语义理解能力。

看向量空间示意。ThinkPad X1 Carbon、商务轻薄笔记本、MacBook Pro 在向量空间中距离很近——因为它们都是笔记本相关。AirPods Pro、降噪耳机距离也很近——因为它们都是耳机相关。但笔记本和耳机之间距离很远。这就是语义检索的基础：通过计算向量距离，找到语义相近的文本。

"写代码"和"编程"在向量空间中距离很近——Embedding 模型理解它们是同一个意思。用户问"推荐一台写代码的笔记本"，向量检索能匹配到包含"适合编程开发"的商品描述。这是关键词匹配做不到的。

模型选型看左边的三个方案。BGE-large-zh 是国内团队做的，中文效果最好，SiliconFlow 免费调用，1024 维。ada-002 是 OpenAI 的，通用但要付费，1536 维。Jina 多语言支持好，有免费额度。中文场景首选 BGE——免费、中文效果好、SiliconFlow 直接支持。

选型维度：成本（免费 vs 付费）、语言支持（中文 vs 英文 vs 多语言）、维度大小（影响存储和检索速度）、部署方式（API vs 本地）。

看右边的代码。Demo 用的是 BGE-large-zh-v1.5，通过 SiliconFlow API 调用。Spring AI 的 `EmbeddingModel` 接口封装了调用细节，代码里只需要调 `embeddingModel.embed(text)` 就能拿到向量。

踩坑经验很重要。Embedding 维度必须和数据库表定义匹配。BGE = 1024 维，所以表定义是 `vector(1024)`。如果换模型（比如 ada-002 是 1536 维），需要 DROP TABLE 重建。维度写错了不报错，查不出来——这是最隐蔽的坑。学员做项目时，第一件事就是确认 Embedding 模型的维度，然后和表定义对齐。

## Key Emphasis
Embedding 是 RAG 的核心能力。但也不要神化它——对专业术语（型号、SKU）的理解不如关键词匹配，这就是后面要讲 Hybrid Search 的原因。

## Demo 与生产的差距
Demo 没有对比不同 Embedding 模型的效果。生产中应该用评测集测试 BGE vs Jina vs ada-002 的 Recall/Precision，选最优的。Demo 也没有做 Embedding 模型微调——领域专有名词（如"X1 Carbon"、"RTX 4070"）可能需要微调才能正确理解。

## Transition
文本变成了向量，存在哪？怎么建索引？怎么查？
