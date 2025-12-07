package com.sistemagestionapp.service;

import com.sistemagestionapp.model.Aplicacion;
import com.sistemagestionapp.model.ControlDespliegue;
import com.sistemagestionapp.model.EstadoControl;
import com.sistemagestionapp.model.PasoDespliegue;
import com.sistemagestionapp.model.Usuario;
import com.sistemagestionapp.repository.AplicacionRepository;
import com.sistemagestionapp.repository.ControlDespliegueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio para gestionar las aplicaciones de cada usuario y
 * calcular el estado de los pasos de despliegue.
 */
@Service
public class AplicacionService {

    @Autowired
    private AplicacionRepository aplicacionRepository;

    @Autowired
    private ControlDespliegueRepository controlDespliegueRepository;

    /**
     * Devuelvo todas las aplicaciones pertenecientes a un usuario.
     */
    public List<Aplicacion> listarPorUsuario(Usuario propietario) {
        return aplicacionRepository.findByPropietario(propietario);
    }

    /**
     * Número de pasos de despliegue definidos en el sistema.
     * (ahora mismo SONAR_ANALISIS, SONAR_INTEGRACION_GIT, REPOSITORIO_GIT,
     *  IMAGEN_ECR, DESPLIEGUE_EC2, RESUMEN_FINAL -> 6 pasos).
     */
    public int getTotalPasos() {
        return PasoDespliegue.values().length;
    }

    /**
     * Cuenta cuántos pasos de despliegue están en estado OK
     * para una aplicación concreta.
     */
    public int contarPasosOk(Aplicacion aplicacion) {
        List<ControlDespliegue> controles =
                controlDespliegueRepository.findByAplicacion(aplicacion);

        return (int) controles.stream()
                .filter(c -> c.getEstado() == EstadoControl.OK)
                .count();
    }
}