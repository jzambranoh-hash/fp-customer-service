package com.nttdata.customer_service.domain.port.in;

import com.nttdata.customer_service.domain.model.Customer;
import io.reactivex.rxjava3.core.Single;

/**
 * Puerto de entrada: registrar un cliente.
 */
public interface CreateCustomerUseCase {

    /**
     * @param command datos del cliente
     * @return el cliente creado; {@code BusinessRuleException} si incumple una regla,
     *         {@code DuplicateDocumentException} si el documento ya existe
     */
    Single<Customer> create(CreateCustomerCommand command);
}
