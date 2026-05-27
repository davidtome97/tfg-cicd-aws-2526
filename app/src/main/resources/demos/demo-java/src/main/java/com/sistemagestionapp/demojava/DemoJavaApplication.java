package com.sistemagestionapp.demojava;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aplicación demo Java que utilizaré como base para generar el ZIP
 * desde mi WebApp de gestión.
 *
 * Más adelante aquí añadiré:
 * - Entidad Producto
 * - Repositorio JPA
 * - Controlador (REST o Thymeleaf) con CRUD sencillo
 */
@SpringBootApplication
public class DemoJavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoJavaApplication.class, args);
    }
}