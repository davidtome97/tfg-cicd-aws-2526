package com.sistemagestionapp.demojava.repository.mongo;

import com.sistemagestionapp.demojava.model.mongo.UsuarioMongo;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UsuarioMongoRepository extends MongoRepository<UsuarioMongo, String> {

    boolean existsByCorreo(String correo);

    Optional<UsuarioMongo> findByCorreo(String correo);
}