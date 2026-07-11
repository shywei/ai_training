package com.example.agent.memory;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 用户画像实体 — 长期记忆
 * 从对话历史中异步抽取的用户偏好信息
 */
@TableName("user_profile")
public class UserProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;
    private String facts;
    private String preferences;
    private String tasks;
    private String constraints;
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
    public String getTasks() { return tasks; }
    public void setTasks(String tasks) { this.tasks = tasks; }
    public String getConstraints() { return constraints; }
    public void setConstraints(String constraints) { this.constraints = constraints; }
    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }

    public String toContextString() {
        StringBuilder sb = new StringBuilder();
        sb.append("## 用户画像\n");
        if (facts != null && !facts.isBlank()) sb.append("事实: ").append(facts).append("\n");
        if (preferences != null && !preferences.isBlank()) sb.append("偏好: ").append(preferences).append("\n");
        if (tasks != null && !tasks.isBlank()) sb.append("任务: ").append(tasks).append("\n");
        if (constraints != null && !constraints.isBlank()) sb.append("约束: ").append(constraints).append("\n");
        return sb.toString();
    }
}
