package com.sistemagestionapp.repository;

import com.sistemagestionapp.model.ControlDespliegue;
import com.sistemagestionapp.model.EstadoControl;
import com.sistemagestionapp.model.PasoDespliegue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ControlDespliegueRepository extends JpaRepository<ControlDespliegue, Long> {

    // Para pintar el resumen del asistente / pasos de una app
    List<ControlDespliegue> findByAplicacionIdOrderByPasoAsc(Long aplicacionId);

    // Para actualizar un paso concreto (idempotente)
    Optional<ControlDespliegue> findByAplicacionIdAndPaso(Long aplicacionId, PasoDespliegue paso);

    // Para el contador "X / 7" en el listado
    long countByAplicacionIdAndEstado(Long aplicacionId, EstadoControl estado);

    // (Opcional) si quieres detectar el último update
    Optional<ControlDespliegue> findTopByAplicacionIdOrderByFechaEjecucionDesc(Long aplicacionId);
}
