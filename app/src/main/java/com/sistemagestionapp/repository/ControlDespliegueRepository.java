package com.sistemagestionapp.repository;

import com.sistemagestionapp.model.ControlDespliegue;
import com.sistemagestionapp.model.Aplicacion;
import com.sistemagestionapp.model.PasoDespliegue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ControlDespliegueRepository extends JpaRepository<ControlDespliegue, Long> {

    List<ControlDespliegue> findByAplicacion(Aplicacion aplicacion);

    Optional<ControlDespliegue> findByAplicacionAndPaso(Aplicacion aplicacion, PasoDespliegue paso);
}
