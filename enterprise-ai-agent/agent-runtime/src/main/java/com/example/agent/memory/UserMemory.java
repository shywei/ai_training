package com.example.agent.memory;

import java.time.LocalDateTime;

/**
 * 用户记忆实体 — 向量化存储的用户记忆
 *
 * 三种记忆类型：
 * - preference：用户偏好（从画像抽取器提取）
 * - conversation：对话片段（Session 结束时沉淀）
 * - event：业务事件（下单、退货等，第七课 Tool 回调写入）
 *
 * 与 UserProfile 的区别：
 * - UserProfile：结构化标签（facts/preferences/tasks/constraints），MySQL 存储
 * - UserMemory：自然语言记忆片段，pgvector 存储，支持语义检索
 *
 * 两者互补：UserProfile 做"这个用户是谁"的概览，UserMemory 做"和当前对话相关的记忆"检索
 */
public class UserMemory {

    private Long id;
    private String userId;
    private String memoryType;   // preference / conversation / event
    private String content;      // 自然语言记忆内容
    private String metadataJson; // JSON 格式的元数据
    private float[] embedding;   // 向量
    private LocalDateTime createdAt;

    public UserMemory() {}

    public UserMemory(String userId, String memoryType, String content) {
        this.userId = userId;
        this.memoryType = memoryType;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public UserMemory(String userId, String memoryType, String content, String metadataJson) {
        this(userId, memoryType, content);
        this.metadataJson = metadataJson;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getMemoryType() { return memoryType; }
    public void setMemoryType(String memoryType) { this.memoryType = memoryType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }

    public float[] getEmbedding() { return embedding; }
    public void setEmbedding(float[] embedding) { this.embedding = embedding; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s", memoryType, userId,
                content.length() > 50 ? content.substring(0, 50) + "..." : content);
    }
}
