package com.nttdata.customer_service.domain.port.in;

import com.nttdata.customer_service.domain.model.Customer;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/**
 * Puerto de entrada: consultar clientes activos.
 */
public interface FindCustomerUseCase {

    /**
     * Lista los clientes activos.
     *
     * @return clientes activos
     */
    Flowable<Customer> findAll();

    /**
     * Busca un cliente activo por su identificador.
     *
     * @param id identificador del cliente
     * @return el cliente; {@code CustomerNotFoundException} si no existe o está inactivo
     */
    Single<Customer> findById(String id);

    /**
     * Busca un cliente activo por su número de documento.
     *
     * @param documentNumber número de documento
     * @return el cliente; {@code CustomerNotFoundException} si no existe o está inactivo
     */
    Single<Customer> findByDocumentNumber(String documentNumber);
}
