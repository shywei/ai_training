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
import java.util.Optional;

/**
 * 用户画像抽取器 — 异步分析对话历史，提取用户偏好
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
            oldProfileJson = String.format("{\"facts\":\"%s\",\"preferences\":\"%s\",\"tasks\":\"%s\",\"constraints\":\"%s\"}",
                    escape(existing.getFacts()), escape(existing.getPreferences()),
                    escape(existing.getTasks()), escape(existing.getConstraints()));
        }

        // Prompt：传入旧画像 + 新对话，让 LLM 合并
        String prompt = """
                你有一个用户画像和一段新对话。请根据新对话更新画像。
                规则：
                - 保留旧画像中仍然有效的信息
                - 用新对话中的信息补充或修正
                - 新信息优先级高于旧信息
                - 没有新信息的字段保持原值
                - 所有值必须用英文双引号包裹
                - 只输出更新后的 JSON，不要任何其他文字
                
                旧画像：
                %s
                
                新对话：
                %s
                
                输出格式：
                {"facts":"...","preferences":"...","tasks":"...","constraints":"..."}
                """.formatted(oldProfileJson, conversation);

        log.info("\n╔══════════════════════════════════════════════════╗\n║ PROFILE EXTRACT REQUEST  {} ║\n╚══════════════════════════════════════════════════╝\n[Model] {}\n[User] {}\n[Session] {}\n\n[Prompt]\n{}", LocalDateTime.now().format(FMT), extractModel, userId, sessionId, prompt);

        long start = System.currentTimeMillis();
        String rawOutput = ChatClient.create(chatModel)
                .prompt()
                .system("只输出合法 JSON。所有值用英文双引号包裹。")
                .user(prompt)
                .options(OpenAiChatOptions.builder()
                        .withModel(extractModel)
                        .withTemperature(0.1)
                        .build())
                .call()
                .content();
        long duration = System.currentTimeMillis() - start;

        log.info("\n╔══════════════════════════════════════════════════╗\n║ PROFILE EXTRACT RESPONSE {} ║\n╚══════════════════════════════════════════════════╝\n[Model] {}\n[Duration] {}ms\n\n[Response]\n{}", LocalDateTime.now().format(FMT), extractModel, duration, rawOutput);

        System.out.println("[UserProfile] LLM 原始输出: " + rawOutput);

        try {
            // 提取 JSON
            String json = extractJson(rawOutput);
            System.out.println("[UserProfile] 提取 JSON: " + json);

            // 解析
            JsonNode node = objectMapper.readTree(json);

            // 更新或创建画像
            UserProfile profile = existing != null ? existing : new UserProfile(userId);

            String facts = getTextOrDefault(node, "facts");
            String prefs = getTextOrDefault(node, "preferences");
            String tasks = getTextOrDefault(node, "tasks");
            String constraints = getTextOrDefault(node, "constraints");

            if (!facts.isBlank()) profile.setFacts(facts);
            if (!prefs.isBlank()) profile.setPreferences(prefs);
            if (!tasks.isBlank()) profile.setTasks(tasks);
            if (!constraints.isBlank()) profile.setConstraints(constraints);
            profile.setUpdatedAt(System.currentTimeMillis());

            if (profile.getId() == null) {
                profileMapper.insert(profile);
            } else {
                profileMapper.updateById(profile);
            }

            System.out.println("[UserProfile] 用户 " + userId + " 画像更新:");
            System.out.println("  事实: " + profile.getFacts());
            System.out.println("  偏好: " + profile.getPreferences());
            System.out.println("  任务: " + profile.getTasks());
            System.out.println("  约束: " + profile.getConstraints());

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

        // 提取 ```json ... ``` 代码块
        int codeStart = text.indexOf("```json");
        if (codeStart >= 0) {
            codeStart = text.indexOf("\n", codeStart) + 1;
            int codeEnd = text.indexOf("```", codeStart);
            if (codeEnd > codeStart) return text.substring(codeStart, codeEnd).trim();
        }

        // 找 { 到 }
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }

        return "{}";
    }
}
