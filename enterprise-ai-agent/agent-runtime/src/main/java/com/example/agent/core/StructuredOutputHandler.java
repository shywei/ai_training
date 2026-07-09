package com.example.agent.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * 结构化输出处理器：确保 LLM 输出符合预期的 JSON Schema
 *
 * 第四课核心知识点：
 * - Java 无法解析带有废话的 JSON
 * - Output Validation：验证 LLM 输出是否符合 Schema
 * - Self-Correction：输出不合法时让 LLM 自我纠错
 */
@Component
public class StructuredOutputHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 验证 LLM 输出是否为合法 JSON
     * @return 解析后的 JsonNode，如果非法返回 null
     */
    public JsonNode validate(String llmOutput) {
        if (llmOutput == null || llmOutput.isBlank()) {
            return null;
        }
        // 提取 JSON（LLM 可能会在 JSON 前后加废话）
        String json = extractJson(llmOutput);
        if (json == null) return null;

        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 LLM 输出中提取 JSON 部分
     * 处理情况：LLM 可能在 JSON 前后添加说明文字
     */
    private String extractJson(String text) {
        text = text.trim();
        // 如果本身就是 JSON
        if (text.startsWith("{") && text.endsWith("}")) {
            return text;
        }
        // 尝试提取 ```json ... ``` 代码块
        int start = text.indexOf("```json");
        if (start >= 0) {
            start = text.indexOf("\n", start) + 1;
            int end = text.indexOf("```", start);
            if (end > start) {
                return text.substring(start, end).trim();
            }
        }
        // 尝试找第一个 { 到最后一个 }
        int firstBrace = text.indexOf("{");
        int lastBrace = text.lastIndexOf("}");
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return text.substring(firstBrace, lastBrace + 1);
        }
        return null;
    }

    /**
     * 构建 Self-Correction 提示词
     * 当 LLM 输出非法 JSON 时，将异常信息发回让它修正
     */
    public String buildCorrectionPrompt(String originalOutput, String errorMessage) {
        return String.format("""
                你上次的输出不是合法的 JSON，解析报错：
                %s
                
                你上次的输出：
                %s
                
                请重新输出，只输出 JSON，不要添加任何其他文字。
                """, errorMessage, originalOutput);
    }
}
