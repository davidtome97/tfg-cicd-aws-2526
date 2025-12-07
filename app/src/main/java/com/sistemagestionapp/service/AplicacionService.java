package com.sistemagestionapp.service;

import com.sistemagestionapp.model.Aplicacion;
import com.sistemagestionapp.model.Usuario;
import com.sistemagestionapp.repository.AplicacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio que utilizo para gestionar la lógica de negocio relacionada
 * con las aplicaciones del usuario.
 *
 * Me permite:
 * - Listar las aplicaciones de un usuario.
 * - Guardar/actualizar una aplicación.
 * - Buscar por id.
 * - Eliminar una aplicación.
 */
@Service
public class AplicacionService {

    @Autowired
    private AplicacionRepository aplicacionRepository;

    /**
     * Devuelvo todas las aplicaciones de un propietario concreto.
     */
    public List<Aplicacion> listarPorPropietario(Usuario propietario) {
        return aplicacionRepository.findByPropietario(propietario);
    }

    /**
     * Guardo una nueva aplicación o actualizo una existente.
     */
    public Aplicacion guardar(Aplicacion aplicacion) {
        return aplicacionRepository.save(aplicacion);
    }

    /**
     * Busco una aplicación por su id.
     */
    public Aplicacion obtenerPorId(Long id) {
        return aplicacionRepository.findById(id).orElse(null);
    }

    /**
     * Elimino una aplicación por su id.
     */
    public void eliminar(Long id) {
        aplicacionRepository.deleteById(id);
    }
}