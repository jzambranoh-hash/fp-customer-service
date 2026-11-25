package com.nttdata.customer_service.infrastructure.config;

import com.mongodb.observability.ObservabilitySettings;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;

/**
 * Configuración de MongoDB: activa la auditoría ({@code @CreatedDate}, {@code @LastModifiedDate}).
 * Vive en la infraestructura porque es un detalle de la persistencia.
 */
@Configuration
@EnableReactiveMongoAuditing
public class MongoConfig {

    /**
     * Activa la observabilidad nativa del driver: cada comando de MongoDB se registra como un span en Zipkin.
     *
     * @param registry registro de observaciones de Micrometer
     * @return personalizador del cliente de MongoDB
     */
    @Bean
    public MongoClientSettingsBuilderCustomizer mongoTracing(ObservationRegistry registry) {
        return builder -> builder.observabilitySettings(
                ObservabilitySettings.micrometerBuilder()
                        .observationRegistry(registry)
                        .build());
    }
}
