package com.sistemagestionapp.repository;

import com.sistemagestionapp.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * En este repositorio gestiono el acceso a datos de la entidad {@link Usuario}.
 *
 * Utilizo Spring Data JPA para realizar operaciones CRUD sobre los usuarios del sistema
 * y defino métodos de consulta adicionales basados en el correo electrónico, que actúa
 * como identificador único para la autenticación.
 *
 * @author David Tomé Arnaiz
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Obtengo un usuario a partir de su correo electrónico.
     *
     * Este método se utiliza principalmente durante el proceso de autenticación
     * y para validar la existencia previa de un usuario en el registro.
     *
     * @param correo correo electrónico del usuario
     * @return usuario encontrado, o {@link Optional#empty()} si no existe
     */
    Optional<Usuario> findByCorreo(String correo);
}