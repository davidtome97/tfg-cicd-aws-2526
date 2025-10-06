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
 *
 * He optado por desactivar CSRF porque mi proyecto no lo necesita por ahora,
 * y también he personalizado la página de login para adaptarla a mi interfaz.
 *
 * Además, uso BCrypt para encriptar las contraseñas de forma segura.
 */
@Configuration
public class WebSecurityConfig {

    /**
     * Esta función define las reglas de seguridad de mi aplicación.
     * Configuro aquí qué rutas están protegidas y cuáles no, cómo es el formulario de login
     * y qué pasa al cerrar sesión.
     *
     * @param http el objeto HttpSecurity que me permite configurar la seguridad web.
     * @return la cadena de filtros de seguridad configurada.
     * @throws Exception en caso de error al construir la configuración.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Desactivo CSRF porque no lo necesito
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Estas rutas son accesibles sin iniciar sesión
                        .requestMatchers("/login", "/logout", "/css/**", "/js/**", "/images/**").permitAll()
                        // Todo lo demás requiere que el usuario esté autenticado
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login
                        // Página personalizada de login
                        .loginPage("/login")
                        // URL para procesar el login
                        .loginProcessingUrl("/login")
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
     * Defino el codificador de contraseñas que uso para encriptar las claves de los usuarios.
     * En este caso utilizo BCrypt, que es una opción segura y recomendada por Spring.
     *
     * @return un codificador de contraseñas con algoritmo BCrypt.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}