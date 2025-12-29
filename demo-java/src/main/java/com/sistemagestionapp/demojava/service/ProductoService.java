package com.sistemagestionapp.demojava.service;

import com.sistemagestionapp.demojava.model.Producto;
import com.sistemagestionapp.demojava.model.Usuario;
import com.sistemagestionapp.demojava.model.mongo.ProductoMongo;
import com.sistemagestionapp.demojava.repository.jpa.ProductoRepository;
import com.sistemagestionapp.demojava.repository.jpa.UsuarioRepository;
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
    private final UsuarioRepository usuarioRepository;              // para SQL
    private final String dbEngine;

    public ProductoService(
            ObjectProvider<ProductoRepository> productoRepository,
            ObjectProvider<ProductoMongoRepository> productoMongoRepository,
            ObjectProvider<UsuarioRepository> usuarioRepository,
            @Value("${DB_ENGINE:mysql}") String dbEngine
    ) {
        this.productoRepository = productoRepository.getIfAvailable();
        this.productoMongoRepository = productoMongoRepository.getIfAvailable();
        this.usuarioRepository = usuarioRepository.getIfAvailable();
        this.dbEngine = (dbEngine == null || dbEngine.isBlank()) ? "mysql" : dbEngine.toLowerCase();
    }

    private boolean isMongo() {
        return "mongo".equalsIgnoreCase(dbEngine);
    }

    private Usuario usuarioSqlObligatorio(String correo) {
        if (usuarioRepository == null) {
            throw new IllegalStateException("UsuarioRepository no disponible (perfil SQL mal configurado)");
        }
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + correo));
    }

    // =========================================================
    // LISTAR SOLO PRODUCTOS DEL USUARIO
    // =========================================================
    @Transactional(readOnly = true)
    public List<?> listarDelUsuario(String correo) {
        if (correo == null || correo.isBlank()) throw new IllegalArgumentException("correo obligatorio");

        if (isMongo()) {
            if (productoMongoRepository == null) {
                throw new IllegalStateException("ProductoMongoRepository no disponible (perfil mongo mal configurado)");
            }
            // ✅ en Mongo usamos correo como usuarioId
            return productoMongoRepository.findByUsuarioId(correo);
        }

        if (productoRepository == null) {
            throw new IllegalStateException("ProductoRepository no disponible (perfil sql mal configurado)");
        }

        Usuario propietario = usuarioSqlObligatorio(correo);
        return productoRepository.findByPropietario(propietario);
    }

    // =========================================================
    // BUSCAR POR ID SOLO SI ES DEL USUARIO
    // =========================================================
    @Transactional(readOnly = true)
    public Object buscarDelUsuario(String id, String correo) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id obligatorio");
        if (correo == null || correo.isBlank()) throw new IllegalArgumentException("correo obligatorio");

        if (isMongo()) {
            if (productoMongoRepository == null) {
                throw new IllegalStateException("ProductoMongoRepository no disponible (perfil mongo mal configurado)");
            }
            return productoMongoRepository.findByIdAndUsuarioId(id, correo)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado o no pertenece al usuario: " + id));
        }

        if (productoRepository == null) {
            throw new IllegalStateException("ProductoRepository no disponible (perfil sql mal configurado)");
        }

        Usuario propietario = usuarioSqlObligatorio(correo);
        Long longId = parseLongId(id);

        return productoRepository.findByIdAndPropietario(longId, propietario)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado o no pertenece al usuario: " + id));
    }

    // =========================================================
    // GUARDAR/ACTUALIZAR (ASIGNANDO USUARIO)
    // =========================================================
    @Transactional
    public void guardar(String id, String nombre, String descripcion, double precio, String correo) {
        if (correo == null || correo.isBlank()) throw new IllegalArgumentException("correo obligatorio");

        if (isMongo()) {
            if (productoMongoRepository == null) {
                throw new IllegalStateException("ProductoMongoRepository no disponible (perfil mongo mal configurado)");
            }

            final ProductoMongo p;

            if (id == null || id.isBlank()) {
                p = new ProductoMongo();
                p.setUsuarioId(correo); // ✅ dueño
            } else {
                p = productoMongoRepository.findByIdAndUsuarioId(id, correo)
                        .orElseThrow(() -> new IllegalArgumentException("No puedes editar un producto que no es tuyo: " + id));
            }

            p.setNombre(nombre);
            p.setDescripcion(descripcion);
            p.setPrecio(precio);

            productoMongoRepository.save(p);
            return;
        }

        if (productoRepository == null) {
            throw new IllegalStateException("ProductoRepository no disponible (perfil sql mal configurado)");
        }

        Usuario propietario = usuarioSqlObligatorio(correo);

        final Producto p;
        if (id == null || id.isBlank()) {
            p = new Producto();
            p.setPropietario(propietario); // ✅ dueño
        } else {
            Long longId = parseLongId(id);
            p = productoRepository.findByIdAndPropietario(longId, propietario)
                    .orElseThrow(() -> new IllegalArgumentException("No puedes editar un producto que no es tuyo: " + id));
        }

        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(precio);

        productoRepository.save(p);
    }

    // =========================================================
    // BORRAR SOLO SI ES DEL USUARIO
    // =========================================================
    @Transactional
    public void borrar(String id, String correo) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id obligatorio");
        if (correo == null || correo.isBlank()) throw new IllegalArgumentException("correo obligatorio");

        if (isMongo()) {
            if (productoMongoRepository == null) {
                throw new IllegalStateException("ProductoMongoRepository no disponible (perfil mongo mal configurado)");
            }

            ProductoMongo p = productoMongoRepository.findByIdAndUsuarioId(id, correo)
                    .orElseThrow(() -> new IllegalArgumentException("No puedes borrar un producto que no es tuyo: " + id));

            productoMongoRepository.delete(p);
            return;
        }

        if (productoRepository == null) {
            throw new IllegalStateException("ProductoRepository no disponible (perfil sql mal configurado)");
        }

        Usuario propietario = usuarioSqlObligatorio(correo);
        Long longId = parseLongId(id);

        Producto p = productoRepository.findByIdAndPropietario(longId, propietario)
                .orElseThrow(() -> new IllegalArgumentException("No puedes borrar un producto que no es tuyo: " + id));

        productoRepository.delete(p);
    }

    private Long parseLongId(String id) {
        try {
            return Long.valueOf(id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID inválido (se esperaba numérico para SQL): " + id);
        }
    }
}