package com.sistemagestionapp.demojava.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
@ConditionalOnProperty(name = "app.db.engine", havingValue = "mysql")
class MySQLConfig {

    @Bean
    public DataSource mysqlDataSource(Environment env) {

        String url = "jdbc:mysql://" +
                env.getProperty("app.db.host") + ":" +
                env.getProperty("app.db.port") + "/" +
                env.getProperty("app.db.name");

        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setJdbcUrl(url);
        ds.setUsername(env.getProperty("app.db.user"));
        ds.setPassword(env.getProperty("app.db.password"));

        return ds;
    }
}

@Configuration
@ConditionalOnProperty(name = "app.db.engine", havingValue = "postgres")
class PostgreSQLConfig {

    @Bean
    public DataSource postgresDataSource(Environment env) {

        String url = "jdbc:postgresql://" +
                env.getProperty("app.db.host") + ":" +
                env.getProperty("app.db.port") + "/" +
                env.getProperty("app.db.name");

        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setJdbcUrl(url);
        ds.setUsername(env.getProperty("app.db.user"));
        ds.setPassword(env.getProperty("app.db.password"));

        return ds;
    }
}