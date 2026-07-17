package com.example.agent.memory;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 用户画像实体 — 长期记忆
 * 从对话历史中异步抽取的用户偏好信息
 *
 * 只保留持久化有意义的字段：
 * - facts：用户的客观事实（性别、身份、设备等）
 * - preferences：用户的主观偏好（喜欢什么、不喜欢什么）
 *
 * 移除的字段：
 * - tasks：会话级任务，不应持久化（"推荐笔记本"是当前需求，不是永久属性）
 * - constraints：和 preferences 重叠（"预算8000"已在 preferences 中）
 */
@TableName("user_profile")
public class UserProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;
    private String facts;
    private String preferences;
    private Long updatedAt;

    public UserProfile() {}

    public UserProfile(String userId) {
        this.userId = userId;
        this.updatedAt = System.currentTimeMillis();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getFacts() { return facts; }
    public void setFacts(String facts) { this.facts = facts; }
    public String getPreferences() { return preferences; }
    public void setPreferences(String preferences) { this.preferences = preferences; }
    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }

    public String toContextString() {
        StringBuilder sb = new StringBuilder();
        sb.append("## 用户画像\n");
        if (facts != null && !facts.isBlank()) sb.append("事实: ").append(facts).append("\n");
        if (preferences != null && !preferences.isBlank()) sb.append("偏好: ").append(preferences).append("\n");
        return sb.toString();
    }
}
