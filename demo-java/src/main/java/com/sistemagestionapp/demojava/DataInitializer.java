package com.sistemagestionapp.demojava;

import com.sistemagestionapp.demojava.model.Usuario;
import com.sistemagestionapp.demojava.model.mongo.UsuarioMongo;
import com.sistemagestionapp.demojava.repository.UsuarioRepository;
import com.sistemagestionapp.demojava.repository.mongo.UsuarioMongoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    // app.db.engine viene de application.properties (o del env DB_ENGINE)
    @Value("${app.db.engine:mysql}")
    private String dbEngine;

    @Bean
    public CommandLineRunner initUsers(UsuarioRepository usuarioRepository,
                                       UsuarioMongoRepository usuarioMongoRepository,
                                       PasswordEncoder passwordEncoder) {
        return args -> {

            // ======================
            // 1) Usuario admin en SQL (MySQL / Postgres)
            // ======================
            String correoAdminSql = "admin@admin.com";

            if (!usuarioRepository.existsByCorreo(correoAdminSql)) {
                Usuario adminSql = new Usuario(
                        "Admin",
                        correoAdminSql,
                        passwordEncoder.encode("admin")
                );
                usuarioRepository.save(adminSql);
                log.info("✅ Usuario admin SQL creado ({} / admin)", correoAdminSql);
            } else {
                log.info("ℹ️ Usuario admin SQL ya existe, no se crea uno nuevo");
            }

            // ======================
            // 2) Usuario admin en Mongo (solo si DB_ENGINE = mongo)
            // ======================
            if ("mongo".equalsIgnoreCase(dbEngine)) {
                String correoAdminMongo = "admin@mongo.com";

                // Usamos el count() genérico del repositorio
                long totalUsuariosMongo = usuarioMongoRepository.count();

                if (totalUsuariosMongo == 0) {
                    UsuarioMongo adminMongo = new UsuarioMongo(
                            "AdminMongo",
                            correoAdminMongo,
                            passwordEncoder.encode("admin")
                    );
                    usuarioMongoRepository.save(adminMongo);
                    log.info("✅ Usuario admin Mongo creado ({} / admin)", correoAdminMongo);
                } else {
                    log.info("ℹ️ Ya existen usuarios en Mongo, no se crea el admin por defecto");
                }
            } else {
                log.info("ℹ️ app.db.engine = {}. No se insertan datos en Mongo desde Java.", dbEngine);
            }
        };
    }
}