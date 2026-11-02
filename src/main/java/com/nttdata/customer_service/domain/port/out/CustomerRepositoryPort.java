package com.nttdata.customer_service.domain.port.out;

import com.nttdata.customer_service.domain.model.Customer;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

/**
 * Puerto de salida: lo que el dominio necesita de la persistencia, sin saber que es MongoDB.
 *
 * <p>Lo implementa {@code CustomerPersistenceAdapter}. Para cambiar de base de datos basta con
 * escribir otro adaptador; el dominio y los casos de uso no cambian.</p>
 */
public interface CustomerRepositoryPort {

    /**
     * @return clientes activos
     */
    Flowable<Customer> findAllActive();

    /**
     * @param id identificador
     * @return el cliente activo, o vacío
     */
    Maybe<Customer> findActiveById(String id);

    /**
     * @param documentNumber número de documento
     * @return el cliente activo, o vacío
     */
    Maybe<Customer> findActiveByDocumentNumber(String documentNumber);

    /**
     * Incluye clientes inactivos: el documento es único en todo el sistema.
     *
     * @param documentNumber número de documento
     * @return true si ya existe
     */
    Single<Boolean> existsByDocumentNumber(String documentNumber);

    /**
     * Crea o actualiza el cliente.
     *
     * @param customer cliente a guardar
     * @return el cliente guardado (con id, fechas y versión);
     *         {@code DuplicateDocumentException} si otro registro ganó la carrera por el mismo documento
     */
    Single<Customer> save(Customer customer);
}
