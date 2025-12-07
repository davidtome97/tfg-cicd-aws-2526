package com.sistemagestionapp.service;

import com.sistemagestionapp.model.Aplicacion;
import com.sistemagestionapp.model.Usuario;
import com.sistemagestionapp.repository.AplicacionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio de acceso a la entidad Aplicacion.
 * Lo uso para:
 *  - Listar aplicaciones de un usuario.
 *  - Guardar/actualizar una aplicación.
 *  - Obtener por id.
 *  - Eliminar.
 */
@Service
public class AplicacionService {

    private final AplicacionRepository aplicacionRepository;

    public AplicacionService(AplicacionRepository aplicacionRepository) {
        this.aplicacionRepository = aplicacionRepository;
    }

    /**
     * Devuelvo todas las aplicaciones cuyo propietario es el usuario indicado.
     *
     * @param propietario usuario dueño de las aplicaciones.
     * @return lista de aplicaciones.
     */
    public List<Aplicacion> listarPorPropietario(Usuario propietario) {
        return aplicacionRepository.findByPropietario(propietario);
    }

    /**
     * Guardo o actualizo una aplicación.
     *
     * @param aplicacion entidad a persistir.
     * @return aplicación persistida.
     */
    public Aplicacion guardar(Aplicacion aplicacion) {
        return aplicacionRepository.save(aplicacion);
    }

    /**
     * Obtengo una aplicación por id, o lanzo excepción si no existe.
     */
    public Aplicacion obtenerPorId(Long id) {
        return aplicacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aplicación no encontrada con id " + id));
    }

    /**
     * Elimino una aplicación por id.
     */
    public void eliminar(Long id) {
        aplicacionRepository.deleteById(id);
    }
}