package com.nttdata.customer_service.application.service;

import com.nttdata.customer_service.domain.exception.CustomerNotFoundException;
import com.nttdata.customer_service.domain.exception.DuplicateDocumentException;
import com.nttdata.customer_service.domain.model.Customer;
import com.nttdata.customer_service.domain.model.CustomerProfile;
import com.nttdata.customer_service.domain.port.in.CreateCustomerCommand;
import com.nttdata.customer_service.domain.port.in.CreateCustomerUseCase;
import com.nttdata.customer_service.domain.port.in.DeleteCustomerUseCase;
import com.nttdata.customer_service.domain.port.in.FindCustomerUseCase;
import com.nttdata.customer_service.domain.port.in.UpdateCustomerCommand;
import com.nttdata.customer_service.domain.port.in.UpdateCustomerUseCase;
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

    // ------------------------------------------------------------------ registro

    @Override
    public Single<Customer> create(CreateCustomerCommand command) {
        return Single.fromCallable(() -> newCustomer(command))
                .map(customerValidator::validate)
                .flatMap(customer -> customerRepository.existsByDocumentNumber(customer.getDocumentNumber())
                        .flatMap(exists -> Boolean.TRUE.equals(exists)
                                ? Single.<Customer>error(new DuplicateDocumentException(customer.getDocumentNumber()))
                                : customerRepository.save(customer)))
                .doOnSuccess(customer -> log.atInfo()
                        .setMessage("Cliente creado")
                        .addKeyValue("event", "customer.created")
                        .addKeyValue("customerId", customer.getId())
                        .addKeyValue("customerType", customer.getCustomerType())
                        .log());
    }

    private Customer newCustomer(CreateCustomerCommand command) {
        return Customer.builder()
                .documentType(command.documentType())
                .documentNumber(command.documentNumber())
                .customerType(command.customerType())
                .profile(command.profile() != null ? command.profile() : CustomerProfile.STANDARD)
                .firstName(command.firstName())
                .lastName(command.lastName())
                .businessName(command.businessName())
                .email(command.email())
                .phone(command.phone())
                .address(command.address())
                .active(Boolean.TRUE)
                .build();
    }

    // ------------------------------------------------------------------ consultas

    @Override
    public Flowable<Customer> findAll() {
        return customerRepository.findAllActive();
    }

    @Override
    public Single<Customer> findById(String id) {
        return findActiveById(id);
    }

    @Override
    public Single<Customer> findByDocumentNumber(String documentNumber) {
        return customerRepository.findActiveByDocumentNumber(documentNumber)
                .switchIfEmpty(Single.<Customer>error(() -> new CustomerNotFoundException(
                        CustomerNotFoundException.DOCUMENT_NUMBER, documentNumber)));
    }

    // ------------------------------------------------------------------ modificación

    @Override
    public Single<Customer> update(String id, UpdateCustomerCommand command) {
        return findActiveById(id)
                .map(customer -> customerValidator.validate(applyChanges(customer, command)))
                .flatMap(customerRepository::save)
                .doOnSuccess(customer -> log.atInfo()
                        .setMessage("Cliente actualizado")
                        .addKeyValue("event", "customer.updated")
                        .addKeyValue("customerId", customer.getId())
                        .addKeyValue("profile", customer.getProfile())
                        .log());
    }

    /**
     * Los campos opcionales en null conservan su valor; email y phone siempre se reemplazan.
     */
    private Customer applyChanges(Customer customer, UpdateCustomerCommand command) {
        if (command.profile() != null) {
            customer.setProfile(command.profile());
        }
        if (command.firstName() != null) {
            customer.setFirstName(command.firstName());
        }
        if (command.lastName() != null) {
            customer.setLastName(command.lastName());
        }
        if (command.businessName() != null) {
            customer.setBusinessName(command.businessName());
        }
        if (command.address() != null) {
            customer.setAddress(command.address());
        }
        customer.setEmail(command.email());
        customer.setPhone(command.phone());
        return customer;
    }

    @Override
    public Completable delete(String id) {
        return findActiveById(id)
                .flatMap(customer -> {
                    customer.deactivate();
                    return customerRepository.save(customer);
                })
                .ignoreElement()
                .doOnComplete(() -> log.atInfo()
                        .setMessage("Cliente desactivado")
                        .addKeyValue("event", "customer.deactivated")
                        .addKeyValue("customerId", id)
                        .log());
    }

    // ------------------------------------------------------------------ auxiliares

    private Single<Customer> findActiveById(String id) {
        return customerRepository.findActiveById(id)
                .switchIfEmpty(Single.<Customer>error(() -> new CustomerNotFoundException("id", id)));
    }
}
