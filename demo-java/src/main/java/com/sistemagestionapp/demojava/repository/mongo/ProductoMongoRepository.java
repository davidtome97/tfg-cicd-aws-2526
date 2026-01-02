package com.sistemagestionapp.demojava.repository.mongo;

import com.sistemagestionapp.demojava.model.mongo.ProductoMongo;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoMongoRepository extends MongoRepository<ProductoMongo, String> {

    List<ProductoMongo> findByUsuarioId(String usuarioId);

    Optional<ProductoMongo> findByIdAndUsuarioId(String id, String usuarioId);
}