package com.example.agent.memory;

import com.example.agent.knowledge.RetrievalService;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

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
 * │   → 用于用户概览、后台展示、数据分析                    │
 * ├─────────────────────────────────────────────────────┤
 * │ pgvector user_memory：语义记忆（自然语言 + Embedding） │
 * │   → 历史对话片段、用户偏好、业务事件                    │
 * │   → 由自动沉淀机制 + 用户主动"记住"写入                 │
 * │   → 用于 AI Agent 语义检索                            │
 * ├─────────────────────────────────────────────────────┤
 * │ Redis session：工作缓冲区（当前会话未沉淀的消息）       │
 * │   → 自动沉淀后清空                                   │
 * │   → 用于当前会话的连贯性                              │
 * └─────────────────────────────────────────────────────┘
 *
 * 自动沉淀机制：
 * - 每 N 轮对话触发一次
 * - 将 Redis 中的消息沉淀到 pgvector（摘要）
 * - 完全清空 Redis（历史上下文由 pgvector 语义检索提供）
 *
 * context 演进：
 * - 第四课：null（纯 Prompt）
 * - 第五课：对话历史 + 用户画像
 * - 第六课：用户画像 + 用户记忆 + 对话历史 + RAG 知识
 * - 第七课：+ Tool 调用结果
 */
@Component
public class MemoryManager {

    private final SessionMemory sessionMemory;
    private final SummaryPipeline summaryPipeline;
    private final UserProfileMapper profileMapper;
    private final UserMemoryService memoryService;
    private final RetrievalService retrievalService;

    /** 自动沉淀阈值：每 5 轮对话（10条消息）触发一次 */
    private static final int PRECIPITATE_THRESHOLD = 10;

    public MemoryManager(SessionMemory sessionMemory,
                         SummaryPipeline summaryPipeline,
                         UserProfileMapper profileMapper,
                         UserMemoryService memoryService,
                         RetrievalService retrievalService) {
        this.sessionMemory = sessionMemory;
        this.summaryPipeline = summaryPipeline;
        this.profileMapper = profileMapper;
        this.memoryService = memoryService;
        this.retrievalService = retrievalService;
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

        // 1. 用户画像（MySQL 结构化标签 —— 始终查询）
        if (userId != null && !userId.isBlank()) {
            UserProfile profile = profileMapper.selectOne(
                    new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
            if (profile != null) {
                String ctx = profile.toContextString();
                if (!ctx.isBlank()) {
                    sb.append(ctx);
                }
            }
        }

        // 2. 用户记忆（pgvector 语义检索 —— 始终查询）
        if (userId != null && !userId.isBlank() && userMessage != null && !userMessage.isBlank()) {
            String memoryContext = memoryService.search(userId, userMessage);
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

        // 4. RAG 检索结果（知识库）
        if (userMessage != null && !userMessage.isBlank()) {
            String ragResults = retrievalService.retrieve(userMessage);
            if (ragResults != null && !ragResults.isBlank()) {
                sb.append("## 参考知识（从商品库检索）\n");
                sb.append(ragResults).append("\n");
                System.out.println("[Knowledge] RAG 检索成功，注入 " + ragResults.split("\n").length + " 行参考信息");
            }
        }

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
     * 自动沉淀：消息达到阈值时，将 Redis 内容沉淀到 pgvector 并清空
     */
    private void checkAndPrecipitate(String sessionId, String userId) {
        if (userId == null || userId.isBlank()) return;

        long count = sessionMemory.getMessageCount(sessionId);
        if (count < PRECIPITATE_THRESHOLD) return;

        System.out.println("[Memory] 消息数 " + count + " 达到阈值 " + PRECIPITATE_THRESHOLD + "，触发自动沉淀");

        List<String> messages = sessionMemory.getMessages(sessionId);
        if (messages == null || messages.isEmpty()) return;

        // 沉淀到 pgvector
        try {
            memoryService.storeConversationSummary(userId, sessionId, messages);
            System.out.println("[Memory] ✅ 自动沉淀完成");
        } catch (Exception e) {
            System.err.println("[Memory] ⚠️ 自动沉淀失败: " + e.getMessage());
            return; // 沉淀失败不清空，避免数据丢失
        }

        // 完全清空 Redis（历史上下文由 pgvector 语义检索提供）
        sessionMemory.clear(sessionId);
        System.out.println("[Memory] Redis 已清空");
    }

    /**
     * 清空 Session — 手动沉淀剩余消息
     */
    public void clearSession(String sessionId, String userId) {
        if (userId != null && !userId.isBlank()) {
            List<String> messages = sessionMemory.getMessages(sessionId);
            if (messages != null && !messages.isEmpty()) {
                try {
                    memoryService.storeConversationSummary(userId, sessionId, messages);
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
}
