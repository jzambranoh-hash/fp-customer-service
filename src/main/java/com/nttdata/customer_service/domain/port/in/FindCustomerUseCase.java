package com.nttdata.customer_service.domain.port.in;

import com.nttdata.customer_service.domain.model.Customer;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/**
 * Puerto de entrada: consultar clientes activos.
 */
public interface FindCustomerUseCase {

    /**
     * @return clientes activos
     */
    Flowable<Customer> findAll();

    /**
     * @param id identificador del cliente
     * @return el cliente; {@code CustomerNotFoundException} si no existe o está inactivo
     */
    Single<Customer> findById(String id);

    /**
     * @param documentNumber número de documento
     * @return el cliente; {@code CustomerNotFoundException} si no existe o está inactivo
     */
    Single<Customer> findByDocumentNumber(String documentNumber);
}
