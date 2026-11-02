package com.nttdata.customer_service.infrastructure.adapter.out.persistence.adapter;

import com.nttdata.customer_service.domain.exception.DuplicateDocumentException;
import com.nttdata.customer_service.domain.model.Customer;
import com.nttdata.customer_service.domain.port.out.CustomerRepositoryPort;
import com.nttdata.customer_service.infrastructure.adapter.out.persistence.repository.CustomerMongoRepository;
import com.nttdata.customer_service.infrastructure.adapter.out.persistence.mapper.CustomerPersistenceMapper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida: implementa {@link CustomerRepositoryPort} con MongoDB.
 *
 * <p>Traduce los detalles de Mongo al lenguaje del dominio: si dos altas con el mismo documento llegan a la vez,
 * el índice único lanza {@link DuplicateKeyException} y aquí se convierte en {@link DuplicateDocumentException}.</p>
 */
@Component
@RequiredArgsConstructor
public class CustomerPersistenceAdapter implements CustomerRepositoryPort {

    private final CustomerMongoRepository repository;
    private final CustomerPersistenceMapper mapper;

    @Override
    public Flowable<Customer> findAllActive() {
        return repository.findByActiveTrue().map(mapper::toDomain);
    }

    @Override
    public Maybe<Customer> findActiveById(String id) {
        return repository.findByIdAndActiveTrue(id).map(mapper::toDomain);
    }

    @Override
    public Maybe<Customer> findActiveByDocumentNumber(String documentNumber) {
        return repository.findByDocumentNumberAndActiveTrue(documentNumber).map(mapper::toDomain);
    }

    @Override
    public Single<Boolean> existsByDocumentNumber(String documentNumber) {
        return repository.existsByDocumentNumber(documentNumber);
    }

    @Override
    public Single<Customer> save(Customer customer) {
        return Single.fromCallable(() -> mapper.toDocument(customer))
                .flatMap(repository::save)
                .map(mapper::toDomain)
                .onErrorResumeNext(error -> Single.error(error instanceof DuplicateKeyException
                        ? new DuplicateDocumentException(customer.getDocumentNumber())
                        : error));
    }
}
