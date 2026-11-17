-- ============================================================
-- Spring AI 2.0 / Checkpointer 模式执行状态快照表 (PostgreSQL)
-- 对应第 09 课 P07~P10：有状态 Agent 的序列化与断点复水
-- ============================================================

CREATE TABLE IF NOT EXISTS agent_execution_checkpoint (
    execution_id VARCHAR(64) NOT NULL,
    checkpoint_id VARCHAR(64) NOT NULL,
    parent_checkpoint_id VARCHAR(64),
    session_id VARCHAR(64),
    user_id VARCHAR(64),
    state VARCHAR(32) NOT NULL,
    step_cursor INT DEFAULT 0,
    snapshot_json JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (execution_id, checkpoint_id)
);

CREATE INDEX IF NOT EXISTS idx_agent_checkpoint_exec ON agent_execution_checkpoint(execution_id);
CREATE INDEX IF NOT EXISTS idx_agent_checkpoint_parent ON agent_execution_checkpoint(parent_checkpoint_id);
CREATE INDEX IF NOT EXISTS idx_agent_checkpoint_created ON agent_execution_checkpoint(created_at);

COMMENT ON TABLE agent_execution_checkpoint IS '智能体执行状态快照检查点表 (Checkpointer)';
COMMENT ON COLUMN agent_execution_checkpoint.execution_id IS '任务执行唯一链路ID';
COMMENT ON COLUMN agent_execution_checkpoint.checkpoint_id IS '快照版本ID';
COMMENT ON COLUMN agent_execution_checkpoint.parent_checkpoint_id IS '父快照版本ID (用于因果DAG溯源与分支回滚)';
COMMENT ON COLUMN agent_execution_checkpoint.state IS '当前状态机状态 (PLANNING/EXECUTING/WAITING_USER等)';
COMMENT ON COLUMN agent_execution_checkpoint.step_cursor IS '当前执行游标步数';
COMMENT ON COLUMN agent_execution_checkpoint.snapshot_json IS '全量执行上下文快照JSON (CallStack/Scratchpad/Steps)';
