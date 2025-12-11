package com.sistemagestionapp.demojava.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.host}")
    private String mongoHost;

    @Value("${spring.data.mongodb.port}")
    private int mongoPort;

    @Value("${spring.data.mongodb.database}")
    private String mongoDatabase;

    @Value("${spring.data.mongodb.username}")
    private String mongoUsername;

    @Value("${spring.data.mongodb.password}")
    private String mongoPassword;

    @Value("${spring.data.mongodb.authentication-database}")
    private String authDatabase;

    @Override
    protected String getDatabaseName() {
        return mongoDatabase;
    }

    @Bean
    @Override
    public MongoClient mongoClient() {
        // URI apuntando SIEMPRE al servicio Docker "mongo", con usuario/contraseña
        String uri = String.format(
                "mongodb://%s:%s@%s:%d/%s?authSource=%s",
                mongoUsername,
                mongoPassword,
                mongoHost,
                mongoPort,
                mongoDatabase,
                authDatabase
        );

        return MongoClients.create(uri);
    }
}