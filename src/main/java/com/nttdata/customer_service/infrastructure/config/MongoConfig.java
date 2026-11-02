package com.nttdata.customer_service.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;

/**
 * Configuración de MongoDB: activa la auditoría ({@code @CreatedDate}, {@code @LastModifiedDate}).
 * Vive en la infraestructura porque es un detalle de la persistencia.
 */
@Configuration
@EnableReactiveMongoAuditing
public class MongoConfig {
}
