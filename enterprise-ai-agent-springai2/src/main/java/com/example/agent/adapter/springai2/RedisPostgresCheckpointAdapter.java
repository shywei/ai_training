package com.example.agent.adapter.springai2;

import com.example.agent.domain.AgentCheckpoint;
import com.example.agent.domain.WorkflowState;
import com.example.agent.domain.WorkflowStep;
import com.example.agent.port.CheckpointStorePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring AI 2.0 状态快照与持久化适配器（实现 CheckpointStorePort 端口）
 *
 * 对应第 09 课 P07~P10（深度深水区 1 · 有状态 Agent 的序列化与断点复水）：
 *
 * 【核心设计哲学】：
 * 1. 物理分离三大存储边界：
 *    - 对话历史（Chat History）：存放于 Redis 会话 List，仅供 LLM Prompt 上下文渲染；
 *    - 用户画像（Agent State）：存放于 MySQL 用户画像表；
 *    - 执行快照（Execution Checkpoint）：包含 Call Stack、Scratchpad、Step 游标、状态差分，
 *      热数据保存在 Redis (Key: checkpoint:{executionId}:{checkpointId})，冷数据持久化至 PostgreSQL 表。
 *
 * 2. 支持双轨快照机理：
 *    - 全量快照（Full Snapshot）：记录某个关键节点时的完整上下文；
 *    - 增量差分（State Delta）：仅记录本步骤产生的变量变更，减少网络与存储开销。
 *
 * 3. 支持分支回滚（Forking / Time-Travel 时光倒流）：
 *    - 基于 parentCheckpointId 形成的因果链 DAG 树，支持从任意历史 Checkpoint 分叉新执行链路。
 */
@Component
public class RedisPostgresCheckpointAdapter implements CheckpointStorePort {

    private static final Logger log = LoggerFactory.getLogger(RedisPostgresCheckpointAdapter.class);

    private static final String REDIS_CHECKPOINT_PREFIX = "agent:checkpoint:";
    private static final String REDIS_LATEST_PREFIX = "agent:checkpoint:latest:";
    private static final Duration CHECKPOINT_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    // 内存兜底存储（防止开发测试环境下 Redis/PostgreSQL 临时不可用导致阻断）
    private final Map<String, Map<String, AgentCheckpoint>> memoryStorage = new ConcurrentHashMap<>();

    public RedisPostgresCheckpointAdapter(StringRedisTemplate redisTemplate,
                                          JdbcTemplate jdbcTemplate,
                                          ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void saveSnapshot(AgentCheckpoint checkpoint) {
        if (checkpoint == null || checkpoint.getExecutionId() == null) {
            return;
        }

        String executionId = checkpoint.getExecutionId();
        String checkpointId = checkpoint.getCheckpointId();

        // 1. 同步保存至本地内存快照表（高可靠兜底）
        memoryStorage.computeIfAbsent(executionId, k -> new LinkedHashMap<>())
                .put(checkpointId, checkpoint);

        try {
            String json = objectMapper.writeValueAsString(checkpoint);

            // 2. 存入 Redis 热存储（带 TTL）
            if (redisTemplate != null) {
                String key = REDIS_CHECKPOINT_PREFIX + executionId + ":" + checkpointId;
                redisTemplate.opsForValue().set(key, json, CHECKPOINT_TTL);
                redisTemplate.opsForValue().set(REDIS_LATEST_PREFIX + executionId, checkpointId, CHECKPOINT_TTL);
            }

            // 3. 异步/同步持久化入 PostgreSQL 表 (演示冷存储落库)
            persistToPostgres(checkpoint, json);

            log.info("[Checkpointer] ✅ 成功保存 Checkpoint 快照: executionId={}, checkpointId={}, state={}, stepCursor={}",
                    executionId, checkpointId, checkpoint.getState(), checkpoint.getStepCursor());

        } catch (JsonProcessingException e) {
            log.error("[Checkpointer] 序列化快照失败: {}", e.getMessage(), e);
            throw new RuntimeException("快照序列化失败", e);
        }
    }

    @Override
    public void saveDelta(String executionId, String checkpointId, Map<String, Object> delta) {
        AgentCheckpoint checkpoint = loadCheckpoint(executionId, checkpointId);
        if (checkpoint != null && delta != null && !delta.isEmpty()) {
            checkpoint.getScratchpad().putAll(delta);
            checkpoint.setStateDelta(new HashMap<>(delta));
            saveSnapshot(checkpoint);
            log.debug("[Checkpointer] 增量差分更新完成: executionId={}, checkpointId={}", executionId, checkpointId);
        }
    }

    @Override
    public AgentCheckpoint loadCheckpoint(String executionId, String checkpointId) {
        if (executionId == null || checkpointId == null) {
            return null;
        }

        // 1. 尝试从 Redis 热缓存读取
        if (redisTemplate != null) {
            try {
                String key = REDIS_CHECKPOINT_PREFIX + executionId + ":" + checkpointId;
                String json = redisTemplate.opsForValue().get(key);
                if (json != null && !json.isBlank()) {
                    return objectMapper.readValue(json, AgentCheckpoint.class);
                }
            } catch (Exception e) {
                log.warn("[Checkpointer] Redis 读取快照异常，降级查库: {}", e.getMessage());
            }
        }

        // 2. 尝试从 PostgreSQL 读取
        AgentCheckpoint dbCheckpoint = loadFromPostgres(executionId, checkpointId);
        if (dbCheckpoint != null) {
            return dbCheckpoint;
        }

        // 3. 从内存兜底读取
        Map<String, AgentCheckpoint> map = memoryStorage.get(executionId);
        if (map != null) {
            return map.get(checkpointId);
        }

        return null;
    }

    @Override
    public AgentCheckpoint loadLatestCheckpoint(String executionId) {
        if (executionId == null) {
            return null;
        }

        if (redisTemplate != null) {
            try {
                String latestId = redisTemplate.opsForValue().get(REDIS_LATEST_PREFIX + executionId);
                if (latestId != null && !latestId.isBlank()) {
                    return loadCheckpoint(executionId, latestId);
                }
            } catch (Exception e) {
                log.warn("[Checkpointer] Redis 获取最新 CheckpointId 失败: {}", e.getMessage());
            }
        }

        // 从内存兜底获取最后一个
        Map<String, AgentCheckpoint> map = memoryStorage.get(executionId);
        if (map != null && !map.isEmpty()) {
            List<AgentCheckpoint> list = new ArrayList<>(map.values());
            return list.get(list.size() - 1);
        }

        return null;
    }

    @Override
    public AgentCheckpoint forkFromCheckpoint(String sourceExecutionId, String checkpointId, String newExecutionId) {
        AgentCheckpoint base = loadCheckpoint(sourceExecutionId, checkpointId);
        if (base == null) {
            throw new IllegalArgumentException("源快照不存在: executionId=" + sourceExecutionId + ", checkpointId=" + checkpointId);
        }

        String forkedCheckpointId = "chk-" + UUID.randomUUID().toString().substring(0, 8);
        AgentCheckpoint forked = new AgentCheckpoint();
        forked.setExecutionId(newExecutionId);
        forked.setCheckpointId(forkedCheckpointId);
        forked.setParentCheckpointId(base.getCheckpointId()); // 溯源建立因果树分支
        forked.setSessionId(base.getSessionId());
        forked.setUserId(base.getUserId());
        forked.setState(WorkflowState.EXECUTING);
        forked.setStepCursor(base.getStepCursor());
        forked.setCallStack(new ArrayList<>(base.getCallStack()));
        forked.setScratchpad(new HashMap<>(base.getScratchpad()));
        forked.setStepSnapshots(new ArrayList<>(base.getStepSnapshots()));
        forked.setCreatedAt(Instant.now());

        Map<String, String> meta = new HashMap<>(base.getMetadata());
        meta.put("forked_from_execution_id", sourceExecutionId);
        meta.put("forked_from_checkpoint_id", checkpointId);
        meta.put("fork_type", "time_travel_branch");
        forked.setMetadata(meta);

        saveSnapshot(forked);
        log.info("[Checkpointer] 🌟 成功时光倒流衍生分支: 源 [{}:{}] -> 新分支 [{}:{}]",
                sourceExecutionId, checkpointId, newExecutionId, forkedCheckpointId);

        return forked;
    }

    @Override
    public List<AgentCheckpoint> getCheckpointHistory(String executionId) {
        Map<String, AgentCheckpoint> map = memoryStorage.get(executionId);
        if (map != null) {
            return new ArrayList<>(map.values());
        }
        return Collections.emptyList();
    }

    // =========================================================================
    // 内部 PostgreSQL 冷数据落库辅助（表结构参见 schema-checkpoint.sql）
    // =========================================================================

    private void persistToPostgres(AgentCheckpoint cp, String json) {
        if (jdbcTemplate == null) return;
        try {
            String sql = """
                    INSERT INTO agent_execution_checkpoint
                    (execution_id, checkpoint_id, parent_checkpoint_id, session_id, user_id, state, step_cursor, snapshot_json, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, CURRENT_TIMESTAMP)
                    ON CONFLICT (execution_id, checkpoint_id)
                    DO UPDATE SET state = EXCLUDED.state, snapshot_json = EXCLUDED.snapshot_json;
                    """;
            jdbcTemplate.update(sql, cp.getExecutionId(), cp.getCheckpointId(),
                    cp.getParentCheckpointId(), cp.getSessionId(), cp.getUserId(),
                    cp.getState().name(), cp.getStepCursor(), json);
        } catch (Exception e) {
            // PostgreSQL 处于未就绪或非 PostgreSQL 数据源时，仅记录日志，不阻断主链路
            log.debug("[Checkpointer] PostgreSQL 落库静默降级: {}", e.getMessage());
        }
    }

    private AgentCheckpoint loadFromPostgres(String executionId, String checkpointId) {
        if (jdbcTemplate == null) return null;
        try {
            String sql = "SELECT snapshot_json FROM agent_execution_checkpoint WHERE execution_id = ? AND checkpoint_id = ?";
            String json = jdbcTemplate.queryForObject(sql, String.class, executionId, checkpointId);
            if (json != null) {
                return objectMapper.readValue(json, AgentCheckpoint.class);
            }
        } catch (Exception e) {
            log.debug("[Checkpointer] PostgreSQL 查询快照静默降级: {}", e.getMessage());
        }
        return null;
    }
}
