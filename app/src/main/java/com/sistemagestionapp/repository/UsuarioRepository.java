package com.sistemagestionapp.repository;

import com.sistemagestionapp.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Esta interfaz la utilizo como repositorio para gestionar el acceso a los datos
 * de usuarios en la base de datos.
 * Heredo de {@link JpaRepository}, lo que me permite realizar operaciones CRUD sobre
 * la entidad {@link Usuario} sin necesidad de implementar los métodos básicos.
 * Además, he definido un método personalizado {@code findByCorreo} para buscar un
 * usuario a partir de su correo electrónico.
 *
 * @author David Tomé Arnáiz
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busco un usuario en la base de datos a partir de su correo electrónico.
     *
     * @param correo correo electrónico del usuario que quiero buscar.
     * @return un {@link Optional} que contiene el usuario si existe, o vacío si no se encuentra.
     */
    Optional<Usuario> findByCorreo(String correo);
}