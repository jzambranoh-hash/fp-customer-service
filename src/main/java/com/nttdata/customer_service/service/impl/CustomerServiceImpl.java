package com.nttdata.customer_service.service.impl;

import com.nttdata.bootcamp.customer.dto.*;
import com.nttdata.customer_service.document.CustomerDocument;
import com.nttdata.customer_service.exception.BusinessRuleException;
import com.nttdata.customer_service.exception.CustomerNotFoundException;
import com.nttdata.customer_service.exception.DuplicateDocumentException;
import com.nttdata.customer_service.mapper.CustomerMapper;
import com.nttdata.customer_service.repository.CustomerRepository;
import com.nttdata.customer_service.service.CustomerService;
import com.nttdata.customer_service.service.validation.CustomerValidator;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Implementación de {@link CustomerService} con las reglas de negocio de clientes.
 *
 * <p>Reglas:</p>
 * <ul>
 *   <li>El número de documento es único.</li>
 *   <li>PERSONAL: requiere nombres y apellidos, no puede usar RUC ni el perfil PYME.</li>
 *   <li>BUSINESS: requiere razón social, debe usar RUC y no puede tener el perfil VIP.</li>
 *   <li>DNI: 8 dígitos. RUC: 11 dígitos.</li>
 *   <li>La eliminación es lógica.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final CustomerValidator customerValidator;

    @Override
    public Single<CustomerResponse> create(CustomerRequest request) {
        return Single.fromCallable(() -> customerMapper.toDocument(request))
                .map(customerValidator::validate)
                .flatMap(document -> customerRepository
                        .existsByDocumentNumber(document.getDocumentNumber())
                        .flatMap(exists -> Boolean.TRUE.equals(exists)
                                ? Single.<CustomerDocument>error(
                                new DuplicateDocumentException(document.getDocumentNumber()))
                                : customerRepository.save(document)))
                // Si dos peticiones llegan a la vez, el índice único de MongoDB lanza DuplicateKeyException
                .onErrorResumeNext(error -> Single.error(
                        translateDuplicateKey(error, request.getDocumentNumber())))
                .map(customerMapper::toResponse)
                .doOnSuccess(customer -> log.info("Cliente creado: id={}, tipo={}",
                        customer.getId(), customer.getCustomerType()))
                .doOnError(error -> log.warn("No se pudo crear el cliente: {}", error.getMessage()));
    }

    @Override
    public Flowable<CustomerResponse> findAll() {
        return customerRepository.findByActiveTrue()
                .map(customerMapper::toResponse)
                .doOnComplete(() -> log.debug("Listado de clientes completado"));
    }

    @Override
    public Single<CustomerResponse> findById(String id) {
        return findActiveById(id).map(customerMapper::toResponse)
                .doOnSuccess(customer -> log.debug("Cliente encontrado: id={}", id));
    }

    @Override
    public Single<CustomerResponse> findByDocumentNumber(String documentNumber) {
        return customerRepository.findByDocumentNumberAndActiveTrue(documentNumber)
                .switchIfEmpty(Single.<CustomerDocument>error(
                        () -> new CustomerNotFoundException("documentNumber", documentNumber)))
                .map(customerMapper::toResponse);
    }

    @Override
    public Single<CustomerResponse> update(String id, CustomerUpdateRequest request) {
        return findActiveById(id)
                .map(document -> {
                    customerMapper.updateDocument(document, request);
                    return customerValidator.validate(document);
                })
                .flatMap(customerRepository::save)
                .map(customerMapper::toResponse)
                .doOnSuccess(customer -> log.info("Cliente actualizado: id={}", id))
                .doOnError(error -> log.warn("No se pudo actualizar el cliente {}: {}", id, error.getMessage()));
    }

    @Override
    public Completable delete(String id) {
        return findActiveById(id)
                .flatMapCompletable(document -> {
                    document.setActive(Boolean.FALSE);
                    return customerRepository.save(document).ignoreElement();
                })
                .doOnComplete(() -> log.info("Cliente eliminado (lógico): id={}", id))
                .doOnError(error -> log.warn("No se pudo eliminar el cliente {}: {}", id, error.getMessage()));
    }

    /**
     * Busca un cliente activo o falla con {@link CustomerNotFoundException}.
     */
    private Single<CustomerDocument> findActiveById(String id) {
        return customerRepository.findByIdAndActiveTrue(id)
                .switchIfEmpty(Single.<CustomerDocument>error(
                        () -> new CustomerNotFoundException("id", id)));
    }

    private Throwable translateDuplicateKey(Throwable error, String documentNumber) {
        return error instanceof DuplicateKeyException
                ? new DuplicateDocumentException(documentNumber)
                : error;
    }

}
