package com.sistemagestionapp.demojava.repository.jpa;

import com.sistemagestionapp.demojava.model.Producto;
import com.sistemagestionapp.demojava.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByPropietario(Usuario propietario);

    Optional<Producto> findByIdAndPropietario(Long id, Usuario propietario);
}