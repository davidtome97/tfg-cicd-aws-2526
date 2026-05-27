package com.sistemagestionapp.repository;

import com.sistemagestionapp.model.Aplicacion;
import com.sistemagestionapp.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * En este repositorio gestiono el acceso a datos de la entidad {@link Aplicacion}.
 *
 * Utilizo Spring Data JPA para realizar operaciones CRUD y consultas derivadas
 * relacionadas con las aplicaciones del sistema.
 *
 * @author David Tomé Arnaiz
 */
public interface AplicacionRepository extends JpaRepository<Aplicacion, Long> {

    /**
     * Obtengo todas las aplicaciones pertenecientes a un usuario concreto.
     *
     * Este método me permite aislar los datos por propietario y mostrar
     * únicamente las aplicaciones del usuario autenticado.
     *
     * @param propietario usuario propietario de las aplicaciones
     * @return lista de aplicaciones asociadas al usuario
     */
    List<Aplicacion> findByPropietario(Usuario propietario);
}