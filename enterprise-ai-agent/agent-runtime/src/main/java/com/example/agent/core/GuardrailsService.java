package com.example.agent.core;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Guardrails 服务：输出过滤与安全检查
 *
 * 第四课核心知识点：
 * - Prompt Injection 防御
 * - 输出内容过滤
 * - 敏感信息拦截
 */
@Component
public class GuardrailsService {

    // 敏感关键词（简化示例，实际应从配置读取）
    private static final List<String> SENSITIVE_KEYWORDS = List.of(
            "密码", "password", "secret", "token", "私钥"
    );

    // Prompt Injection 模式
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)ignore.*previous.*instructions"),
            Pattern.compile("(?i)你是一个.*而不是"),
            Pattern.compile("(?i)system\\s*prompt")
    );

    /**
     * 检查用户输入是否有 Prompt Injection 风险
     */
    public boolean checkInjection(String userInput) {
        if (userInput == null) return false;
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(userInput).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查 LLM 输出是否包含敏感信息
     */
    public boolean containsSensitiveInfo(String llmOutput) {
        if (llmOutput == null) return false;
        String lower = llmOutput.toLowerCase();
        for (String keyword : SENSITIVE_KEYWORDS) {
            if (lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 对 LLM 输出进行脱敏处理
     */
    public String sanitize(String llmOutput) {
        if (llmOutput == null) return null;
        String result = llmOutput;
        for (String keyword : SENSITIVE_KEYWORDS) {
            result = result.replaceAll("(?i)" + keyword, "***");
        }
        return result;
    }
}
