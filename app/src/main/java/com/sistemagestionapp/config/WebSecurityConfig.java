package com.sistemagestionapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Esta clase configura la seguridad de toda mi aplicación usando Spring Security.
 * Aquí defino qué rutas son públicas, cuáles requieren autenticación,
 * cómo se realiza el login y el logout, y qué codificador de contraseñas utilizo.
 */
@Configuration
public class WebSecurityConfig {

    /**
     * Reglas de seguridad de la aplicación:
     * - Rutas públicas: login, registro, recursos estáticos.
     * - Resto de rutas: requieren estar autenticado.
     * - Login con formulario personalizado.
     * - Logout con redirección al login.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Desactivo CSRF porque, de momento, no lo necesito
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas (sin autenticación)
                        .requestMatchers(
                                "/login",
                                "/registro",
                                "/logout",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()
                        // Cualquier otra ruta requiere usuario autenticado
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login
                        // Página personalizada de login
                        .loginPage("/login")
                        // URL donde se envía el POST del formulario de login
                        .loginProcessingUrl("/login")
                        // IMPORTANTE: nombres de los campos del formulario
                        .usernameParameter("correo")
                        .passwordParameter("password")
                        // Página a la que redirige si el login es correcto
                        .defaultSuccessUrl("/usuarios", true)
                        // Página si el login falla
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        // URL para hacer logout
                        .logoutUrl("/logout")
                        // Página a la que redirige tras cerrar sesión
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    /**
     * Codificador de contraseñas (BCrypt).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}