## Slide Objective
深入剖析 Self-Attention 自注意力机制的核心数学矩阵乘法与点积缩放 $\sqrt{d_k}$ 的工程意义；横向对比从 Multi-Head Attention（MHA）到 Multi-Query Attention（MQA）再到现代黄金标准 Grouped-Query Attention（GQA）的演进脉络与显存优化原理。

## Suggested Presentation
大家在各种科普里都听说过“注意力机制”，但作为工程开发人员，我们必须看懂它的矩阵运算本质。

请看上方卡片中的经典公式：$\text{Attention}(Q, K, V) = \text{Softmax}\left(\frac{Q K^T}{\sqrt{d_k}}\right) V$。
这其实就是三步高度规整的矩阵乘法：
第一步，计算关联度：把 Query 矩阵与 Key 转置矩阵做点积。每一个 Token 的 Query 向量就像一个雷达探针，去探测序列中所有 Token 的 Key 索引，点积越大说明两个词的语义关联度越强；
第二步，为什么公式里一定要除以 $\sqrt{d_k}$？很多同学在面试中答不上来。其实这是为了避免数值弥散！假设每个注意力头的维度 $d_k = 128$，两个均值为 0、方差为 1 的 128 维独立随机向量做点积，其方差会飙升到 128！极大的数值直接喂给 Softmax，会导致最大的那个数概率无限逼近 1，其余所有数全部无限逼近 0，反向传播的梯度直接归零饱和。除以 $\sqrt{d_k}$ 正好把方差重新规范化回 1.0；
第三步，加权求和：把 Softmax 归一化后的注意力概率矩阵与 Value 矩阵相乘，完成上下文信息的动态聚合。

然而，在生产部署中，原始的 MHA（多头注意力）遇到了巨大的灾难。请看下方三根柱子的演进：
在 MHA 中，如果有 32 个 Query 头，就必须配套 32 个独立的 Key 头和 32 个 Value 头。当上下文拉长到 32K、128K 时，这 32 对 KV 矩阵必须完整保存在显存（KV Cache）里，显存直接被撑爆；
为了解决这个问题，学术界提出了 MQA（多查询注意力），让所有 32 个 Query 头共享同一对 KV 头，显存直接砍掉 32 倍！但 MQA 过于激进，在复杂数理和代码生成任务上表达能力有所下降；
最终，工业界找到了完美的平衡点——GQA（分组查询注意力）。例如 32 个 Query 头分成 8 组，每 4 个 Query 头共享 1 对 KV 头（8 个 Key 头和 8 个 Value 头）。实验表明，GQA 的生成质量与原始 MHA 几乎完全无差，却一举削减了 75% 的 KV Cache 显存占用与显存带宽搬运量！因此，今天主流开源模型（LLaMA-3、Qwen-2.5、Mistral）已全量切换为 GQA。

## Key Q&A / Clarifications
**Q：在查看 HuggingFace 上的 `config.json` 时，哪个参数能看出模型是否使用了 GQA？**
A：观察两个参数：`num_attention_heads`（Query 头数）和 `num_key_value_heads`（KV 头数）。如果两者相等（比如都是 32），说明是传统的 MHA；如果 `num_key_value_heads: 8` 而 `num_attention_heads: 32`，就说明采用了 4:1 分组的 GQA 架构。

## Transition
自注意力机制完成了上下文向量的特征提取，经过多层堆叠后，模型输出了词表中每个 Token 的预测得分 Logits。那么，系统是如何根据这些得分采样出下一个词的？我们进入 P08：自回归采样解码的数学本质。