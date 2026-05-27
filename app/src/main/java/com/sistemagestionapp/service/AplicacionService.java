package com.sistemagestionapp.service;

import com.sistemagestionapp.model.Aplicacion;
import com.sistemagestionapp.model.Usuario;
import com.sistemagestionapp.repository.AplicacionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * En este servicio encapsulo la lógica de acceso a datos relacionada con la entidad {@link Aplicacion}.
 *
 * Utilizo este servicio como capa intermedia entre los controladores y el repositorio,
 * centralizando las operaciones de listado, guardado, obtención y eliminación de aplicaciones.
 *
 * @author David Tomé Arnaiz
 */
@Service
public class AplicacionService {

    private final AplicacionRepository aplicacionRepository;

    /**
     * En este constructor inyecto el repositorio de aplicaciones.
     *
     * @param aplicacionRepository repositorio de acceso a datos de aplicaciones
     */
    public AplicacionService(AplicacionRepository aplicacionRepository) {
        this.aplicacionRepository = aplicacionRepository;
    }

    /**
     * Devuelvo todas las aplicaciones cuyo propietario es el usuario indicado.
     *
     * Este método se utiliza para mostrar únicamente las aplicaciones asociadas
     * al usuario autenticado.
     *
     * @param propietario usuario propietario de las aplicaciones
     * @return lista de aplicaciones del usuario
     */
    public List<Aplicacion> listarPorPropietario(Usuario propietario) {
        return aplicacionRepository.findByPropietario(propietario);
    }

    /**
     * Guardo o actualizo una aplicación en la base de datos.
     *
     * @param aplicacion aplicación a persistir
     * @return aplicación persistida
     */
    public Aplicacion guardar(Aplicacion aplicacion) {
        return aplicacionRepository.save(aplicacion);
    }

    /**
     * Obtengo una aplicación a partir de su identificador.
     *
     * Lanzo una excepción si la aplicación no existe.
     *
     * @param id identificador de la aplicación
     * @return aplicación encontrada
     * @throws IllegalArgumentException si la aplicación no existe
     */
    public Aplicacion obtenerPorId(Long id) {
        return aplicacionRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Aplicación no encontrada con id " + id)
                );
    }

    /**
     * Elimino una aplicación a partir de su identificador.
     *
     * @param id identificador de la aplicación
     */
    public void eliminar(Long id) {
        aplicacionRepository.deleteById(id);
    }
}