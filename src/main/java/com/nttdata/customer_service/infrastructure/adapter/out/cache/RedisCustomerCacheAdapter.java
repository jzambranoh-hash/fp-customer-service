package com.nttdata.customer_service.infrastructure.adapter.out.cache;

import com.nttdata.customer_service.domain.model.Customer;
import com.nttdata.customer_service.domain.port.out.CustomerCachePort;
import com.nttdata.customer_service.infrastructure.adapter.out.cache.context.TraceContext;
import com.nttdata.customer_service.infrastructure.adapter.out.persistence.document.CustomerDocument;
import com.nttdata.customer_service.infrastructure.adapter.out.persistence.mapper.CustomerPersistenceMapper;
import io.micrometer.observation.ObservationRegistry;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Adaptador de salida: caché de clientes en Redis.
 *
 * <p>Clave {@code customer:{id}}, valor en JSON, con TTL. Los errores de Redis se registran como WARN y no
 * se propagan: la caché acelera las lecturas, pero el servicio funciona igual sin ella.</p>
 */
@Slf4j
@Component
public class RedisCustomerCacheAdapter implements CustomerCachePort {

    private static final String KEY_PREFIX = "customer:";

    private final ReactiveStringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final CustomerPersistenceMapper mapper;
    private final Duration ttl;

    private final ObservationRegistry observationRegistry;

    /**
     * Crea el adaptador.
     *
     * @param redis        cliente reactivo de Redis
     * @param objectMapper serializador JSON de Spring
     * @param mapper       conversión dominio ↔ documento (la misma de la persistencia)
     * @param ttl          tiempo de vida de cada entrada, por ejemplo {@code 10m}
     */
    public RedisCustomerCacheAdapter(ReactiveStringRedisTemplate redis, ObjectMapper objectMapper,
                                     CustomerPersistenceMapper mapper,
                                     @Value("${bank.cache.customer.ttl:10m}") Duration ttl,
                                     ObservationRegistry observationRegistry) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.mapper = mapper;
        this.ttl = ttl;
        this.observationRegistry = observationRegistry;
    }

    @Override
    public Maybe<Customer> get(String id) {
        return Maybe.fromPublisher(TraceContext.withCurrentTrace(
                        redis.opsForValue().get(KEY_PREFIX + id), observationRegistry))
                .map(json -> mapper.toDomain(objectMapper.readValue(json, CustomerDocument.class)))
                .onErrorResumeNext(error -> {
                    log.warn("Caché no disponible al leer; se usa la base de datos", error);
                    return Maybe.empty();
                });
    }

    @Override
    public Completable put(Customer customer) {
        return Completable.defer(() -> Completable.fromPublisher(TraceContext.withCurrentTrace(
                        redis.opsForValue().set(KEY_PREFIX + customer.getId(),
                                objectMapper.writeValueAsString(mapper.toDocument(customer)), ttl),
                        observationRegistry)))
                .onErrorComplete(error -> {
                    log.warn("No se pudo guardar en caché", error);
                    return true;
                });
    }

    @Override
    public Completable evict(String id) {
        return Completable.fromPublisher(TraceContext.withCurrentTrace(
                        redis.delete(KEY_PREFIX + id), observationRegistry))
                .onErrorComplete(error -> {
                    log.warn("No se pudo borrar de la caché", error);
                    return true;
                });
    }
}
