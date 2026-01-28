package com.sistemagestionapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * En esta clase centralizo la configuración de seguridad de la aplicación mediante Spring Security.
 *
 * Defino qué rutas son públicas y cuáles requieren autenticación, así como el mecanismo de inicio de sesión
 * basado en formulario y el proceso de cierre de sesión. Mi objetivo es asegurar el acceso a los recursos
 * principales del sistema (incluyendo el asistente de despliegue y su API) sin exponer funcionalidades internas
 * a usuarios no autenticados.
 *
 * También proporciono el codificador de contraseñas que utilizo en el proceso de registro y validación de credenciales,
 * asegurando que las contraseñas se almacenen de forma segura.
 *
 * @author David Tomé Arnaiz
 */
@Configuration
public class WebSecurityConfig {

    /**
     * En este método construyo y registro la cadena de filtros de seguridad que aplica Spring Security.
     *
     * Desactivo la protección CSRF para simplificar el funcionamiento en este escenario, y defino reglas de autorización:
     * permito el acceso público a recursos estáticos y a las rutas de autenticación/registro, mientras que exijo
     * autenticación para el asistente de despliegue, su API y el resto de endpoints.
     *
     * Además, configuro el formulario de login indicando:
     * - la página de login,
     * - el endpoint de procesamiento,
     * - los nombres de los parámetros del formulario (correo y password),
     * - la redirección tras autenticación correcta al listado de aplicaciones,
     * - y la redirección en caso de error.
     *
     * Por último, defino el comportamiento del logout, redirigiendo al usuario a la pantalla de login una vez cerrada la sesión.
     *
     * @param http objeto de configuración de seguridad HTTP proporcionado por Spring Security
     * @return la cadena de filtros de seguridad construida y lista para aplicarse en la aplicación
     * @throws Exception si se produce un error al construir la configuración de seguridad
     * @author David Tomé Arnaiz
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas
                        .requestMatchers(
                                "/login",
                                "/registro",
                                "/logout",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()

                        // El asistente y su API requieren login
                        .requestMatchers("/wizard/**", "/api/wizard/**").authenticated()

                        // Cualquier otra ruta también requiere login
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("correo")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/aplicaciones", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    /**
     * En este método defino el codificador de contraseñas que utilizo en la aplicación.
     *
     * Empleo BCrypt como algoritmo de hashing, ya que incorpora un factor de coste configurable y está diseñado
     * para dificultar ataques de fuerza bruta. De este modo, aseguro que las contraseñas no se almacenen en texto plano
     * y que la verificación de credenciales se realice de manera segura.
     *
     * @return instancia de {@link PasswordEncoder} basada en BCrypt
     * @author David Tomé Arnaiz
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}