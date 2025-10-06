package com.sistemagestionapp;

import com.sistemagestionapp.model.Usuario;
import com.sistemagestionapp.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

/**
 * Esta clase la utilizo para inicializar la base de datos con un usuario por defecto
 * cuando arranca la aplicación.
 * Compruebo si el usuario "admin" ya existe y, si no es así, lo creo con una contraseña cifrada.
 * Este proceso se realiza automáticamente al ejecutar el proyecto gracias a un {@link CommandLineRunner}.
 *
 * @author David Tomé Arnáiz
 */
@Configuration
public class DataInitializer {

    /**
     * Este método se ejecuta al arrancar la aplicación y se encarga de insertar un usuario
     * administrador por defecto si no existe previamente.
     *
     * @param usuarioRepository el repositorio de usuarios donde hago la comprobación y guardado.
     * @return un {@link CommandLineRunner} que se ejecuta automáticamente en el arranque.
     */

    @Value("${admin.default.password}")
    private String adminPassword;

    @Bean
    public CommandLineRunner initDatabase(UsuarioRepository usuarioRepository) {
        return args -> {
            // Aquí compruebo si ya existe el usuario admin en la base de datos
            if (usuarioRepository.findByCorreo("admin@admin.com").isEmpty()) {
                // Si no existe, creo un nuevo usuario admin con contraseña cifrada
                Usuario usuario = new Usuario();
                usuario.setNombre("Admin");
                usuario.setCorreo("admin@admin.com");
                usuario.setPassword(new BCryptPasswordEncoder().encode(adminPassword));
                usuarioRepository.save(usuario); // guardo el usuario en la base de datos
                System.out.println("✅ Usuario admin creado");
            } else {
                // Si ya existe, lo indico por consola
                System.out.println("ℹ️ Usuario admin ya existe");
            }
        };
    }
}