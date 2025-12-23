package com.sistemagestionapp.demojava.service;

import com.sistemagestionapp.demojava.model.Producto;
import com.sistemagestionapp.demojava.model.mongo.ProductoMongo;
import com.sistemagestionapp.demojava.repository.jpa.ProductoRepository;
import com.sistemagestionapp.demojava.repository.mongo.ProductoMongoRepository;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;            // null si mongo
    private final ProductoMongoRepository productoMongoRepository;  // null si sql
    private final String dbEngine;

    public ProductoService(
            ObjectProvider<ProductoRepository> productoRepository,
            ObjectProvider<ProductoMongoRepository> productoMongoRepository,
            @Value("${DB_ENGINE:mysql}") String dbEngine
    ) {
        this.productoRepository = productoRepository.getIfAvailable();
        this.productoMongoRepository = productoMongoRepository.getIfAvailable();
        this.dbEngine = dbEngine == null ? "mysql" : dbEngine.toLowerCase();
    }

    private boolean isMongo() {
        return "mongo".equalsIgnoreCase(dbEngine);
    }

    @Transactional(readOnly = true)
    public List<?> listarTodos() {
        if (isMongo()) {
            if (productoMongoRepository == null) throw new IllegalStateException("ProductoMongoRepository no disponible (perfil mongo mal configurado)");
            return productoMongoRepository.findAll();
        }
        if (productoRepository == null) throw new IllegalStateException("ProductoRepository no disponible (perfil sql mal configurado)");
        return productoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Object buscarPorId(String id) {
        if (isMongo()) {
            if (productoMongoRepository == null) throw new IllegalStateException("ProductoMongoRepository no disponible (perfil mongo mal configurado)");
            return productoMongoRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
        }
        if (productoRepository == null) throw new IllegalStateException("ProductoRepository no disponible (perfil sql mal configurado)");
        return productoRepository.findById(Long.valueOf(id))
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
    }

    @Transactional
    public void guardar(String id, String nombre, String descripcion, double precio) {
        if (isMongo()) {
            if (productoMongoRepository == null) throw new IllegalStateException("ProductoMongoRepository no disponible (perfil mongo mal configurado)");

            ProductoMongo p = (id == null || id.isBlank())
                    ? new ProductoMongo()
                    : productoMongoRepository.findById(id).orElse(new ProductoMongo());

            p.setNombre(nombre);
            p.setDescripcion(descripcion);
            p.setPrecio(precio);

            productoMongoRepository.save(p);
            return;
        }

        if (productoRepository == null) throw new IllegalStateException("ProductoRepository no disponible (perfil sql mal configurado)");

        Producto p = (id == null || id.isBlank())
                ? new Producto()
                : productoRepository.findById(Long.valueOf(id)).orElse(new Producto());

        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(precio);

        productoRepository.save(p);
    }

    @Transactional
    public void borrarPorId(String id) {
        if (isMongo()) {
            if (productoMongoRepository == null) throw new IllegalStateException("ProductoMongoRepository no disponible (perfil mongo mal configurado)");
            productoMongoRepository.deleteById(id);
            return;
        }
        if (productoRepository == null) throw new IllegalStateException("ProductoRepository no disponible (perfil sql mal configurado)");
        productoRepository.deleteById(Long.valueOf(id));
    }
}