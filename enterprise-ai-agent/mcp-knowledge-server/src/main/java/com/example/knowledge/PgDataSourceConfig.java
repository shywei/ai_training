package com.example.knowledge;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * PostgreSQL 数据源配置（vector_store + user_memory 表）。
 *
 * mcp-knowledge-server 使用两个数据源：
 * - PostgreSQL（本类配置）：vector_store（商品知识向量）、user_memory（用户记忆向量）
 * - MySQL（MysqlDataSourceConfig）：user_profile（用户画像）
 *
 * 使用 @Primary 标记 PostgreSQL 为默认数据源，
 * 这样不加 @Qualifier 的 JdbcTemplate 注入默认走 PostgreSQL。
 */
@Configuration
public class PgDataSourceConfig {

    @Primary
    @Bean("pgDataSource")
    public DataSource pgDataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}") String driver) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName(driver);
        ds.setPoolName("pg-pool");
        return ds;
    }

    @Primary
    @Bean("pgJdbcTemplate")
    public JdbcTemplate pgJdbcTemplate(@Qualifier("pgDataSource") DataSource pgDataSource) {
        return new JdbcTemplate(pgDataSource);
    }
}
