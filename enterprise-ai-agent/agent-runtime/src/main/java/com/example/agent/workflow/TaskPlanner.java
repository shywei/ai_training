package com.example.agent.workflow;

import com.example.agent.core.StructuredOutputHandler;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务规划器
 *
 * 用 LLM 把复杂请求拆成可执行的步骤列表。
 * 这是 Agent 和 Chatbot 的本质区别——
 * Chatbot 只回答问题，Agent 规划"该做哪些事"。
 *
 * 示例：
 * "帮我对比拯救者和MacBook的价格，预算8000"
 * → [
 *     {step: 1, action: "搜索拯救者笔记本价格和库存"},
 *     {step: 2, action: "搜索MacBook价格和库存"},
 *     {step: 3, action: "对比分析，结合预算推荐"}
 *   ]
 *
 * 教学点：LLM 不仅能回答问题，还能规划执行步骤。
 * TaskPlanner 是 Agent 的"大脑前额叶"。
 */
@Component
public class TaskPlanner {

    private static final Logger log = LoggerFactory.getLogger(TaskPlanner.class);

    private final ChatModel chatModel;
    private final String planningModel;
    private final StructuredOutputHandler outputHandler;

    public TaskPlanner(ChatModel chatModel,
                       @Value("${agent.model.reasoning}") String planningModel,
                       StructuredOutputHandler outputHandler) {
        this.chatModel = chatModel;
        this.planningModel = planningModel;
        this.outputHandler = outputHandler;
    }

    /**
     * 判断任务是否需要规划（多步骤）
     *
     * 简单任务（问候、单次查询）不需要规划，
     * 复杂任务（对比、多商品、分析）需要拆步骤。
     */
    public boolean needsPlanning(String userMessage) {
        // 多步关键词
        String[] multiStepKeywords = {
                "对比", "比较", "帮我选", "推荐几个", "哪些适合",
                "然后", "同时", "还要", "顺便",
                "区别", "差异", "分析",
                "compare", "recommend several", "which ones"
        };

        for (String keyword : multiStepKeywords) {
            if (userMessage.contains(keyword)) {
                return true;
            }
        }

        // 包含多个商品名也可能是多步
        int productMentionCount = countProductMentions(userMessage);
        if (productMentionCount >= 2) {
            return true;
        }

        return false;
    }

    /**
     * 规划任务步骤
     *
     * @return 步骤列表，如果规划失败返回空列表（降级为单步执行）
     */
    public List<WorkflowStep> plan(String userMessage, String context) {
        String prompt = """
                你是一个任务规划器。请将用户的请求分解为可执行的步骤列表。
                
                每个步骤应该是一个独立的、可验证的动作。
                步骤可以是：
                - 调用工具查询信息（如查询商品价格、库存）
                - 检索知识库获取商品信息
                - 分析和对比已有信息
                - 给出最终建议或回答
                
                用户请求：%s
                
                %s
                
                请用 JSON 数组格式输出步骤列表，每个步骤包含：
                - step: 步骤编号（从 1 开始）
                - action: 动作描述（简洁明确）
                
                只输出 JSON 数组，不要用代码块包裹，不要添加任何解释。
                示例：[{"step":1,"action":"查询拯救者笔记本价格"},{"step":2,"action":"对比分析推荐"}]
                """.formatted(userMessage,
                context != null && !context.isBlank()
                        ? "已有上下文信息：\n" + context
                        : "");

        try {
            log.info("[TaskPlanner] 规划任务: {}", userMessage);

            String result = ChatClient.create(chatModel)
                    .prompt()
                    .user(prompt)
                    .options(OpenAiChatOptions.builder()
                            .withModel(planningModel)
                            .withTemperature(0.2)
                            .build())
                    .call()
                    .content();

            if (result == null || result.isBlank()) {
                log.warn("[TaskPlanner] LLM 返回空结果，降级为单步");
                return List.of();
            }

            // 解析 JSON 数组
            String json = outputHandler.extractJson(result);
            if (json == null) {
                log.warn("[TaskPlanner] 无法提取 JSON: {}", result);
                return List.of();
            }

            // 确保是数组格式（LLM 常见输出变体兜底）
            if (!json.startsWith("[")) {
                // 变体1: {"steps": [...]} 包装格式
                JsonNode root = outputHandler.validate(json);
                if (root != null && root.has("steps") && root.get("steps").isArray()) {
                    json = root.get("steps").toString();
                } else {
                    // 变体2: 多个平铺对象 {...},{...}（LLM 常丢外层数组）→ 自动补 []
                    String wrapped = "[" + json + "]";
                    if (outputHandler.validate(wrapped) != null) {
                        log.info("[TaskPlanner] 检测到平铺对象格式，已自动补全数组括号");
                        json = wrapped;
                    } else {
                        log.warn("[TaskPlanner] JSON 非数组格式: {} | LLM 原始返回: {}", json, result);
                        return List.of();
                    }
                }
            }

            JsonNode stepsNode = outputHandler.validate(json);
            if (stepsNode == null || !stepsNode.isArray()) {
                log.warn("[TaskPlanner] 解析失败: {} | LLM 原始返回: {}", json, result);
                return List.of();
            }

            List<WorkflowStep> steps = new ArrayList<>();
            for (JsonNode stepNode : stepsNode) {
                int stepNum = stepNode.has("step") ? stepNode.get("step").asInt() : steps.size() + 1;
                String action = stepNode.has("action") ? stepNode.get("action").asText() : "未知操作";
                steps.add(new WorkflowStep(stepNum, action));
            }

            log.info("[TaskPlanner] 规划完成: {} 个步骤", steps.size());
            for (WorkflowStep step : steps) {
                log.info("[TaskPlanner]   Step {}: {}", step.getStepNumber(), step.getAction());
            }

            return steps;

        } catch (Exception e) {
            log.error("[TaskPlanner] 规划失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 统计消息中可能的商品名数量（简单启发式）
     */
    private int countProductMentions(String message) {
        String[] productKeywords = {
                "拯救者", "ThinkPad", "MacBook", "MateBook", "小新",
                "华为", "小米", "联想", "戴尔", "惠普", "iPhone", "iPad"
        };
        int count = 0;
        for (String keyword : productKeywords) {
            if (message.contains(keyword)) {
                count++;
            }
        }
        return count;
    }
}