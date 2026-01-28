package com.sistemagestionapp.service;

import com.sistemagestionapp.model.ControlDespliegue;
import com.sistemagestionapp.model.EstadoControl;
import com.sistemagestionapp.model.PasoDespliegue;
import com.sistemagestionapp.repository.AplicacionRepository;
import com.sistemagestionapp.repository.ControlDespliegueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * En este servicio gestiono el estado y el flujo de los pasos del asistente de despliegue.
 *
 * Me encargo de comprobar si un paso está completado correctamente, decidir si se puede
 * acceder a un paso concreto y registrar el resultado de cada fase del asistente
 * (OK, KO o PENDIENTE).
 *
 * Centralizar esta lógica me permite mantener un flujo secuencial coherente
 * y evitar inconsistencias entre pasos.
 *
 * @author David Tomé Arnaiz
 */
@Service
public class ControlDespliegueService {

    private final ControlDespliegueRepository controlRepo;
    private final AplicacionRepository aplicacionRepo;

    /**
     * En este constructor inyecto los repositorios necesarios para gestionar
     * los controles de despliegue y su relación con las aplicaciones.
     *
     * @param controlRepo repositorio de controles de despliegue
     * @param aplicacionRepo repositorio de aplicaciones
     */
    public ControlDespliegueService(ControlDespliegueRepository controlRepo,
                                    AplicacionRepository aplicacionRepo) {
        this.controlRepo = controlRepo;
        this.aplicacionRepo = aplicacionRepo;
    }

    /**
     * Compruebo si un paso concreto se encuentra en estado OK para una aplicación.
     *
     * @param appId identificador de la aplicación
     * @param paso paso del asistente
     * @return {@code true} si el paso está en estado OK, {@code false} en caso contrario
     */
    @Transactional(readOnly = true)
    public boolean estaPasoOk(Long appId, PasoDespliegue paso) {
        return controlRepo.findByAplicacionIdAndPaso(appId, paso)
                .map(cd -> cd.getEstado() == EstadoControl.OK)
                .orElse(false);
    }

    /**
     * Decido si se puede acceder a un paso concreto del asistente.
     *
     * Permito el acceso únicamente si el paso previo está en estado OK.
     * Si el paso no tiene un paso previo, permito el acceso directamente.
     *
     * @param appId identificador de la aplicación
     * @param pasoActual paso al que se quiere acceder
     * @return {@code true} si se permite el acceso, {@code false} en caso contrario
     */
    @Transactional(readOnly = true)
    public boolean puedoAcceder(Long appId, PasoDespliegue pasoActual) {
        PasoDespliegue previo = pasoActual.getPasoPrevio();
        if (previo == null) {
            return true;
        }
        return estaPasoOk(appId, previo);
    }

    /**
     * Devuelvo el paso al que se debe redirigir al usuario cuando intenta
     * acceder a un paso no permitido.
     *
     * @param pasoActual paso solicitado
     * @return paso al que se debe redirigir
     */
    public PasoDespliegue getPasoRedireccion(PasoDespliegue pasoActual) {
        PasoDespliegue previo = pasoActual.getPasoPrevio();
        return (previo == null) ? pasoActual : previo;
    }

    /**
     * Marco un paso como completado correctamente (OK).
     *
     * Si el control no existe, lo creo y lo asocio a la aplicación indicada.
     * También registro un mensaje descriptivo y la fecha de ejecución.
     *
     * @param appId identificador de la aplicación
     * @param paso paso a marcar
     * @param mensaje mensaje informativo asociado al paso
     */
    @Transactional
    public void marcarPasoOk(Long appId, PasoDespliegue paso, String mensaje) {
        ControlDespliegue cd = controlRepo.findByAplicacionIdAndPaso(appId, paso)
                .orElseGet(() -> {
                    ControlDespliegue nuevo = new ControlDespliegue();
                    nuevo.setAplicacion(aplicacionRepo.getReferenceById(appId));
                    nuevo.setPaso(paso);
                    return nuevo;
                });

        cd.setEstado(EstadoControl.OK);
        cd.setMensaje(mensaje);
        cd.setFechaEjecucion(LocalDateTime.now());
        controlRepo.save(cd);
    }

    /**
     * Marco un paso como pendiente de completar.
     *
     * @param appId identificador de la aplicación
     * @param paso paso a marcar
     * @param mensaje mensaje informativo asociado al estado pendiente
     */
    @Transactional
    public void marcarPasoPendiente(Long appId, PasoDespliegue paso, String mensaje) {
        ControlDespliegue cd = controlRepo.findByAplicacionIdAndPaso(appId, paso)
                .orElseGet(() -> {
                    ControlDespliegue nuevo = new ControlDespliegue();
                    nuevo.setAplicacion(aplicacionRepo.getReferenceById(appId));
                    nuevo.setPaso(paso);
                    return nuevo;
                });

        cd.setEstado(EstadoControl.PENDIENTE);
        cd.setMensaje(mensaje);
        cd.setFechaEjecucion(LocalDateTime.now());
        controlRepo.save(cd);
    }

    /**
     * Marco un paso como fallido (KO).
     *
     * Este estado indica que el asistente no puede continuar hasta que
     * el error sea corregido.
     *
     * @param appId identificador de la aplicación
     * @param paso paso a marcar
     * @param mensaje mensaje descriptivo del error
     */
    @Transactional
    public void marcarPasoKo(Long appId, PasoDespliegue paso, String mensaje) {
        ControlDespliegue cd = controlRepo.findByAplicacionIdAndPaso(appId, paso)
                .orElseGet(() -> {
                    ControlDespliegue nuevo = new ControlDespliegue();
                    nuevo.setAplicacion(aplicacionRepo.getReferenceById(appId));
                    nuevo.setPaso(paso);
                    return nuevo;
                });

        cd.setEstado(EstadoControl.KO);
        cd.setMensaje(mensaje);
        cd.setFechaEjecucion(LocalDateTime.now());
        controlRepo.save(cd);
    }

    /**
     * Obtengo el control de un paso concreto para una aplicación.
     *
     * Este método se utiliza principalmente para mostrar información
     * en las vistas del asistente.
     *
     * @param appId identificador de la aplicación
     * @param paso paso del asistente
     * @return control del paso o {@code null} si no existe
     */
    @Transactional(readOnly = true)
    public ControlDespliegue getControl(Long appId, PasoDespliegue paso) {
        return controlRepo.findByAplicacionIdAndPaso(appId, paso).orElse(null);
    }
}