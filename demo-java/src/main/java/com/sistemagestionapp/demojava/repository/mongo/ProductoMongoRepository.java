package com.sistemagestionapp.demojava.repository.mongo;

import com.sistemagestionapp.demojava.model.mongo.ProductoMongo;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductoMongoRepository extends MongoRepository<ProductoMongo, String> {
}