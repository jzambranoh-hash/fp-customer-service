package com.nttdata.customer_service.repository;

import com.nttdata.customer_service.document.CustomerDocument;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.springframework.data.repository.reactive.RxJava3CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio reactivo (RxJava 3) para la colección {@code customers}.
 *
 * <p>Las consultas solo consideran clientes activos, ya que la eliminación es lógica.</p>
 */
@Repository
public interface CustomerRepository extends RxJava3CrudRepository<CustomerDocument, String> {

    /**
     * Lista todos los clientes activos.
     *
     * @return flujo de clientes activos
     */
    Flowable<CustomerDocument> findByActiveTrue();

    /**
     * Busca un cliente activo por su ID.
     *
     * @param id identificador del cliente
     * @return el cliente, o vacío si no existe o fue eliminado
     */
    Maybe<CustomerDocument> findByIdAndActiveTrue(String id);

    /**
     * Busca un cliente activo por su número de documento.
     *
     * @param documentNumber número de documento
     * @return el cliente, o vacío si no existe o fue eliminado
     */
    Maybe<CustomerDocument> findByDocumentNumberAndActiveTrue(String documentNumber);

    /**
     * Verifica si ya existe un cliente con el número de documento indicado,
     * incluidos los eliminados, porque el documento es único en toda la colección.
     *
     * @param documentNumber número de documento
     * @return true si ya existe
     */
    Single<Boolean> existsByDocumentNumber(String documentNumber);

}
