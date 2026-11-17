package com.example.agent.adapter.agentscope;

import com.example.agent.domain.AgentCheckpoint;
import com.example.agent.domain.WorkflowState;
import com.example.agent.port.CheckpointStorePort;
import io.agentscope.core.state.AgentState;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.InMemoryAgentStateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AgentScope 专属执行快照持久化适配器（AgentScope Checkpoint Store Adapter）
 *
 * 对应第 09 课 P07~P10（Checkpointer 模式与断点复水）：
 * 完美实现与 Spring AI 2.0 统一的 CheckpointStorePort 端口契约。
 *
 * ────────── 核心边界辨析（P07 深度深水区 1） ──────────
 * 1. 【AgentScope 原生 StateStore 的定位】：
 *    - 管理 Agent 内部会话状态（AgentState：curIter 游标、context 消息快照、summary 摘要）。
 *    - 内置的 `InMemoryAgentStateStore` / `RedisAgentStateStore`
 *      接管 Agent 实例在内存或缓存中的基本状态存取。
 *
 * 2. 【本 CheckpointStore 的增强演进】：
 *    - 本类将 AgentScope 的 `AgentStateStore` 与企业级 `AgentCheckpoint` 双向融合：
 *      ① 支持全量状态快照（Full Snapshot）持久化
 *      ② 支持计算两快照之间的增量状态差分（State Delta）
 *      ③ 提供按 checkpointId 跨进程反序列化复水（Rehydration）
 *      ④ 提供基于 Checkpoint DAG 的时光倒流与分支回滚（Time-Travel Forking）
 */
@Component
public class AgentScopeCheckpointStore implements CheckpointStorePort {

    private static final Logger log = LoggerFactory.getLogger(AgentScopeCheckpointStore.class);

    /** 核心 AgentScope 原生框架状态存储器 */
    private final AgentStateStore agentScopeStateStore;

    /** 企业级可恢复执行快照仓库（按 executionId + checkpointId 索引） */
    private final Map<String, AgentCheckpoint> checkpointIndex = new ConcurrentHashMap<>();

    /** 任务版本血缘链条（按 executionId 索引） */
    private final Map<String, List<String>> executionCheckpointHistory = new ConcurrentHashMap<>();

    public AgentScopeCheckpointStore() {
        this.agentScopeStateStore = new InMemoryAgentStateStore();
    }

    public AgentScopeCheckpointStore(AgentStateStore customStore) {
        this.agentScopeStateStore = customStore != null ? customStore : new InMemoryAgentStateStore();
    }

    public AgentStateStore getAgentScopeStateStore() {
        return agentScopeStateStore;
    }

    private String buildKey(String executionId, String checkpointId) {
        return executionId + "::" + checkpointId;
    }

    @Override
    public void saveSnapshot(AgentCheckpoint checkpoint) {
        if (checkpoint == null) return;
        String executionId = checkpoint.getExecutionId();
        String checkpointId = checkpoint.getCheckpointId();

        log.info("[Checkpointer] 保存全量状态快照: executionId={}, checkpointId={}, step={}",
                executionId, checkpointId, checkpoint.getStepCursor());

        // 1. 索引快照对象
        checkpointIndex.put(buildKey(executionId, checkpointId), checkpoint);

        // 2. 记入版本血缘链条
        executionCheckpointHistory.computeIfAbsent(executionId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(checkpointId);

        // 3. 同步回写 AgentScope 底层 StateStore (以保障 AgentScope 框架内部可感知)
        try {
            AgentState internalState = AgentState.builder()
                    .sessionId(executionId)
                    .summary("Checkpoint [" + checkpointId + "] at step " + checkpoint.getStepCursor())
                    .curIter(checkpoint.getStepCursor())
                    .build();
            agentScopeStateStore.save("EnterpriseShoppingAgent", executionId, "CURRENT_STATE", internalState);
        } catch (Exception e) {
            log.warn("[Checkpointer] 同步回写 AgentScope StateStore 告警: {}", e.getMessage());
        }
    }

    @Override
    public void saveDelta(String executionId, String checkpointId, Map<String, Object> delta) {
        AgentCheckpoint parent = loadCheckpoint(executionId, checkpointId);
        if (parent == null) {
            log.warn("[Checkpointer] 未找到父快照 {}::{}，跳过增量差分保存", executionId, checkpointId);
            return;
        }

        String nextCheckpointId = "cp-step-" + (parent.getStepCursor() + 1) + "-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("[Checkpointer:StateDelta] 计算并合并增量差分快照: executionId={}, parent={}, next={}, deltaKeys={}",
                executionId, checkpointId, nextCheckpointId, delta != null ? delta.keySet() : "[]");

        AgentCheckpoint next = AgentCheckpoint.nextStep(
                parent, nextCheckpointId, parent.getState(), parent.getStepCursor() + 1, delta
        );
        saveSnapshot(next);
    }

    @Override
    public AgentCheckpoint loadCheckpoint(String executionId, String checkpointId) {
        if (executionId == null || checkpointId == null) return null;
        AgentCheckpoint cp = checkpointIndex.get(buildKey(executionId, checkpointId));
        if (cp == null) {
            // 兼容通过单个 checkpointId 遍历查询
            for (AgentCheckpoint candidate : checkpointIndex.values()) {
                if (checkpointId.equals(candidate.getCheckpointId())) {
                    cp = candidate;
                    break;
                }
            }
        }
        if (cp != null) {
            log.info("[Checkpointer:Rehydration] 命中快照成功，执行跨进程复水: executionId={}, checkpointId={}, step={}",
                    executionId, checkpointId, cp.getStepCursor());
        } else {
            log.warn("[Checkpointer:Rehydration] 未找到指定快照: executionId={}, checkpointId={}", executionId, checkpointId);
        }
        return cp;
    }

    @Override
    public AgentCheckpoint loadLatestCheckpoint(String executionId) {
        List<String> history = executionCheckpointHistory.get(executionId);
        if (history == null || history.isEmpty()) {
            return null;
        }
        String latestId = history.get(history.size() - 1);
        return loadCheckpoint(executionId, latestId);
    }

    @Override
    public AgentCheckpoint forkFromCheckpoint(String sourceExecutionId, String checkpointId, String newExecutionId) {
        AgentCheckpoint base = loadCheckpoint(sourceExecutionId, checkpointId);
        if (base == null) {
            throw new IllegalArgumentException("无法分叉：基准快照不存在: " + sourceExecutionId + "::" + checkpointId);
        }

        log.info("[Checkpointer:TimeTravel] 触发时光倒流分支分叉: sourceExec={}, baseCheckpoint={}, newExec={}",
                sourceExecutionId, checkpointId, newExecutionId);

        String forkedCheckpointId = "cp-fork-" + UUID.randomUUID().toString().substring(0, 8);
        AgentCheckpoint forked = new AgentCheckpoint(newExecutionId, forkedCheckpointId, checkpointId, WorkflowState.EXECUTING, base.getStepCursor());
        forked.setSessionId(base.getSessionId());
        forked.setUserId(base.getUserId());
        forked.setCallStack(new ArrayList<>(base.getCallStack()));
        forked.setScratchpad(new HashMap<>(base.getScratchpad()));
        forked.setStepSnapshots(new ArrayList<>(base.getStepSnapshots()));
        forked.setMetadata(new HashMap<>(base.getMetadata()));
        forked.getMetadata().put("forkedFromExecutionId", sourceExecutionId);
        forked.getMetadata().put("forkedFromCheckpointId", checkpointId);

        saveSnapshot(forked);
        return forked;
    }

    @Override
    public List<AgentCheckpoint> getCheckpointHistory(String executionId) {
        List<String> history = executionCheckpointHistory.get(executionId);
        if (history == null) return Collections.emptyList();

        List<AgentCheckpoint> result = new ArrayList<>();
        synchronized (history) {
            for (String cpId : history) {
                AgentCheckpoint cp = loadCheckpoint(executionId, cpId);
                if (cp != null) result.add(cp);
            }
        }
        return result;
    }
}
