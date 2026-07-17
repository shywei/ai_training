package com.example.agent.memory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

/**
 * 用户记忆 Mapper — 操作 pgvector 的 user_memory 表
 *
 * 注意：使用 pgvectorDataSource（PostgreSQL），不是主数据源（MySQL）
 *
 * 核心操作：
 * - save()：存储记忆 + 向量
 * - searchBySimilarity()：按用户 + 语义相似度检索
 * - findByUserId()：查询用户所有记忆
 * - deleteByUserId()：清空用户记忆
 */
@Repository
public class UserMemoryMapper {

    private final JdbcTemplate pgvectorJdbcTemplate;

    public UserMemoryMapper(JdbcTemplate pgvectorJdbcTemplate) {
        this.pgvectorJdbcTemplate = pgvectorJdbcTemplate;
    }

    /**
     * 保存一条用户记忆（含向量）
     */
    public void save(UserMemory memory) {
        String vectorStr = floatArrayToSqlVector(memory.getEmbedding());
        pgvectorJdbcTemplate.update(
                "INSERT INTO user_memory (user_id, memory_type, content, metadata, embedding) VALUES (?, ?, ?, ?::jsonb, ?::vector)",
                memory.getUserId(),
                memory.getMemoryType(),
                memory.getContent(),
                memory.getMetadataJson(),
                vectorStr
        );
    }

    /**
     * 按用户 ID + 语义相似度检索记忆
     *
     * @param userId     用户 ID
     * @param queryEmbedding 查询向量
     * @param threshold  相似度阈值（越小越严格，0.3 是经验值）
     * @param limit      返回数量
     * @return 按相似度排序的记忆列表
     */
    public List<UserMemory> searchBySimilarity(String userId, float[] queryEmbedding,
                                                 double threshold, int limit) {
        String vectorStr = floatArrayToSqlVector(queryEmbedding);

        // cosine distance: 0 = 完全相同, 2 = 完全相反
        // similarity = 1 - cosine_distance
        // 所以 threshold 0.3 对应 similarity 0.7
        String sql = """
                SELECT id, user_id, memory_type, content, metadata,
                       1 - (embedding <=> ?::vector) AS similarity
                FROM user_memory
                WHERE user_id = ?
                  AND 1 - (embedding <=> ?::vector) > ?
                ORDER BY embedding <=> ?::vector
                LIMIT ?
                """;

        return pgvectorJdbcTemplate.query(sql, memoryRowMapper(),
                vectorStr, userId, vectorStr, threshold, vectorStr, limit);
    }

    /**
     * 查询用户所有记忆（不做过滤，按时间倒序）
     */
    public List<UserMemory> findByUserId(String userId) {
        return pgvectorJdbcTemplate.query(
                "SELECT id, user_id, memory_type, content, metadata, embedding FROM user_memory WHERE user_id = ? ORDER BY created_at DESC",
                memoryRowMapperWithoutSimilarity(), userId);
    }

    /**
     * 按类型查询用户记忆
     */
    public List<UserMemory> findByUserIdAndType(String userId, String memoryType) {
        return pgvectorJdbcTemplate.query(
                "SELECT id, user_id, memory_type, content, metadata, embedding FROM user_memory WHERE user_id = ? AND memory_type = ? ORDER BY created_at DESC",
                memoryRowMapperWithoutSimilarity(), userId, memoryType);
    }

    /**
     * 删除用户所有记忆
     */
    public void deleteByUserId(String userId) {
        pgvectorJdbcTemplate.update("DELETE FROM user_memory WHERE user_id = ?", userId);
    }

    /**
     * 获取用户记忆数量
     */
    public int countByUserId(String userId) {
        Integer count = pgvectorJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_memory WHERE user_id = ?", Integer.class, userId);
        return count != null ? count : 0;
    }

    /**
     * float[] 转 pgvector 的字符串格式：'[0.1, 0.2, 0.3, ...]'
     */
    private String floatArrayToSqlVector(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 带相似度分数的 RowMapper
     */
    private RowMapper<UserMemory> memoryRowMapper() {
        return (ResultSet rs, int rowNum) -> {
            UserMemory mem = new UserMemory();
            mem.setId(rs.getLong("id"));
            mem.setUserId(rs.getString("user_id"));
            mem.setMemoryType(rs.getString("memory_type"));
            mem.setContent(rs.getString("content"));
            mem.setMetadataJson(rs.getString("metadata"));
            return mem;
        };
    }

    /**
     * 不带相似度的 RowMapper
     */
    private RowMapper<UserMemory> memoryRowMapperWithoutSimilarity() {
        return (ResultSet rs, int rowNum) -> {
            UserMemory mem = new UserMemory();
            mem.setId(rs.getLong("id"));
            mem.setUserId(rs.getString("user_id"));
            mem.setMemoryType(rs.getString("memory_type"));
            mem.setContent(rs.getString("content"));
            mem.setMetadataJson(rs.getString("metadata"));
            return mem;
        };
    }
}
