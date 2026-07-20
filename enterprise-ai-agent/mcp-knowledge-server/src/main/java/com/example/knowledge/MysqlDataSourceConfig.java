package com.example.knowledge;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * MySQL 数据源配置（用户画像表）。
 *
 * mcp-knowledge-server 使用两个数据源：
 * - 默认数据源：PostgreSQL + pgvector（Spring Boot 自动配置）
 * - MySQL 数据源：用户画像表（本类手动配置）
 */
@Configuration
public class MysqlDataSourceConfig {

    @Bean("mysqlDataSource")
    public DataSource mysqlDataSource(
            @Value("${mysql.datasource.url}") String url,
            @Value("${mysql.datasource.username}") String username,
            @Value("${mysql.datasource.password}") String password) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setPoolName("mysql-pool");
        return ds;
    }

    @Bean("mysqlJdbcTemplate")
    public JdbcTemplate mysqlJdbcTemplate(DataSource mysqlDataSource) {
        return new JdbcTemplate(mysqlDataSource);
    }
}
