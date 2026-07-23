package com.example.agent.memory;

import com.example.agent.tool.McpKnowledgeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Memory Manager — 记忆管理器
 * 负责构建 context：用户画像 + 用户记忆 + 对话历史 + RAG 知识
 *
 * 三层存储架构：
 * ┌─────────────────────────────────────────────────────┐
 * │ MySQL user_profile：结构化画像标签                     │
 * │   → facts / preferences                             │
 * │   → 由 UserProfileExtractor 写入                     │
 * │   → 通过 MCP Resource（user://{userId}）读取          │
 * ├─────────────────────────────────────────────────────┤
 * │ pgvector user_memory：语义记忆（自然语言 + Embedding） │
 * │   → 历史对话片段、用户偏好、业务事件                    │
 * │   → 通过 MCP Tool（search_memory / save_memory）访问  │
 * ├─────────────────────────────────────────────────────┤
 * │ Redis session：工作缓冲区（当前会话未沉淀的消息）       │
 * │   → 自动沉淀后清空                                   │
 * └─────────────────────────────────────────────────────┘
 *
 * 所有持久化存储统一通过 MCP 协议访问，MemoryManager 不直接耦合任何数据库。
 *
 * 自动沉淀机制：
 * - 每 20 条消息触发一次
 * - 提取前 10 条沉淀到 pgvector，保留后 10 条在 Redis
 * - 确保每次对话还能带上最近的历史上下文
 *
 * context 演进：
 * - 第四课：null（纯 Prompt）
 * - 第五课：对话历史 + 用户画像
 * - 第六课：用户画像 + 用户记忆 + 对话历史 + RAG 知识
 * - 第七课：+ Tool 调用结果
 *
 * MCP 化变化（第七课）：
 * - 画像读取：McpKnowledgeClient.readUserProfile() → MCP Resource → MySQL
 * - 记忆读写：McpKnowledgeClient.searchMemory() / saveMemory() → MCP Tool → pgvector
 * - 沉淀写入：McpKnowledgeClient.saveMemory() → MCP Tool → pgvector
 * - MemoryManager 不直接依赖任何 Mapper 或数据库访问组件
 */
@Component
public class MemoryManager {

    private final SessionMemory sessionMemory;
    private final McpKnowledgeClient mcpKnowledgeClient;
    private final ChatModel chatModel;
    private final String memoryModel;

    /** 自动沉淀阈值：消息数达到此值时触发 */
    private static final int PRECIPITATE_THRESHOLD = 20;
    /** 沉淀后保留的最近消息数 */
    private static final int KEEP_RECENT = 10;
    private static final Logger log = LoggerFactory.getLogger("LLM_MEMORY");

    public MemoryManager(SessionMemory sessionMemory,
                         McpKnowledgeClient mcpKnowledgeClient,
                         ChatModel chatModel,
                         @Value("${agent.model.memory}") String memoryModel) {
        this.sessionMemory = sessionMemory;
        this.mcpKnowledgeClient = mcpKnowledgeClient;
        this.chatModel = chatModel;
        this.memoryModel = memoryModel;
    }

    /**
     * 构建上下文字符串
     *
     * @param sessionId   对话历史的 key
     * @param userId      用户 ID
     * @param userMessage 用户当前消息（用于语义检索）
     */
    public String buildContext(String sessionId, String userId, String userMessage) {
        StringBuilder sb = new StringBuilder();

        // 1. 用户画像（通过 MCP Resource 读取）
        if (userId != null && !userId.isBlank()) {
            String profile = mcpKnowledgeClient.readUserProfile(userId);
            if (profile != null && !profile.isBlank()) {
                sb.append("## 用户画像\n");
                sb.append(profile).append("\n\n");
            }
        }

        // 2. 用户记忆（通过 MCP 协议调用 Knowledge Server 语义检索）
        if (userId != null && !userId.isBlank() && userMessage != null && !userMessage.isBlank()) {
            String memoryContext = mcpKnowledgeClient.searchMemory(userId, userMessage);
            if (memoryContext != null && !memoryContext.isBlank()) {
                sb.append("## 相关记忆\n");
                sb.append(memoryContext).append("\n\n");
                System.out.println("[Memory] 语义检索到用户记忆");
            }
        }

        // 3. 对话历史（Redis 工作缓冲区 —— 沉淀后为空）
        List<String> messages = sessionMemory.getMessages(sessionId);
        if (messages != null && !messages.isEmpty()) {
            sb.append("## 对话历史\n");
            for (String msg : messages) {
                String[] parts = msg.split("\\|", 2);
                if (parts.length == 2) {
                    String role = "user".equals(parts[0]) ? "用户" : "助手";
                    if (parts[1].startsWith("[对话摘要]")) {
                        sb.append("摘要: ").append(parts[1].substring(6)).append("\n");
                    } else {
                        sb.append(role).append(": ").append(parts[1].replace("\\n", "\n")).append("\n");
                    }
                }
            }
        }

        // 注意：RAG 检索已由 ShoppingAgentController 直接调用 mcpKnowledgeClient.search()

        return sb.toString().isBlank() ? null : sb.toString();
    }

    /**
     * 保存一轮对话，并检查是否需要自动沉淀
     *
     * 自动沉淀机制：
     * 1. 消息写入 Redis
     * 2. 检查消息数是否达到阈值
     * 3. 达到阈值 → 沉淀到 pgvector → 完全清空 Redis
     *
     * 沉淀后 Redis 为空，历史上下文由 pgvector 语义检索提供
     */
    public void saveTurn(String sessionId, String userId, String userMessage, String assistantReply) {
        sessionMemory.addMessage(sessionId, "user", userMessage);
        sessionMemory.addMessage(sessionId, "assistant", assistantReply);
        checkAndPrecipitate(sessionId, userId);
    }

    /**
     * 保存一轮对话（兼容旧调用，不触发沉淀）
     */
    public void saveTurn(String sessionId, String userMessage, String assistantReply) {
        sessionMemory.addMessage(sessionId, "user", userMessage);
        sessionMemory.addMessage(sessionId, "assistant", assistantReply);
    }

    /**
     * 自动沉淀：消息达到阈值时，提取前 N-KEEP 条沉淀，保留后 KEEP 条
     *
     * 策略：满 20 条 → 提取前 10 条 → 保留后 10 条
     * 确保每次对话还能带上最近的历史上下文
     */
    private void checkAndPrecipitate(String sessionId, String userId) {
        if (userId == null || userId.isBlank()) return;

        long count = sessionMemory.getMessageCount(sessionId);
        if (count < PRECIPITATE_THRESHOLD) return;

        System.out.println("[Memory] 消息数 " + count + " 达到阈值 " + PRECIPITATE_THRESHOLD + "，触发自动沉淀");

        List<String> messages = sessionMemory.getMessages(sessionId);
        if (messages == null || messages.isEmpty()) return;

        // 取前 N-KEEP 条用于提取摘要
        int extractEnd = messages.size() - KEEP_RECENT;
        List<String> toExtract = messages.subList(0, extractEnd);

        // 通过 MCP 协议沉淀到 pgvector
        try {
            String summary = extractConversationSummary(toExtract);
            if (summary != null && !summary.isBlank()) {
                mcpKnowledgeClient.saveMemory(userId, "conversation", summary);
                System.out.println("[Memory] ✅ 自动沉淀完成，提取了 " + extractEnd + " 条消息");
            }
        } catch (Exception e) {
            System.err.println("[Memory] ⚠️ 自动沉淀失败: " + e.getMessage());
            return; // 沉淀失败不裁剪，避免数据丢失
        }

        // 保留后 KEEP_RECENT 条，丢弃更早的消息
        sessionMemory.keepRecent(sessionId, KEEP_RECENT);
        System.out.println("[Memory] Redis 已裁剪，保留最近 " + KEEP_RECENT + " 条消息");
    }

    /**
     * 清空 Session — 手动沉淀剩余消息
     */
    public void clearSession(String sessionId, String userId) {
        if (userId != null && !userId.isBlank()) {
            List<String> messages = sessionMemory.getMessages(sessionId);
            if (messages != null && !messages.isEmpty()) {
                try {
                    String summary = extractConversationSummary(messages);
                    if (summary != null && !summary.isBlank()) {
                        mcpKnowledgeClient.saveMemory(userId, "conversation", summary);
                    }
                } catch (Exception e) {
                    System.err.println("[Memory] 对话沉淀失败: " + e.getMessage());
                }
            }
        }
        sessionMemory.clear(sessionId);
    }

    /**
     * 清空 Session（兼容旧调用）
     */
    public void clearSession(String sessionId) {
        sessionMemory.clear(sessionId);
    }

    public SessionMemory getSessionMemory() {
        return sessionMemory;
    }

    /**
     * 用 LLM 从对话中提取关键信息（记忆沉淀的核心逻辑）
     *
     * 提取用户的偏好、需求、决策和关键事实，
     * 作为自然语言记忆存入 pgvector。
     *
     * @param messages 对话消息列表（格式：role|content）
     * @return 提取的关键信息摘要
     */
    private String extractConversationSummary(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        // 拼接对话内容
        StringBuilder conversation = new StringBuilder();
        for (String msg : messages) {
            String[] parts = msg.split("\\|", 2);
            if (parts.length == 2) {
                if (parts[1].startsWith("[对话摘要]")) continue;
                String role = "user".equals(parts[0]) ? "用户" : "助手";
                conversation.append(role).append(": ").append(parts[1]).append("\n");
            }
        }

        if (conversation.length() < 10) return null;

        String prompt = """
                请从以下对话中提取关键信息，用简洁的自然语言总结。

                记录对话中发生的事实：
                - 用户做了什么决定（选择了哪个商品、放弃了什么）
                - 用户提到了什么需求或约束（预算、用途、场景）
                - 对话中涉及的关键商品和价格

                不要提炼用户偏好（那是画像系统的事），只客观记录对话中发生了什么。
                忽略寒暄和无关内容，不超过200字，用中文。

                对话内容：
                %s
                """.formatted(conversation);

        try {
            log.info("\n╔══════════════════════════════════════════════════╗\n║ MEMORY EXTRACT {} ║\n╚══════════════════════════════════════════════════╝\n[Prompt]\n{}", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), prompt.substring(0, Math.min(200, prompt.length())));

            String result = ChatClient.create(chatModel)
                    .prompt()
                    .user(prompt)
                    .options(OpenAiChatOptions.builder()
                            .withModel(memoryModel)
                            .withTemperature(0.3)
                            .build())
                    .call()
                    .content();

            log.info("[Result] {}", result != null ? result.substring(0, Math.min(200, result.length())) : "null");
            return result;

        } catch (Exception e) {
            System.err.println("[Memory] LLM 摘要提取失败: " + e.getMessage());
            return null;
        }
    }
}
