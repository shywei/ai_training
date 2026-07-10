package com.example.agent.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * 结构化输出处理器：确保 LLM 输出符合预期的 JSON Schema
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
        String json = extractJson(llmOutput);
        if (json == null) return null;

        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            // 尝试清理常见问题后重试
            String cleaned = cleanJson(json);
            try {
                return objectMapper.readTree(cleaned);
            } catch (JsonProcessingException e2) {
                return null;
            }
        }
    }

    /**
     * 从 LLM 输出中提取 JSON 部分
     */
    public String extractJson(String text) {
        if (text == null) return null;
        text = text.trim();

        // 1. 尝试直接解析
        if (text.startsWith("{") && text.endsWith("}")) {
            return text;
        }

        // 2. 提取 ```json ... ``` 代码块
        int codeStart = text.indexOf("```json");
        if (codeStart >= 0) {
            codeStart = text.indexOf("\n", codeStart) + 1;
            int codeEnd = text.indexOf("```", codeStart);
            if (codeEnd > codeStart) {
                return text.substring(codeStart, codeEnd).trim();
            }
        }

        // 3. 提取 ``` ... ``` 代码块（无 json 标记）
        codeStart = text.indexOf("```");
        if (codeStart >= 0) {
            codeStart = text.indexOf("\n", codeStart) + 1;
            int codeEnd = text.indexOf("```", codeStart);
            if (codeEnd > codeStart) {
                String candidate = text.substring(codeStart, codeEnd).trim();
                if (candidate.startsWith("{")) {
                    return candidate;
                }
            }
        }

        // 4. 找第一个 { 到最后一个 }
        int firstBrace = text.indexOf("{");
        int lastBrace = text.lastIndexOf("}");
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return text.substring(firstBrace, lastBrace + 1);
        }

        return null;
    }

    /**
     * 清理 JSON 中的常见问题
     */
    private String cleanJson(String json) {
        // 移除尾部逗号
        json = json.replaceAll(",\\s*([}\\]])", "$1");
        // 移除注释
        json = json.replaceAll("//.*?\\n", "\n");
        return json;
    }

    /**
     * 构建 Self-Correction 提示词
     */
    public String buildCorrectionPrompt(String originalOutput, String errorMessage) {
        return String.format("""
                你上次的输出不是合法的 JSON，解析报错：
                %s

                你上次的输出：
                %s

                请重新输出，只输出纯 JSON，不要用代码块包裹，不要添加任何其他文字。
                """, errorMessage, originalOutput);
    }
}
