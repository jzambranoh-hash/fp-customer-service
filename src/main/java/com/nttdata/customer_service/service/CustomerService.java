package com.nttdata.customer_service.service;

import com.nttdata.bootcamp.customer.dto.CustomerRequest;
import com.nttdata.bootcamp.customer.dto.CustomerResponse;
import com.nttdata.bootcamp.customer.dto.CustomerUpdateRequest;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

public interface CustomerService {

    /**
     * Registra un nuevo cliente validando las reglas de negocio.
     *
     * @param request datos del cliente
     * @return el cliente creado
     */
    Single<CustomerResponse> create(CustomerRequest request);

    /**
     * Lista los clientes activos.
     *
     * @return flujo de clientes activos
     */
    Flowable<CustomerResponse> findAll();

    /**
     * Obtiene un cliente activo por su ID.
     *
     * @param id identificador del cliente
     * @return el cliente; error {@code CustomerNotFoundException} si no existe
     */
    Single<CustomerResponse> findById(String id);

    /**
     * Obtiene un cliente activo por su número de documento.
     *
     * @param documentNumber número de documento
     * @return el cliente; error {@code CustomerNotFoundException} si no existe
     */
    Single<CustomerResponse> findByDocumentNumber(String documentNumber);

    /**
     * Actualiza los datos editables de un cliente.
     * El tipo de cliente y el documento no se pueden cambiar.
     *
     * @param id      identificador del cliente
     * @param request nuevos datos
     * @return el cliente actualizado
     */
    Single<CustomerResponse> update(String id, CustomerUpdateRequest request);

    /**
     * Elimina un cliente de forma lógica (active = false).
     *
     * @param id identificador del cliente
     * @return señal de finalización
     */
    Completable delete(String id);

}
