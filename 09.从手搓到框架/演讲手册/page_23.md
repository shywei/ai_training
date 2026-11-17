# 第 23 页：2026 企业 AI 技术栈三层生态坐标与需求选型决策指南

## Slide Objective
梳理 L1 模型通信底座、L2 智能体运行时与 L3 低代码平台三层生态坐标，阐明为何选择稳定的 AgentScope 2.0 GA 而非 Milestone 阶段的 Spring AI Alibaba，并给出一张可落地的企业项目需求选型决策表。

## Suggested Presentation
各位同学，学到这里，我们把视野拉开到整个企业 AI 技术栈生态。平时大家听到 Spring AI、LangChain4j、AgentScope、Embabel、LangGraph、Dify 这些名字总觉得眼花缭乱，其实它们根本不是同一个层面的东西，而是清晰的三层坐标（看左栏）：

1. **L1 · 模型通信底座层（Spring AI 2.0 / LangChain4j）**：类比 JDBC / Servlet API。解决协议统一、`ChatClient`、`@Tool`、单次请求 `Advisor` 切面；轻量标准，不内置多步状态机。
2. **L2 · 智能体运行时层（AgentScope 2.0 / Embabel / LangGraph）**：类比 Spring MVC / 状态引擎。内置 `ReActAgent` 多步循环、认知生命周期 `Middleware`、多通道事件流与 `StateStore` 状态管理。
3. **L3 · 低代码平台层（Dify / Coze / 百炼）**：类比可视化 BPM。适合运营快速搭 FAQ 或做 POC 原型，但很难深度嵌入企业核心微服务事务和内网复杂权限体系。

这里顺带回答一个高频选型问题：**为什么阿里生态里我们选了 AgentScope Java 2.0.0 GA，而明确放弃了 Spring AI Alibaba 2.0？**
两个硬原因：第一，版本成熟度——Spring AI Alibaba 2.0 目前仍处于 Milestone 预发布阶段，API 变动大；而 AgentScope Java 2.0 已经发布正式稳定 GA；第二，架构定位——Spring AI Alibaba 偏向阿里云生态适配，而 AgentScope 2.0 专注通用 Agent 运行时与多智能体能力，正好命中我们第 09、10 课的核心痛点。

最后看右栏的**实战选型决策树**：
- **什么时候直接选 L1（Spring AI 2.0）就够了？** 单轮/少轮问答、标准 RAG（检索➔生成）、或者传统 Java 代码控制主流程、仅在某一步调 1~2 个只读工具。
- **什么时候必须引入 L2（AgentScope 2.0）？** 需要动态多步 ReAct 规划、高危工具执行前风控挂起 HITL、跨进程断点复水、以及思考/工具/正文多通道实时 UI。

## Key Emphasis
1. **破除“横向乱比”**：L1（通信底座）、L2（应用运行时）、L3（低代码）是分层协作关系，不是互斥替代品。
2. **版本选型铁律**：企业生产核心链路永远优先选 GA 稳定版，并用六边形架构隔离。

## Optional Extension
### Q1: 在真实企业项目里，L1（Spring AI）和 L2（AgentScope）可以混用吗？
**讲师解析**：完全可以按场景分层混用。例如企业内部的“通用 AI 网关 / 基础 RAG 服务”用 Spring AI 2.0 最轻量标准；而复杂的“导购下单 / 退款仲裁”智能体微服务用 AgentScope 2.0 接管多步运行时与长链路状态，两者统一遵循六边形端口和标准协议通信。

## Classroom Interaction
“如果你们公司要做一个‘内部制度文档检索问答机器人’，只查知识库不下单不挂起，你会选 L1、L2 还是 L3？”（引导学员根据团队研发能力与系统集成度在 L1 和 L3 之间合理权衡，避免盲目上 L2）。

## Take Away
先定层级再选框架：简单问答/RAG 用 L1（Spring AI 2.0），复杂长链路与 HITL 上 L2（AgentScope 2.0），核心链路坚守 GA 版本。

## Why This Slide Matters
把整节课的技术认知收敛为一线架构师立项评审时直接可用的技术选型决断框架。

## Transition
明确了技术生态坐标与选型指南后，下面我们用最后一页 P24 对第 09 课做一次完整的闭环总结。
