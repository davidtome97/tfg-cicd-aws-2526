package com.sistemagestionapp.demojava;

import com.sistemagestionapp.demojava.model.Usuario;
import com.sistemagestionapp.demojava.repository.jpa.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("!mongo")
public class DataInitializerSql {

    private static final Logger log = LoggerFactory.getLogger(DataInitializerSql.class);

    @Bean
    public CommandLineRunner initUsersSql(UsuarioRepository usuarioRepository,
                                          PasswordEncoder passwordEncoder) {
        return args -> {

            // ======================
            // Usuario admin en SQL (MySQL / Postgres)
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

            // ✅ Importante: aquí NO hay nada de Mongo
        };
    }
}