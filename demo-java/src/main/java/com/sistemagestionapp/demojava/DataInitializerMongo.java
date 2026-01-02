package com.sistemagestionapp.demojava;

import com.sistemagestionapp.demojava.model.mongo.UsuarioMongo;
import com.sistemagestionapp.demojava.repository.mongo.UsuarioMongoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("mongo")
public class DataInitializerMongo {

    private static final Logger log = LoggerFactory.getLogger(DataInitializerMongo.class);

    @Bean
    public CommandLineRunner initUsersMongo(UsuarioMongoRepository usuarioMongoRepository,
                                            PasswordEncoder passwordEncoder) {
        return args -> {

            // ======================
            // Usuario admin en Mongo
            // ======================
            String correoAdminMongo = "admin@mongo.com";

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
        };
    }
}