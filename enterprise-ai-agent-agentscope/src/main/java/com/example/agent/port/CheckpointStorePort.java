package com.example.agent.port;

import com.example.agent.domain.AgentCheckpoint;

import java.util.List;
import java.util.Map;

/**
 * 六边形出站端口：智能体状态快照与检查点持久化存储端口（Outbound Driven Port）
 *
 * 对应第 09 课 P07~P10：
 * 解耦 Agent 运行时与具体存储介质（Redis/PostgreSQL/MySQL）。
 * 明确区分全量状态快照（Full Snapshot）与增量差分（State Delta），
 * 并提供时光倒流与分支回滚（Forking）的核心契约。
 */
public interface CheckpointStorePort {

    /**
     * 持久化保存全量检查点快照
     *
     * @param checkpoint 状态快照
     */
    void saveSnapshot(AgentCheckpoint checkpoint);

    /**
     * 保存增量状态差分（减少持久化网络开销与存储占用）
     *
     * @param executionId  执行 ID
     * @param checkpointId 检查点 ID
     * @param delta        步骤变量增量
     */
    void saveDelta(String executionId, String checkpointId, Map<String, Object> delta);

    /**
     * 根据 Checkpoint ID 加载指定版本的完整执行快照
     *
     * @param executionId  执行 ID
     * @param checkpointId 检查点版本 ID
     * @return 复水所需的完整快照
     */
    AgentCheckpoint loadCheckpoint(String executionId, String checkpointId);

    /**
     * 获取指定执行链路的最新有效检查点
     *
     * @param executionId 执行 ID
     * @return 最新快照
     */
    AgentCheckpoint loadLatestCheckpoint(String executionId);

    /**
     * 分支衍生回滚（Forking / Time-Travel 时光倒流）
     *
     * 从某一历史失败或备选 Checkpoint 分叉出一个全新的独立执行流，探索不同决策路径
     *
     * @param sourceExecutionId 原执行 ID
     * @param checkpointId      需要回滚到的目标快照 ID
     * @param newExecutionId    新分叉的执行链路 ID
     * @return 新分支初始化的 Checkpoint
     */
    AgentCheckpoint forkFromCheckpoint(String sourceExecutionId, String checkpointId, String newExecutionId);

    /**
     * 查询指定执行链路的完整 Checkpoint 因果历史列表
     *
     * @param executionId 执行 ID
     * @return 按时间正序排列的检查点列表
     */
    List<AgentCheckpoint> getCheckpointHistory(String executionId);
}
