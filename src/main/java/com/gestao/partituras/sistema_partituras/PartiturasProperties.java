package com.gestao.partituras.sistema_partituras;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configurações do sistema, lidas de application.properties (prefixo "partituras").
 */
@ConfigurationProperties(prefix = "partituras")
public record PartiturasProperties(String folderId, String credentialsPath, Duration cacheTtl) {
}
