package com.example.agent.memory;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Session Memory — 短期记忆
 * 基于 Redis 的会话历史存储
 *
 * 第五课核心知识点：
 * - Session 存储：每轮对话的消息存入 Redis
 * - 滑动窗口：只保留最近 N 轮对话，避免 Token 爆炸
 * - Key 设计：session:{sessionId}:messages
 */
@Component
public class SessionMemory {

    private final StringRedisTemplate redis;
    private static final int MAX_MESSAGES = 20; // 滑动窗口：最多保留 20 条消息
    private static final long TTL_HOURS = 24;   // Session 过期时间

    public SessionMemory(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 保存一条消息到 Session
     */
    public void addMessage(String sessionId, String role, String content) {
        String key = "session:" + sessionId + ":messages";
        String value = role + "|" + content.replace("\n", "\\n");
        redis.opsForList().rightPush(key, value);
        redis.expire(key, TTL_HOURS, TimeUnit.HOURS);

        // 滑动窗口：超过上限时裁剪
        Long size = redis.opsForList().size(key);
        if (size != null && size > MAX_MESSAGES) {
            redis.opsForList().leftPop(key);
        }
    }

    /**
     * 获取 Session 中的所有消息
     */
    public List<String> getMessages(String sessionId) {
        String key = "session:" + sessionId + ":messages";
        return redis.opsForList().range(key, 0, -1);
    }

    /**
     * 清空 Session
     */
    public void clear(String sessionId) {
        redis.delete("session:" + sessionId + ":messages");
    }

    /**
     * 获取 Session 消息数量
     */
    public long getMessageCount(String sessionId) {
        Long size = redis.opsForList().size("session:" + sessionId + ":messages");
        return size != null ? size : 0;
    }

    /**
     * 保留最近 N 条消息，丢弃更早的消息
     */
    public void keepRecent(String sessionId, int keepCount) {
        String key = "session:" + sessionId + ":messages";
        Long size = redis.opsForList().size(key);
        if (size == null || size <= keepCount) return;
        long start = size - keepCount;
        // LTRIM 保留 [start, -1] 区间
        redis.opsForList().trim(key, start, -1);
    }
}
