package com.nttdata.customer_service.application.service;

import com.nttdata.customer_service.application.mapper.CustomerCommandMapper;
import com.nttdata.customer_service.domain.exception.CustomerNotFoundException;
import com.nttdata.customer_service.domain.exception.DuplicateDocumentException;
import com.nttdata.customer_service.domain.model.Customer;
import com.nttdata.customer_service.domain.port.in.CreateCustomerCommand;
import com.nttdata.customer_service.domain.port.in.CreateCustomerUseCase;
import com.nttdata.customer_service.domain.port.in.DeleteCustomerUseCase;
import com.nttdata.customer_service.domain.port.in.FindCustomerUseCase;
import com.nttdata.customer_service.domain.port.in.UpdateCustomerCommand;
import com.nttdata.customer_service.domain.port.in.UpdateCustomerUseCase;
import com.nttdata.customer_service.domain.port.out.CustomerCachePort;
import com.nttdata.customer_service.domain.port.out.CustomerRepositoryPort;
import com.nttdata.customer_service.domain.validation.CustomerValidator;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Casos de uso de clientes: orquesta el dominio y el puerto de salida.
 *
 * <ul>
 *   <li>Las reglas por tipo de cliente y el formato del documento las valida {@link CustomerValidator}.</li>
 *   <li>Aquí queda lo que necesita la base: el número de documento es único.</li>
 *   <li>La eliminación es lógica.</li>
 * </ul>
 *
 * <p>Logs: solo los hechos de negocio exitosos, con eventos estructurados ({@code event=customer.*}).
 * Los errores los registra {@code GlobalExceptionHandler}, en un solo lugar y con traceId.</p>
 *
 * <p>No tiene anotaciones de Spring: se registra como bean en {@code CustomerBeanConfig}.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class CustomerApplicationService
        implements CreateCustomerUseCase, FindCustomerUseCase, UpdateCustomerUseCase, DeleteCustomerUseCase {

    private final CustomerRepositoryPort customerRepository;
    private final CustomerValidator customerValidator;
    private final CustomerCommandMapper customerCommandMapper;
    private final CustomerCachePort customerCachePort;

    @Override
    public Single<Customer> create(CreateCustomerCommand command) {
        return Single.fromCallable(() -> toValidCustomer(command))
                .flatMap(customer -> customerRepository.existsByDocumentNumber(customer.getDocumentNumber())
                        .flatMap(exists -> {
                            if (exists) {
                                return Single.error(new DuplicateDocumentException(customer.getDocumentNumber()));
                            }
                            return customerRepository.save(customer);
                        })
                )
                .doOnSuccess(customer -> log.atInfo()
                        .setMessage("Cliente creado")
                        .addKeyValue("event", "customer.created")
                        .addKeyValue("customerId", customer.getId())
                        .addKeyValue("customerType", customer.getCustomerType())
                        .log());
    }

    @Override
    public Flowable<Customer> findAll() {
        return customerRepository.findAllActive();
    }

    @Override
    public Single<Customer> findById(String id) {
        return customerCachePort.get(id)                                                 // 1. ¿está en Redis?
                .switchIfEmpty(Single.defer(() -> findActiveById(id)                     // 2. miss → Mongo
                        .flatMap(customer -> customerCachePort.put(customer)    // 3. guardarlo en Redis
                                .andThen(Single.just(customer)))));
    }

    @Override
    public Single<Customer> findByDocumentNumber(String documentNumber) {
        return customerRepository.findActiveByDocumentNumber(documentNumber)
                .switchIfEmpty(Single.error(() -> new CustomerNotFoundException(
                        CustomerNotFoundException.DOCUMENT_NUMBER, documentNumber)));
    }

    @Override
    public Single<Customer> update(String id, UpdateCustomerCommand command) {
        return findActiveById(id)
                .map(customer -> {
                    Customer c = customerCommandMapper.applyChanges(customer, command);
                    customerValidator.validate(c);
                    return c;
                })
                .flatMap(customerRepository::save)
                .flatMap(saved -> customerCachePort.evict(saved.getId())          // ← invalidar
                        .andThen(Single.just(saved)))
                .doOnSuccess(customer -> log.atInfo()
                        .setMessage("Cliente actualizado")
                        .addKeyValue("event", "customer.updated")
                        .addKeyValue("customerId", customer.getId())
                        .addKeyValue("profile", customer.getProfile())
                        .log());
    }

    @Override
    public Completable delete(String id) {
        return findActiveById(id)
                .flatMap(customer -> {
                    customer.deactivate();
                    return customerRepository.save(customer);
                })
                .ignoreElement()
                .andThen(customerCachePort.evict(id))                             // ← invalidar
                .doOnComplete(() -> log.atInfo()
                        .setMessage("Cliente desactivado")
                        .addKeyValue("event", "customer.deactivated")
                        .addKeyValue("customerId", id)
                        .log());
    }

    private Single<Customer> findActiveById(String id) {
        return customerRepository.findActiveById(id)
                .switchIfEmpty(Single.error(() -> new CustomerNotFoundException("id", id)));
    }

    /** Convierte el comando en cliente y lo valida; falla rápido si alguna regla no se cumple. */
    private Customer toValidCustomer(CreateCustomerCommand command) {
        Customer customer = customerCommandMapper.toCustomer(command);
        customerValidator.validate(customer);
        return customer;
    }

}
