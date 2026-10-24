package com.nttdata.customer_service.controller;

import com.nttdata.bootcamp.customer.dto.CustomerRequest;
import com.nttdata.bootcamp.customer.dto.CustomerResponse;
import com.nttdata.bootcamp.customer.dto.CustomerUpdateRequest;
import com.nttdata.customer_service.service.CustomerService;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(CustomerController.API_PATH)
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    public static final String API_PATH = "/customers";

    /**
     * Lista los clientes activos.
     *
     * @return flujo de clientes
     */
    @GetMapping
    public Flowable<CustomerResponse> findAll() {
        log.debug("GET /customers");
        return customerService.findAll();
    }

    /**
     * Obtiene un cliente por su ID.
     *
     * @param id identificador del cliente
     * @return el cliente (200) o 404 si no existe
     */
    @GetMapping("/{id}")
    public Single<CustomerResponse> findById(@PathVariable String id) {
        log.debug("GET /customers/{}", id);
        return customerService.findById(id);
    }

    /**
     * Obtiene un cliente por su número de documento.
     *
     * @param documentNumber número de documento
     * @return el cliente (200) o 404 si no existe
     */
    @GetMapping("/document/{documentNumber}")
    public Single<CustomerResponse> findByDocumentNumber(@PathVariable String documentNumber) {
        log.debug("GET /customers/document/{}", documentNumber);
        return customerService.findByDocumentNumber(documentNumber);
    }

    /**
     * Registra un cliente.
     *
     * @param request datos del cliente, validados con las restricciones del contrato
     * @return el cliente creado (201)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Single<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        log.debug("POST /customers tipo={}", request.getCustomerType());
        return customerService.create(request);
    }

    /**
     * Actualiza los datos editables de un cliente.
     *
     * @param id      identificador del cliente
     * @param request nuevos datos
     * @return el cliente actualizado (200)
     */
    @PutMapping("/{id}")
    public Single<CustomerResponse> update(@PathVariable String id, @Valid @RequestBody CustomerUpdateRequest request) {
        log.debug("PUT /customers/{}", id);
        return customerService.update(id, request);
    }

    /**
     * Elimina un cliente (borrado lógico).
     *
     * @param id identificador del cliente
     * @return sin contenido (204)
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Completable delete(@PathVariable String id) {
        log.debug("DELETE /customers/{}", id);
        return customerService.delete(id);
    }

}
