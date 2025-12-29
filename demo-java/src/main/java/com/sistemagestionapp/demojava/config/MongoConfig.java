package com.sistemagestionapp.demojava.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.util.StringUtils;

@Configuration
@Profile("mongo")
@EnableMongoRepositories(basePackages = "com.sistemagestionapp.demojava.repository.mongo")
public class MongoConfig extends AbstractMongoClientConfiguration {

    /**
     * ✅ Fuente principal (Atlas/remoto o local si la pasas por env):
     * - Se rellena con SPRING_DATA_MONGODB_URI o spring.data.mongodb.uri
     * - En tu compose ya la mapeas desde DB_URI
     */
    @Value("${spring.data.mongodb.uri:}")
    private String mongoUri;

    // Fallback local (solo si mongoUri viene vacío)
    @Value("${DB_HOST:mongo}")
    private String mongoHost;

    @Value("${DB_PORT:27017}")
    private int mongoPort;

    @Value("${DB_NAME:demo}")
    private String mongoDatabase;

    @Value("${DB_USER:demo}")
    private String mongoUsername;

    @Value("${DB_PASSWORD:demo}")
    private String mongoPassword;

    @Value("${DB_AUTH_DB:admin}")
    private String authDatabase;

    @Override
    protected String getDatabaseName() {
        // Si viene un URI, el database puede ir dentro del URI.
        // Aun así, devolver uno por defecto ayuda a AbstractMongoClientConfiguration.
        return mongoDatabase;
    }

    @Bean
    @Override
    public MongoClient mongoClient() {

        // ✅ Si hay URI, se usa SIEMPRE (prioridad real)
        if (StringUtils.hasText(mongoUri)) {
            return MongoClients.create(mongoUri);
        }

        // ✅ Fallback local: construimos URI con defaults
        String fallbackUri = String.format(
                "mongodb://%s:%s@%s:%d/%s?authSource=%s",
                mongoUsername,
                mongoPassword,
                mongoHost,
                mongoPort,
                mongoDatabase,
                authDatabase
        );

        return MongoClients.create(fallbackUri);
    }
}