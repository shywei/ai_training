package com.example.agent.adapter.springai2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Spring AI 2.0.2 GA 安全守卫与拦截器 (GuardrailsCallAdvisor)
 *
 * 严格实现 Spring AI 2.0.2 GA 官方接口 org.springframework.ai.chat.client.advisor.api.CallAdvisor
 * （替代早期 1.0-M4 的 CallAroundAdvisor）：
 * - 前置拦截：Prompt Injection 拦截、用户输入 PII 脱敏；
 * - 后置拦截：企业敏感词审查、密码/Token 泄露脱敏、调用耗时打点。
 */
@Component
public class GuardrailsCallAdvisor implements CallAdvisor {

    private static final Logger log = LoggerFactory.getLogger(GuardrailsCallAdvisor.class);

    /** 最高优先级执行前置风控拦截 (Order 最小优先) */
    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 100;

    private static final List<String> SENSITIVE_KEYWORDS = List.of(
            "密码", "password", "secret", "token", "私钥", "内部接口", "rootkey"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("\\b\\d{17}[\\dXx]\\b");

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)ignore.*previous.*instructions"),
            Pattern.compile("(?i)system\\s*prompt"),
            Pattern.compile("(?i)forget.*all.*rules"),
            Pattern.compile("(?i)you are now.*DAN"),
            Pattern.compile("忽略.*(?:之前|上面|以前).*(?:指令|提示|规则|设定)"),
            Pattern.compile("(?:告诉我|显示|输出|泄露).*(?:系统提示|System Prompt|初始指令)"),
            Pattern.compile("(?:你是一个|你现在是).*(?:黑客|管理员|开发者|DAN)"),
            Pattern.compile("(?:忘记|抛弃|无视).*(?:规则|限制|约束|指令)")
    );

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        long startTime = System.currentTimeMillis();
        UserMessage userMsg = request.prompt().getUserMessage();
        String userText = userMsg != null ? userMsg.getText() : "";

        // 1. 【前置拦截】Prompt Injection 检测
        if (checkInjection(userText)) {
            log.warn("[GuardrailsAdvisor] ⚠️ 拦截到潜在的 Prompt 注入攻击: {}", userText);
            throw new SecurityException("检测到违规输入或 Prompt 注入风险，已被安全拦截！");
        }

        // 2. 【前置改写】PII 敏感个人隐私脱敏（手机号、身份证打码）
        String sanitizedInput = sanitizePii(userText);
        if (!sanitizedInput.equals(userText)) {
            log.info("[GuardrailsAdvisor] 前置 PII 脱敏处理: {} -> {}", userText, sanitizedInput);
            Prompt updatedPrompt = request.prompt().augmentUserMessage(old -> new UserMessage(sanitizedInput));
            request = request.mutate().prompt(updatedPrompt).build();
        }

        // 3. 【放行下游】推进 Advisor 责任链或进入底层 ChatModel 调用
        ChatClientResponse response = chain.nextCall(request);

        // 4. 【后置审查】输出敏感信息过滤与耗时审计
        String rawOutput = extractResponseText(response);
        String sanitizedOutput = sanitizeOutput(rawOutput);
        long duration = System.currentTimeMillis() - startTime;

        log.info("[GuardrailsAdvisor] 后置拦截审计完成，耗时: {}ms", duration);

        if (!sanitizedOutput.equals(rawOutput) && response.chatResponse() != null) {
            ChatResponse maskedChatResp = new ChatResponse(
                    List.of(new Generation(new AssistantMessage(sanitizedOutput))),
                    response.chatResponse().getMetadata()
            );
            return response.mutate()
                    .chatResponse(maskedChatResp)
                    .context("guardrails_checked", true)
                    .context("latency_ms", duration)
                    .build();
        }

        return response.mutate()
                .context("guardrails_checked", true)
                .context("latency_ms", duration)
                .build();
    }

    private String extractResponseText(ChatClientResponse response) {
        if (response == null || response.chatResponse() == null || response.chatResponse().getResult() == null) {
            return "";
        }
        AssistantMessage output = response.chatResponse().getResult().getOutput();
        return output != null && output.getText() != null ? output.getText() : "";
    }

    public boolean checkInjection(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    public String sanitizePii(String text) {
        if (text == null) return "";
        String masked = PHONE_PATTERN.matcher(text).replaceAll(m -> {
            String p = m.group();
            return p.substring(0, 3) + "****" + p.substring(7);
        });
        masked = ID_CARD_PATTERN.matcher(masked).replaceAll(m -> {
            String id = m.group();
            return id.substring(0, 6) + "********" + id.substring(14);
        });
        return masked;
    }

    public String sanitizeOutput(String text) {
        if (text == null) return "";
        String result = text;
        for (String keyword : SENSITIVE_KEYWORDS) {
            result = result.replaceAll("(?i)" + Pattern.quote(keyword), "***");
        }
        return result;
    }
}
