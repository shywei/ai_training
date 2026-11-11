## Slide Objective
深入剖析大模型推理的两阶段物理本质：Prefill 阶段的计算密集（Compute-bound）与 Decode 阶段的访存密集（Memory-bound）；厘清 KV Cache 空间换时间的数学机理，并推导 KV Cache 随并发与上下文长度爆炸的显存定量计算公式。

## Suggested Presentation
在企业落地大模型推理服务时，工程师经常遇到两个困惑：
第一，为什么明明 GPU 算力利用率显示只有 20%，但服务却卡成 PPT，每秒只能吐出几个字？
第二，为什么模型文件只有 14GB，但跑着跑着 80GB 的 A100 显存突然 OOM（内存溢出）崩掉了？
搞懂这两个问题，就必须掌握 Prefill、Decode 与 KV Cache 的底层关系。

请看上方卡片 1：Prefill 阶段（输入评估阶段）。
当用户发送一段 2000 字的 Prompt 时，模型内部是一次性把这 2000 个 Token 并行灌进 GPU 的。此时做的是大矩阵乘大矩阵（GEMM），GPU 的 Tensor Core 能够以接近理论上限的效率满负荷运转。这个阶段只发生一次，决定了用户点击“发送”后需要等待多久才能看到第一个字，也就是首字延迟（TTFT）。

请看上方卡片 2：Decode 阶段（逐字生成阶段）。
第一个字生成后，系统进入自回归循环，每次只生成 1 个新词。此时模型做的是大矩阵乘以单向量（GEMV）。在每一个词生成的微秒级瞬间，GPU 必须把数十层网络数百亿参数的权重、以及历史上所有词的 KV 缓存，全部从 HBM 显存通过物理总线搬到流式多处理器（SM）中算一遍！算力核心瞬间算完后，又得傻傻等待下一次从显存里搬运数据。因此，Decode 阶段的算力利用率极低，系统性能完全被显存带宽死死卡住（Memory-bound），显存带宽多宽，每秒吐字速度（TPS）就有多快！

为了避免每次生成一个新词都把前面几千个历史词从头算一遍（否则复杂度会达到 $O(N^2)$），我们使用了下方底部的 KV Cache（键值缓存）。
KV Cache 将过去算好的 Key 和 Value 矩阵常驻在显存里，单步计算复杂度降到了 $O(1)$，实现了空间换时间。
但大家请看这个显存公式：
$\text{VRAM}_{KV} = 2 \times 2 \times L \times H_{kv} \times d_{head} \times \text{Context\_Len} \times \text{Batch\_Size}$ 字节。
第一个 2 代表 Key 和 Value 两个矩阵；第二个 2 代表 16 位浮点数占 2 字节。
请注意：它是随着上下文长度（Context Length）和并发会话数（Batch Size）线性膨胀的！
在推演案例中，一个标准的 70B 模型，当并发达到 16、上下文拉长到 32K 时，光是存放 KV Cache 就需要吃掉 67GB 显存，直接超过了 INT4 量化后的权重显存！如果不做系统级优化，长文本并发推理根本不可能上线。

## Key Q&A / Clarifications
**Q：在云厂商的 API 计费中，为什么输入 Token（Prompt）的价格通常比输出 Token（Completion）便宜很多？**
A：这正是底层硬件物理特性的直接映射！Prefill 阶段（输入 Token）是高度并行的 GEMM 运算，GPU 算力利用率极高，计算成本低；而 Decode 阶段（输出 Token）是极度消耗显存带宽且计算单元大量空转的 GEMV 串行循环，耗费的 GPU 物理机时和带宽成本远高于输入阶段。

## Transition
第二章我们把推理计算单元从 Tokenizer、RoPE、GQA 到 KV Cache 彻底拆解完毕。那么，一个具备数十亿参数的神经网络，最初是如何被一步步“炼”出来的？让我们进入第三章 P10：2026 现代大模型四阶段训练流水线全景。