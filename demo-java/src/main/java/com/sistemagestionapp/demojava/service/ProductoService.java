package com.sistemagestionapp.demojava.service;

import com.sistemagestionapp.demojava.model.Producto;
import com.sistemagestionapp.demojava.model.mongo.ProductoMongo;
import com.sistemagestionapp.demojava.repository.ProductoRepository;
import com.sistemagestionapp.demojava.repository.mongo.ProductoMongoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoMongoRepository productoMongoRepository;
    private final String dbEngine;

    public ProductoService(ProductoRepository productoRepository,
                           ProductoMongoRepository productoMongoRepository,
                           @Value("${app.db.engine:h2}") String dbEngine) {
        this.productoRepository = productoRepository;
        this.productoMongoRepository = productoMongoRepository;
        this.dbEngine = dbEngine == null ? "h2" : dbEngine.toLowerCase();
    }

    private boolean isMongo() {
        return "mongo".equalsIgnoreCase(dbEngine);
    }

    // LISTAR
    @Transactional(readOnly = true)
    public List<?> listarTodos() {
        return isMongo()
                ? productoMongoRepository.findAll()
                : productoRepository.findAll();
    }

    // BUSCAR POR ID
    @Transactional(readOnly = true)
    public Object buscarPorId(String id) {
        if (isMongo()) {
            return productoMongoRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
        } else {
            Long longId = Long.valueOf(id);
            return productoRepository.findById(longId)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
        }
    }

    // GUARDAR O ACTUALIZAR
    @Transactional
    public void guardar(String id, String nombre, String descripcion, double precio) {

        if (isMongo()) {
            ProductoMongo p = (id == null || id.isBlank())
                    ? new ProductoMongo()                                         // crear
                    : productoMongoRepository.findById(id).orElse(new ProductoMongo()); // editar

            p.setNombre(nombre);
            p.setDescripcion(descripcion);
            p.setPrecio(precio);

            productoMongoRepository.save(p);

        } else {
            Producto p = (id == null || id.isBlank())
                    ? new Producto()
                    : productoRepository.findById(Long.valueOf(id)).orElse(new Producto());

            p.setNombre(nombre);
            p.setDescripcion(descripcion);
            p.setPrecio(precio);

            productoRepository.save(p);
        }
    }

    // ELIMINAR
    @Transactional
    public void borrarPorId(String id) {
        if (isMongo()) {
            productoMongoRepository.deleteById(id);
        } else {
            productoRepository.deleteById(Long.valueOf(id));
        }
    }
}