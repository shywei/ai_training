-- ============================================================
-- 第六课：向量存储表（pgvector）
-- 需要先安装 PostgreSQL 并启用 pgvector 扩展
-- ============================================================

-- 启用 pgvector 扩展（需要超级用户权限）
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================================
-- 1. 知识库表：存储商品知识的 Embedding
--    表名和列名必须匹配 Spring AI PgVectorStore 的预期：
--    表名：vector_store（可配置）
--    列名：id, content, metadata, embedding
-- ============================================================

CREATE TABLE IF NOT EXISTS vector_store (
    id          VARCHAR(255) PRIMARY KEY,
    content     TEXT         NOT NULL,
    metadata    JSONB,
    embedding   vector(1024) NOT NULL
);

-- HNSW 索引：适合中小规模，精度高
CREATE INDEX IF NOT EXISTS idx_vs_embedding
    ON vector_store USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

CREATE INDEX IF NOT EXISTS idx_vs_metadata ON vector_store USING gin (metadata jsonb_path_ops);

-- ============================================================
-- 2. 用户记忆表：存储用户画像和对话片段的 Embedding
--    同一个 pgvector 实例，不同的业务用途
-- ============================================================

CREATE TABLE IF NOT EXISTS user_memory (
    id          BIGSERIAL PRIMARY KEY,
    user_id     VARCHAR(64)  NOT NULL,
    memory_type VARCHAR(32)  NOT NULL,   -- preference / conversation / event
    content     TEXT         NOT NULL,   -- 记忆内容（自然语言）
    metadata    JSONB,                   -- 来源会话、时间戳等
    embedding   vector(1024) NOT NULL,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- 按用户+向量检索的复合索引
CREATE INDEX IF NOT EXISTS idx_memory_user_embedding
    ON user_memory USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

CREATE INDEX IF NOT EXISTS idx_memory_user_id ON user_memory (user_id);
CREATE INDEX IF NOT EXISTS idx_memory_type ON user_memory (user_id, memory_type);

-- ============================================================
-- 说明：
-- embedding 维度取决于 Embedding 模型：
--   BAAI/bge-large-zh-v1.5 → 1024
--   text-embedding-ada-002 → 1536
-- 如果换模型，需要 DROP TABLE 重建（维度不可变）
-- ============================================================
