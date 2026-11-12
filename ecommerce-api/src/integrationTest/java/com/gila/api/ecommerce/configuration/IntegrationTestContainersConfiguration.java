package com.gila.api.ecommerce.configuration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class IntegrationTestContainersConfiguration {

    private static final int VALKEY_PORT = 6379;

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> valkeyContainer() {
        return new GenericContainer<>(DockerImageName.parse("valkey/valkey:8-alpine")).withExposedPorts(VALKEY_PORT);
    }
}
