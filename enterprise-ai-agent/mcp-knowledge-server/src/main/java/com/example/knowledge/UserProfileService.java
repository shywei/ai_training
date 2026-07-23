package com.example.knowledge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 用户画像服务 — 从 MySQL 读取用户画像。
 *
 * 为 MCP Resource 提供数据源。
 * 用户画像表（user_profile）存储在 MySQL 中，由 agent-runtime 的 UserProfileExtractor 写入。
 */
@Service
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

    private final JdbcTemplate mysqlJdbcTemplate;

    public UserProfileService(@Qualifier("mysqlJdbcTemplate") JdbcTemplate mysqlJdbcTemplate) {
        this.mysqlJdbcTemplate = mysqlJdbcTemplate;
    }

    /**
     * 获取用户画像，返回格式化的 JSON 字符串。
     *
     * @param userId 用户 ID
     * @return JSON 格式的画像数据，未找到返回 null
     */
    public String getProfile(String userId) {
        try {
            var results = mysqlJdbcTemplate.queryForList(
                    "SELECT user_id, facts, preferences, updated_at FROM user_profile WHERE user_id = ?",
                    userId);

            if (results.isEmpty()) {
                return null;
            }

            Map<String, Object> row = results.get(0);
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"user_id\": \"").append(row.get("user_id")).append("\",\n");

            String facts = (String) row.get("facts");
            if (facts != null && !facts.isBlank()) {
                sb.append("  \"facts\": \"").append(escapeJson(facts)).append("\",\n");
            }

            String preferences = (String) row.get("preferences");
            if (preferences != null && !preferences.isBlank()) {
                sb.append("  \"preferences\": \"").append(escapeJson(preferences)).append("\"\n");
            }

            sb.append("}");
            return sb.toString();
        } catch (Exception e) {
            log.error("读取用户画像失败: {}", e.getMessage(), e);
            return null;
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
