package com.nttdata.customer_service.domain.port.in;

import com.nttdata.customer_service.domain.model.Customer;
import io.reactivex.rxjava3.core.Single;

/**
 * Puerto de entrada: actualizar los datos editables de un cliente.
 */
public interface UpdateCustomerUseCase {

    /**
     * @param id      identificador del cliente
     * @param command nuevos datos
     * @return el cliente actualizado
     */
    Single<Customer> update(String id, UpdateCustomerCommand command);
}
