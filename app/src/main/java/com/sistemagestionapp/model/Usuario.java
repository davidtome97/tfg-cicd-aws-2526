package com.sistemagestionapp.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * En esta entidad represento a un usuario del sistema.
 *
 * Almaceno la información necesaria para la autenticación y gestión de usuarios,
 * así como la relación con las aplicaciones que pertenecen a cada usuario.
 *
 * Cada usuario puede ser propietario de varias aplicaciones, lo que me permite
 * aislar los datos y el progreso del asistente por usuario.
 *
 * @author David Tomé Arnaiz
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre visible del usuario.
     */
    private String nombre;

    /**
     * Correo electrónico del usuario.
     *
     * Se utiliza como identificador único para el proceso de autenticación.
     */
    @Column(unique = true)
    private String correo;

    /**
     * Contraseña del usuario.
     *
     * Se almacena en formato cifrado y nunca en texto plano.
     */
    private String password;

    /**
     * Aplicaciones de las que el usuario es propietario.
     */
    @OneToMany(mappedBy = "propietario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Aplicacion> aplicaciones = new ArrayList<>();

    public Usuario() {
    }

    /**
     * En este constructor inicializo un usuario con sus datos básicos.
     *
     * @param nombre nombre del usuario
     * @param correo correo electrónico del usuario
     * @param password contraseña del usuario (será cifrada en el servicio)
     */
    public Usuario(String nombre, String correo, String password) {
        this.nombre = nombre;
        this.correo = correo;
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<Aplicacion> getAplicaciones() {
        return aplicaciones;
    }

    public void setAplicaciones(List<Aplicacion> aplicaciones) {
        this.aplicaciones = aplicaciones;
    }
}
