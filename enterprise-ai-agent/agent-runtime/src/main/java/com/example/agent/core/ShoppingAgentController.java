package com.example.agent.core;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 导购 Agent API
 *
 * v1.0-prompt 验证目标：
 * - [x] Streaming 输出
 * - [x] 模型路由（Fast vs Reasoning）
 * - [x] 结构化输出（JSON Schema）
 * - [x] Self-Correction（输出非法时自动重试）
 * - [x] Guardrails（注入防御 + 敏感信息过滤）
 */
@RestController
@RequestMapping("/api/agent")
public class ShoppingAgentController {

    private final ChatModel chatModel;
    private final ModelRouter modelRouter;
    private final PromptManager promptManager;
    private final StructuredOutputHandler outputHandler;
    private final GuardrailsService guardrails;

    public ShoppingAgentController(ChatModel chatModel,
                                   ModelRouter modelRouter,
                                   PromptManager promptManager,
                                   StructuredOutputHandler outputHandler,
                                   GuardrailsService guardrails) {
        this.chatModel = chatModel;
        this.modelRouter = modelRouter;
        this.promptManager = promptManager;
        this.outputHandler = outputHandler;
        this.guardrails = guardrails;
    }

    /**
     * 流式对话接口
     * GET /api/agent/chat?message=帮我推荐一台笔记本
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam String message) {
        // Guardrails: 检查 Prompt Injection
        if (guardrails.checkInjection(message)) {
            return Flux.just("检测到不安全的输入，请重新提问。");
        }

        // 模型路由：选择用哪个模型
        String model = modelRouter.selectModel(message);

        // 构建 Prompt
        String systemPrompt = promptManager.buildSystemPrompt();
        String userPrompt = promptManager.buildUserPrompt(message, null);

        // 调用 LLM（Streaming）
        return ChatClient.create(chatModel)
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(OpenAiChatOptions.builder()
                        .withModel(model)
                        .build())
                .stream()
                .content()
                .map(chunk -> {
                    // Guardrails: 过滤敏感信息
                    return guardrails.sanitize(chunk);
                });
    }

    /**
     * 结构化输出接口（带 Self-Correction）
     * POST /api/agent/recommend
     * 返回符合 JSON Schema 的商品推荐
     */
    @PostMapping("/recommend")
    public String recommend(@RequestBody String userNeed) {
        // Guardrails: 检查 Prompt Injection
        if (guardrails.checkInjection(userNeed)) {
            return "{\"error\":\"检测到不安全的输入\"}";
        }

        String model = modelRouter.selectModel(userNeed);
        String systemPrompt = promptManager.buildSystemPrompt() + """

                ## 输出格式
                严格按以下 JSON 格式输出，不要添加任何其他文字：
                {
                  "recommendation": "总体推荐理由",
                  "products": [
                    {"name": "商品名", "reason": "推荐理由", "price_range": "价格区间"}
                  ],
                  "confidence": 0.9
                }
                """;
        String userPrompt = promptManager.buildUserPrompt(userNeed, null);

        // 第一次调用
        String rawOutput = callLLM(systemPrompt, userPrompt, model);

        // Output Validation
        var json = outputHandler.validate(rawOutput);
        if (json != null) {
            // 验证通过
            return guardrails.sanitize(rawOutput);
        }

        // Self-Correction：第一次输出非法，让 LLM 自我纠错
        String correctionPrompt = outputHandler.buildCorrectionPrompt(rawOutput, "输出不是合法 JSON");
        String correctedOutput = callLLM(systemPrompt, correctionPrompt, model);

        json = outputHandler.validate(correctedOutput);
        if (json != null) {
            return guardrails.sanitize(correctedOutput);
        }

        // 两次都失败，返回错误
        return "{\"error\":\"模型输出无法解析，请重试\"}";
    }

    /**
     * 非流式调用 LLM
     */
    private String callLLM(String systemPrompt, String userPrompt, String model) {
        return ChatClient.create(chatModel)
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(OpenAiChatOptions.builder()
                        .withModel(model)
                        .withTemperature(0.2)
                        .build())
                .call()
                .content();
    }
}
