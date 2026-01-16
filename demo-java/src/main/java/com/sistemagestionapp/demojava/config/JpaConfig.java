package com.sistemagestionapp.demojava.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@Profile({"mysql","postgres"})
@EnableJpaRepositories(basePackages = "com.sistemagestionapp.demojava.repository.jpa")
@EntityScan(basePackages = "com.sistemagestionapp.demojava.model")
public class JpaConfig {
}