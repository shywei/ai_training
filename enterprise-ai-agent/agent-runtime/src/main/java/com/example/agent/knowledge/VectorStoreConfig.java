package com.example.agent.knowledge;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * pgvector 向量存储配置
 *
 * 当前项目主数据库是 MySQL，但 pgvector 需要 PostgreSQL。
 * 关键：不暴露 DataSource Bean，避免和 MySQL 主数据源冲突。
 * 只暴露 JdbcTemplate 和 PgVectorStore。
 */
@Configuration
public class VectorStoreConfig {

    @Value("${spring.datasource.pgvector.url}")
    private String pgUrl;

    @Value("${spring.datasource.pgvector.username}")
    private String pgUsername;

    @Value("${spring.datasource.pgvector.password}")
    private String pgPassword;

    /**
     * pgvector 专用 JdbcTemplate（不暴露 DataSource Bean，避免干扰 MyBatis）
     */
    @Bean
    public JdbcTemplate pgvectorJdbcTemplate() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(pgUrl);
        ds.setUsername(pgUsername);
        ds.setPassword(pgPassword);
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setMaximumPoolSize(5);
        ds.setPoolName("pgvector-pool");
        return new JdbcTemplate(ds);
    }

    /**
     * PgVectorStore Bean
     * Spring AI 1.0.0-M4 构造函数：JdbcTemplate 在前，EmbeddingModel 在后
     */
    @Bean
    public PgVectorStore pgVectorStore(JdbcTemplate pgvectorJdbcTemplate,
                                        EmbeddingModel embeddingModel) {
        return new PgVectorStore(pgvectorJdbcTemplate, embeddingModel);
    }
}
