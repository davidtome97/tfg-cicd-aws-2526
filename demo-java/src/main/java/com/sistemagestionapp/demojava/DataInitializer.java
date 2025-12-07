package com.sistemagestionapp.demojava;

import com.sistemagestionapp.demojava.model.Usuario;
import com.sistemagestionapp.demojava.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initUsers(UsuarioRepository usuarioRepository,
                                       PasswordEncoder passwordEncoder) {
        return args -> {
            String correoAdmin = "admin@admin.com";
            if (!usuarioRepository.existsByCorreo(correoAdmin)) {
                Usuario admin = new Usuario(
                        "Admin",
                        correoAdmin,
                        passwordEncoder.encode("admin")
                );
                usuarioRepository.save(admin);
                log.info("✅ Usuario admin creado por defecto (admin@admin.com / admin)");
            } else {
                log.info("ℹ️ Usuario admin ya existe");
            }
        };
    }
}