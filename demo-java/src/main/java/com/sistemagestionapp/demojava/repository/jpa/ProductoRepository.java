package com.sistemagestionapp.demojava.repository;

import com.sistemagestionapp.demojava.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Esta interfaz la utilizo como repositorio para acceder a los datos de productos
 * en la base de datos.
 * Al extender de {@link JpaRepository}, heredo automáticamente todos los métodos
 * necesarios para realizar operaciones CRUD sobre la entidad {@link Producto},
 * sin necesidad de implementarlos manualmente.
 *
 * @author David Tomé Arnáiz
 */
public interface ProductoRepository extends JpaRepository<Producto, Long> {
}