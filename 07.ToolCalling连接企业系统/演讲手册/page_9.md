## Slide Objective
讲解 MCP 的四大核心概念。

## Suggested Presentation
MCP 定义了四大能力：

Tool——LLM 可调用的函数。搜索、查询、写入。LLM 决定何时调用。Demo 中的 search_knowledge、queryProductInfo 都是 Tool。

Resource——Client 可读取的数据。文件、配置、上下文。Client 主动读取。Demo 中的 user://{userId} 用户画像是 Resource。

Prompts——预定义的 Prompt 模板。Server 提供，Client 选择使用。Demo 未覆盖。

Sampling——Server 请求 Client 调用 LLM。服务端编排场景。Demo 未覆盖。

Demo 覆盖了 Tool 和 Resource 两个核心概念。Prompts 和 Sampling 是扩展能力，企业场景中会用到。

## Key Emphasis
Tool 和 Resource 是最核心的两个概念。Tool 是"做事"，Resource 是"读数据"。后面有专门一页讲选型。

## Take Away
MCP 四大能力：Tool（做事）、Resource（读数据）、Prompt（模板）、Sampling（编排）。

## Why This Slide Matters
没有这页，学员只知道 Tool 不知道 Resource，不知道 MCP 的完整能力。

## Transition
知道了概念，MCP 的技术架构是什么样的？
