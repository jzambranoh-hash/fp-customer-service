package com.nttdata.customer_service.domain.port.in;

import io.reactivex.rxjava3.core.Completable;

/**
 * Puerto de entrada: eliminar un cliente (borrado lógico).
 */
public interface DeleteCustomerUseCase {

    /**
     * @param id identificador del cliente
     * @return señal de finalización; {@code CustomerNotFoundException} si no existe
     */
    Completable delete(String id);
}
