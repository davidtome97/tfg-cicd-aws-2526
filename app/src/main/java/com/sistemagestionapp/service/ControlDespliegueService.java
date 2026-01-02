package com.sistemagestionapp.service;

import com.sistemagestionapp.model.*;
import com.sistemagestionapp.repository.AplicacionRepository;
import com.sistemagestionapp.repository.ControlDespliegueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ControlDespliegueService {

    private final ControlDespliegueRepository controlRepo;
    private final AplicacionRepository aplicacionRepo;

    public ControlDespliegueService(ControlDespliegueRepository controlRepo,
                                    AplicacionRepository aplicacionRepo) {
        this.controlRepo = controlRepo;
        this.aplicacionRepo = aplicacionRepo;
    }

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
}
