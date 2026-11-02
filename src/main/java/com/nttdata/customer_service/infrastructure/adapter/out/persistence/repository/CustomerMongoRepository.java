package com.nttdata.customer_service.infrastructure.adapter.out.persistence.repository;

import com.nttdata.customer_service.infrastructure.adapter.out.persistence.adapter.CustomerPersistenceAdapter;
import com.nttdata.customer_service.infrastructure.adapter.out.persistence.document.CustomerDocument;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.springframework.data.repository.reactive.RxJava3CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de Spring Data (RxJava 3) para la colección {@code customers}.
 * Solo lo usa {@link CustomerPersistenceAdapter}; el dominio no lo conoce.
 */
@Repository
public interface CustomerMongoRepository extends RxJava3CrudRepository<CustomerDocument, String> {

    /**
     * @return clientes activos
     */
    Flowable<CustomerDocument> findByActiveTrue();

    /**
     * @param id identificador
     * @return el cliente activo, o vacío
     */
    Maybe<CustomerDocument> findByIdAndActiveTrue(String id);

    /**
     * @param documentNumber número de documento
     * @return el cliente activo, o vacío
     */
    Maybe<CustomerDocument> findByDocumentNumberAndActiveTrue(String documentNumber);

    /**
     * Incluye inactivos: el documento es único en toda la colección.
     *
     * @param documentNumber número de documento
     * @return true si ya existe
     */
    Single<Boolean> existsByDocumentNumber(String documentNumber);
}
