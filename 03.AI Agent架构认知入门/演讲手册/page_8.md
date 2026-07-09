## Slide Objective
解释Knowledge与RAG的存在意义，让学员了解RAG的完整技术栈和行业主流方案。

## Suggested Presentation
Memory记住了用户，但如果用户问"你们新款路由器怎么配置？"，大模型肯定不知道——训练数据是旧的，也不可能包含你们公司的内部文档。

能不能把所有文档全塞给大模型？不行，Context Window有限，塞不下。

所以我们需要RAG——检索增强生成。思路很简单：Agent遇到不懂的知识，先去资料库里查字典，找到最相关的几段文字，然后拿着这些段落去问大模型。大模型有推理能力，但不一定有你的知识；RAG补上了这块短板。

那RAG在工程上怎么做？大家看这条流水线。
第一步，把企业文档切成小段（Chunking）。
第二步，用Embedding模型把每段文字转成一个向量——你可以理解为给每段文字打上一组多维度的坐标标签。
第三步，把向量存进专门的数据库，叫向量数据库（Vector DB）。
第四步，用户提问时，把问题也转成向量，去向量数据库里找最相似的几段文字（Retrieval）。
第五步，如果需要更精准，可以加一层ReRank（重排序），把最相关的排在前面。
最后，把检索到的段落和用户问题一起交给大模型生成回答。

目前行业里做RAG的主流方案有哪些？
Embedding模型方面，OpenAI的text-embedding系列是标杆，国内BGE和Jina也很常用，开源免费。
向量数据库方面，Milvus是国内用得最多的开源向量数据库，性能强；PgVector是PostgreSQL的扩展，适合已有PG基础设施的企业；Chroma轻量级，适合快速原型。
框架方面，LlamaIndex是Python生态里最成熟的RAG框架，Spring AI RAG是Java生态的首选，提供了开箱即用的RAG流程。
如果需要提升检索精度，可以加ReRanker，比如Cohere Rerank或者BGE Reranker。

这里再强调一个边界：Knowledge查的是静态文档（说明书、政策），Tool调的是实时API（天气、库存）。两者不能互换。

## Key Emphasis
重点讲RAG的完整流水线：文档切分→Embedding→向量存储→检索→ReRank→生成。
逐层展开行业方案：Embedding模型（OpenAI/BGE/Jina）、向量数据库（Milvus/PgVector/Chroma）、RAG框架（LlamaIndex/Spring AI RAG）、ReRanker。
强调Knowledge和Tool的区别。

## Optional Extension
如果学员问"需不需要自己训练Embedding模型"，回答：大多数场景不需要。通用的Embedding模型已经足够好。只有在非常专业的领域（比如医疗、法律术语），才需要考虑微调。

## Classroom Interaction
无。

## Take Away
RAG让Agent具备查资料的能力。核心流水线：文档→Embedding→向量DB→检索→ReRank→LLM生成。

## Why This Slide Matters
RAG是目前企业AI落地最核心的技术方向。如果只讲概念不讲技术栈，学员回去还是不知道怎么动手。

## Transition
认知层的三个能力讲完了。现在Agent能思考了，但如果它想真正干活——比如帮用户查快递、下单——它还需要"手和脚"。接下来进入执行层，看看Tool和MCP。
