package com.example.agent.memory;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.List;
import java.util.Optional;

/**
 * Memory Manager — 记忆管理器
 * 负责构建 context：对话历史 + 用户画像
 *
 * v1.0: context = null
 * v2.0: context = 对话历史 + 用户画像
 * v3.0: context = 对话历史 + 用户画像 + RAG 检索结果
 */
@Component
public class MemoryManager {

    private final SessionMemory sessionMemory;
    private final SummaryPipeline summaryPipeline;
    private final UserProfileMapper profileMapper;

    public MemoryManager(SessionMemory sessionMemory,
                         SummaryPipeline summaryPipeline,
                         UserProfileMapper profileMapper) {
        this.sessionMemory = sessionMemory;
        this.summaryPipeline = summaryPipeline;
        this.profileMapper = profileMapper;
    }

    /**
     * 构建上下文字符串
     * 这是 v1.0 中 context=null 的第一次实际填充
     */
    /**
     * 构建上下文字符串
     * @param sessionId 对话历史的 key
     * @param userId 用户画像的 key（登录用户名）
     */
    public String buildContext(String sessionId, String userId) {
        StringBuilder sb = new StringBuilder();

        // 1. 用户画像（长期记忆，按 userId 查询）
        if (userId != null && !userId.isBlank()) {
            UserProfile profile = profileMapper.selectOne(
                    new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
            if (profile != null) {
                String ctx = profile.toContextString();
                if (!ctx.isBlank()) {
                    sb.append(ctx).append("\n");
                }
            }
        }

        // 2. 对话历史（短期记忆）
        List<String> messages = sessionMemory.getMessages(sessionId);
        if (messages != null && !messages.isEmpty()) {
            sb.append("## 对话历史\n");
            for (String msg : messages) {
                String[] parts = msg.split("\\|", 2);
                if (parts.length == 2) {
                    String role = "user".equals(parts[0]) ? "用户" : "助手";
                    // 检查是否是摘要
                    if (parts[1].startsWith("[对话摘要]")) {
                        sb.append("摘要: ").append(parts[1].substring(6)).append("\n");
                    } else {
                        sb.append(role).append(": ").append(parts[1].replace("\\n", "\n")).append("\n");
                    }
                }
            }
        }

        String context = sb.toString().isBlank() ? null : sb.toString();

        // 3. 检查是否需要摘要压缩
        if (summaryPipeline.needsSummarize(sessionId)) {
            System.out.println("[Memory] Session " + sessionId + " 消息过多，触发摘要压缩");
            summaryPipeline.summarizeSession(sessionId);
        }

        return context;
    }

    /**
     * 保存一轮对话
     */
    public void saveTurn(String sessionId, String userMessage, String assistantReply) {
        sessionMemory.addMessage(sessionId, "user", userMessage);
        sessionMemory.addMessage(sessionId, "assistant", assistantReply);
    }

    /**
     * 清空 Session
     */
    public void clearSession(String sessionId) {
        sessionMemory.clear(sessionId);
    }

    public SessionMemory getSessionMemory() {
        return sessionMemory;
    }
}
