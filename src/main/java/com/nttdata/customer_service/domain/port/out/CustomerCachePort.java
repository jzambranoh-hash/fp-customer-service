package com.nttdata.customer_service.domain.port.out;

import com.nttdata.customer_service.domain.model.Customer;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;

/**
 * Caché de clientes (patrón cache-aside). Si la caché falla, las operaciones no propagan el error:
 * una lectura fallida se comporta como "no está en caché".
 */
public interface CustomerCachePort {

    /** Busca el cliente en la caché; vacío si no está o si la caché no responde. */
    Maybe<Customer> get(String id);

    /** Guarda el cliente en la caché con el TTL configurado. */
    Completable put(Customer customer);

    /** Borra el cliente de la caché (después de actualizarlo o desactivarlo). */
    Completable evict(String id);
}