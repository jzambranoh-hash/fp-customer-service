package com.nttdata.customer_service.infrastructure.adapter.in.web.controller;

import com.nttdata.bootcamp.customer.dto.CustomerRequest;
import com.nttdata.bootcamp.customer.dto.CustomerResponse;
import com.nttdata.bootcamp.customer.dto.CustomerUpdateRequest;
import com.nttdata.customer_service.domain.port.in.CreateCustomerUseCase;
import com.nttdata.customer_service.domain.port.in.DeleteCustomerUseCase;
import com.nttdata.customer_service.domain.port.in.FindCustomerUseCase;
import com.nttdata.customer_service.domain.port.in.UpdateCustomerUseCase;
import com.nttdata.customer_service.infrastructure.adapter.in.web.mapper.CustomerWebMapper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST. Implementa el contrato {@code customer-api.yaml}.
 *
 * <p>Solo traduce HTTP ↔ casos de uso: recibe DTOs, llama a los puertos de entrada y devuelve DTOs.
 * No registra logs: el access log y los errores se registran en un solo lugar.</p>
 *
 * <p>La ruta no lleva {@code /api}: ese prefijo lo agrega el API Gateway y lo quita con {@code StripPrefix=1}.</p>
 */
@RestController
@RequestMapping(CustomerController.API_PATH)
@RequiredArgsConstructor
public class CustomerController {

    /** Ruta base del recurso. */
    public static final String API_PATH = "/customers";

    private final CreateCustomerUseCase createCustomerUseCase;
    private final FindCustomerUseCase findCustomerUseCase;
    private final UpdateCustomerUseCase updateCustomerUseCase;
    private final DeleteCustomerUseCase deleteCustomerUseCase;
    private final CustomerWebMapper mapper;

    /**
     * @return clientes activos
     */
    @GetMapping
    public Flowable<CustomerResponse> findAll() {
        return findCustomerUseCase.findAll().map(mapper::toResponse);
    }

    /**
     * @param id identificador del cliente
     * @return el cliente (200) o 404
     */
    @GetMapping("/{id}")
    public Single<CustomerResponse> findById(@PathVariable String id) {
        return findCustomerUseCase.findById(id).map(mapper::toResponse);
    }

    /**
     * @param documentNumber número de documento
     * @return el cliente (200) o 404
     */
    @GetMapping("/document/{documentNumber}")
    public Single<CustomerResponse> findByDocumentNumber(@PathVariable String documentNumber) {
        return findCustomerUseCase.findByDocumentNumber(documentNumber).map(mapper::toResponse);
    }

    /**
     * @param request datos del cliente, validados con las restricciones del contrato
     * @return el cliente creado (201)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Single<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        return createCustomerUseCase.create(mapper.toCreateCommand(request)).map(mapper::toResponse);
    }

    /**
     * @param id      identificador del cliente
     * @param request nuevos datos
     * @return el cliente actualizado (200)
     */
    @PutMapping("/{id}")
    public Single<CustomerResponse> update(@PathVariable String id,
                                           @Valid @RequestBody CustomerUpdateRequest request) {
        return updateCustomerUseCase.update(id, mapper.toUpdateCommand(request)).map(mapper::toResponse);
    }

    /**
     * @param id identificador del cliente
     * @return sin contenido (204)
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Completable delete(@PathVariable String id) {
        return deleteCustomerUseCase.delete(id);
    }
}
