package com.example.agent.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 用户画像抽取器 — 从对话历史中提取结构化画像，写入 MySQL
 *
 * 只提取持久化有意义的字段：
 * - facts：用户的客观事实（性别、身份、设备等）
 * - preferences：用户的主观偏好（喜欢什么、不喜欢什么）
 *
 * 职责分工：
 * - 本类：LLM 提取结构化标签 → MySQL user_profile
 * - MemoryManager：对话摘要沉淀 → pgvector user_memory（通过 MCP）
 */
@Component
public class UserProfileExtractor {

    private final SessionMemory sessionMemory;
    private final UserProfileMapper profileMapper;
    private final ChatModel chatModel;
    private final String extractModel;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger("LLM_PROFILE");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public UserProfileExtractor(SessionMemory sessionMemory,
                                UserProfileMapper profileMapper,
                                ChatModel chatModel,
                                @Value("${agent.model.extract}") String extractModel) {
        this.sessionMemory = sessionMemory;
        this.profileMapper = profileMapper;
        this.chatModel = chatModel;
        this.extractModel = extractModel;
    }

    public UserProfile getProfile(String userId) {
        return profileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
    }

    public UserProfile extract(String userId, String sessionId) {
        List<String> messages = sessionMemory.getMessages(sessionId);
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        // 构建对话文本
        StringBuilder conversation = new StringBuilder();
        for (String msg : messages) {
            String[] parts = msg.split("\\|", 2);
            if (parts.length == 2) {
                String role = "user".equals(parts[0]) ? "用户" : "助手";
                conversation.append(role).append(": ").append(parts[1]).append("\n");
            }
        }

        // 获取旧画像
        UserProfile existing = getProfile(userId);
        String oldProfileJson = "{}";
        if (existing != null) {
            oldProfileJson = String.format("{\"facts\":\"%s\",\"preferences\":\"%s\"}",
                    escape(existing.getFacts()), escape(existing.getPreferences()));
        }

        // Prompt：传入旧画像 + 新对话，让 LLM 合并
        String prompt = """
                你有一个用户画像和一段新对话。请根据新对话更新画像。

                ## 字段说明

                facts 和 preferences 都是**纯文本字符串**，不是 JSON 对象。
                用逗号分隔多个信息点，用中文描述。

                facts 字段（用户的客观事实）：
                - 示例："男性，学生，在用小米手机"
                - 示例："程序员，上班族，用 MacBook"

                preferences 字段（用户的通用偏好）：
                - 示例："偏好轻薄本，注重性价比"
                - 示例："喜欢苹果生态，预算 8000-12000"

                ## 禁止提取
                - 具体商品推荐内容（“推荐了ThinkPad X1”）
                - 临时需求（“这次想买个笔记本”）
                - 单次对话中的价格/配置信息

                ## 更新规则
                - 保留旧画像中仍然有效的信息
                - 用新对话中的信息补充或修正
                - 新信息优先级高于旧信息
                - 没有新信息的字段保持原值

                ## 输出格式（严格遵守）
                只输出一个 JSON 对象，facts 和 preferences 都是字符串值：
                {"facts":"性别，身份，设备等信息","preferences":"偏好，品牌倾向，预算等信息"}

                错误示例（不要这样输出）：
                {"facts":"gender":"male"} ← 错误！facts 不是嵌套对象

                正确示例：
                {"facts":"男性，学生，在用小米手机","preferences":"偏好轻薄本，预算 8000"}

                ## 旧画像
                %s

                ## 新对话
                %s

                只输出 JSON，不要任何其他文字。
                """.formatted(oldProfileJson, conversation);

        log.info("\n╔══════════════════════════════════════════════════╗\n║ PROFILE EXTRACT REQUEST  {} ║\n╚══════════════════════════════════════════════════╝\n[Model] {}\n[User] {}\n[Session] {}\n\n[Prompt]\n{}", LocalDateTime.now().format(FMT), extractModel, userId, sessionId, prompt);

        long start = System.currentTimeMillis();
        String rawOutput = ChatClient.create(chatModel)
                .prompt()
                .system("只输出合法 JSON。facts 和 preferences 都是纯文本字符串，不是嵌套对象。示例：{\"facts\":\"男性，学生\",\"preferences\":\"偏好轻薄本\"}")
                .user(prompt)
                .options(OpenAiChatOptions.builder()
                        .withModel(extractModel)
                        .withTemperature(0.1)
                        .build())
                .call()
                .content();
        long duration = System.currentTimeMillis() - start;

        log.info("\n╔══════════════════════════════════════════════════╗\n║ PROFILE EXTRACT RESPONSE {} ║\n╚══════════════════════════════════════════════════╝\n[Model] {}\n[Duration] {}ms\n\n[Response]\n{}", LocalDateTime.now().format(FMT), extractModel, duration, rawOutput);

        try {
            String json = extractJson(rawOutput);
            log.debug("[UserProfile] 提取 JSON: {}", json);

            JsonNode node = objectMapper.readTree(json);

            UserProfile profile = existing != null ? existing : new UserProfile(userId);

            String facts = getTextOrDefault(node, "facts");
            String prefs = getTextOrDefault(node, "preferences");

            if (!facts.isBlank()) profile.setFacts(facts);
            if (!prefs.isBlank()) profile.setPreferences(prefs);
            profile.setUpdatedAt(System.currentTimeMillis());

            if (profile.getId() == null) {
                profileMapper.insert(profile);
            } else {
                profileMapper.updateById(profile);
            }

            System.out.println("[UserProfile] 用户 " + userId + " 画像更新（MySQL）:");
            System.out.println("  事实: " + profile.getFacts());
            System.out.println("  偏好: " + profile.getPreferences());

            return profile;

        } catch (Exception e) {
            System.out.println("[UserProfile] 解析失败: " + e.getMessage());
            System.out.println("[UserProfile] 原始输出: " + rawOutput);
            return null;
        }
    }

    private String getTextOrDefault(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asText("");
        }
        return "";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String extractJson(String text) {
        if (text == null) return "{}";
        text = text.trim();

        int codeStart = text.indexOf("```json");
        if (codeStart >= 0) {
            codeStart = text.indexOf("\n", codeStart) + 1;
            int codeEnd = text.indexOf("```", codeStart);
            if (codeEnd > codeStart) return text.substring(codeStart, codeEnd).trim();
        }

        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }

        return "{}";
    }
}
