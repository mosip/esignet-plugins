package org.mock.esignet.plugin.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class MockDBConfig {
    @Value("${org.mock.esignet.plugin.db-url}")
    private String dbURL;

    @Value("${org.mock.esignet.plugin.db-username}")
    private String dbUsername;

    @Value("${org.mock.esignet.plugin.db-password}")
    private String dbPassword;

    public DataSource dataSource() {
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setJdbcUrl(dbURL);
        hikariDataSource.setUsername(dbUsername);
        hikariDataSource.setPassword(dbPassword);
        hikariDataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        return hikariDataSource;
    }

    @Bean("mockPluginJdbcTemplate")
    public JdbcTemplate mockPluginJdbcTemplate() {
        return new JdbcTemplate(dataSource());
    }
}
