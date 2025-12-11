package com.sistemagestionapp.demojava.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.sistemagestionapp.demojava.repository",
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager"
)
public class JpaConfig {

    @Value("${app.db.engine}")
    private String engine;

    @Value("${app.db.host}")
    private String host;

    @Value("${app.db.port}")
    private int port;

    @Value("${app.db.name}")
    private String dbName;

    @Value("${app.db.user}")
    private String user;

    @Value("${app.db.password}")
    private String password;

    @Value("${spring.jpa.hibernate.ddl-auto:create}")
    private String ddlAuto;

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        String url;
        String driverClassName;

        if ("postgres".equalsIgnoreCase(engine)) {
            url = String.format("jdbc:postgresql://%s:%d/%s", host, port, dbName);
            driverClassName = "org.postgresql.Driver";
        } else {
            // Por defecto MySQL
            url = String.format("jdbc:mysql://%s:%d/%s", host, port, dbName);
            driverClassName = "com.mysql.cj.jdbc.Driver";
        }

        config.setJdbcUrl(url);
        config.setDriverClassName(driverClassName);
        config.setUsername(user);
        config.setPassword(password);

        return new HikariDataSource(config);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();

        emf.setDataSource(dataSource);
        emf.setPackagesToScan("com.sistemagestionapp.demojava.model");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        emf.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.hbm2ddl.auto", ddlAuto);

        if ("postgres".equalsIgnoreCase(engine)) {
            props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        } else {
            props.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        }

        emf.setJpaPropertyMap(props);
        emf.setPersistenceUnitName("default");

        return emf;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}