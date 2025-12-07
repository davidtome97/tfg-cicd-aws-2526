package com.sistemagestionapp.repository;

import com.sistemagestionapp.model.Aplicacion;
import com.sistemagestionapp.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AplicacionRepository extends JpaRepository<Aplicacion, Long> {

    List<Aplicacion> findByPropietario(Usuario propietario);
}
