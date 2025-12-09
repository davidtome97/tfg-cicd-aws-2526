package com.sistemagestionapp.demojava.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.beans.factory.annotation.Value;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

@Configuration
@ConditionalOnProperty(name = "app.db.engine", havingValue = "mongo")
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${app.db.host}")
    private String host;

    @Value("${app.db.port}")
    private String port;

    @Value("${app.db.name}")
    private String dbName;

    @Value("${app.db.user}")
    private String user;

    @Value("${app.db.password}")
    private String password;

    @Override
    protected String getDatabaseName() {
        return dbName;
    }

    @Override
    public MongoClient mongoClient() {

        String uri = String.format(
                "mongodb://%s:%s@%s:%s/%s",
                user, password, host, port, dbName
        );

        return MongoClients.create(uri);
    }
}