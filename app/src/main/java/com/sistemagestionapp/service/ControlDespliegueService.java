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
 * En este servicio gestiono el estado de cada paso del asistente de despliegue.
 * Me encargo de comprobar si un paso está completado, si se puede acceder a un paso
 * concreto y de registrar los resultados (OK, KO o PENDIENTE) de cada fase.
 */
@Service
public class ControlDespliegueService {

    // Repositorio que utilizo para leer y guardar los estados de cada paso
    private final ControlDespliegueRepository controlRepo;

    // Repositorio que utilizo para asociar los pasos a una aplicación concreta
    private final AplicacionRepository aplicacionRepo;

    public ControlDespliegueService(ControlDespliegueRepository controlRepo,
                                    AplicacionRepository aplicacionRepo) {
        this.controlRepo = controlRepo;
        this.aplicacionRepo = aplicacionRepo;
    }

    /**
     * Compruebo si un paso concreto está en estado OK para una aplicación.
     * Este método lo utilizo para validar si un paso ya se ha completado correctamente.
     */
    @Transactional(readOnly = true)
    public boolean estaPasoOk(Long appId, PasoDespliegue paso) {
        return controlRepo.findByAplicacionIdAndPaso(appId, paso)
                .map(cd -> cd.getEstado() == EstadoControl.OK)
                .orElse(false);
    }

    /**
     * Decido si se puede acceder a un paso del asistente.
     * La regla es simple: el paso anterior debe estar en estado OK.
     * Si el paso no tiene previo (primer paso), permito el acceso directamente.
     */
    @Transactional(readOnly = true)
    public boolean puedoAcceder(Long appId, PasoDespliegue pasoActual) {
        PasoDespliegue previo = pasoActual.getPasoPrevio();
        if (previo == null) {
            // Es el primer paso del asistente
            return true;
        }
        return estaPasoOk(appId, previo);
    }

    /**
     * Devuelvo el paso al que se debe redirigir al usuario si intenta acceder
     * a un paso que todavía no está permitido.
     * Normalmente será el paso previo.
     */
    public PasoDespliegue getPasoRedireccion(PasoDespliegue pasoActual) {
        PasoDespliegue previo = pasoActual.getPasoPrevio();
        return (previo == null) ? pasoActual : previo;
    }

    /**
     * Marco un paso como OK.
     * Si no existe aún un registro para ese paso y aplicación, lo creo.
     * También guardo un mensaje descriptivo y la fecha de ejecución.
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
     * Marco un paso como PENDIENTE.
     * Utilizo este estado cuando el paso todavía no se ha completado
     * o queda a la espera de una acción del usuario.
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
     * Marco un paso como KO.
     * Este estado indica que el paso ha fallado y no se puede continuar
     * hasta que el problema se solucione.
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
     * Obtengo el estado de un paso concreto para una aplicación.
     * Este método lo utilizo principalmente para mostrar información
     * en las vistas del asistente.
     */
    @Transactional(readOnly = true)
    public ControlDespliegue getControl(Long appId, PasoDespliegue paso) {
        return controlRepo.findByAplicacionIdAndPaso(appId, paso).orElse(null);
    }
}