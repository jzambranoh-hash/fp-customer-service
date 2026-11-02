package com.nttdata.customer_service.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nttdata.customer_service.domain.exception.DuplicateDocumentException;
import com.nttdata.customer_service.domain.model.Customer;
import com.nttdata.customer_service.domain.model.CustomerProfile;
import com.nttdata.customer_service.domain.model.CustomerType;
import com.nttdata.customer_service.domain.model.DocumentType;
import com.nttdata.customer_service.infrastructure.adapter.out.persistence.adapter.CustomerPersistenceAdapter;
import com.nttdata.customer_service.infrastructure.adapter.out.persistence.document.CustomerDocument;
import com.nttdata.customer_service.infrastructure.adapter.out.persistence.mapper.CustomerPersistenceMapper;
import com.nttdata.customer_service.infrastructure.adapter.out.persistence.repository.CustomerMongoRepository;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;

/**
 * Pruebas del adaptador de MongoDB: conversión documento ↔ dominio y traducción de errores de Mongo.
 */
@ExtendWith(MockitoExtension.class)
class CustomerPersistenceAdapterTest {

    private static final String ID = "6710f1c2a3b4c5d6e7f80912";
    private static final String DNI = "45678912";

    @Mock
    private CustomerMongoRepository repository;

    private CustomerPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CustomerPersistenceAdapter(repository, new CustomerPersistenceMapper());
    }

    @Test
    @DisplayName("findAllActive convierte los documentos a dominio")
    void findAllActive() {
        when(repository.findByActiveTrue()).thenReturn(Flowable.just(document(), document()));

        adapter.findAllActive().test()
                .assertComplete()
                .assertValueCount(2)
                .assertValueAt(0, customer -> DNI.equals(customer.getDocumentNumber())
                        && customer.getCustomerType() == CustomerType.PERSONAL);
    }

    @Test
    @DisplayName("findActiveById conserva id, versión y fechas")
    void findActiveById() {
        when(repository.findByIdAndActiveTrue(ID)).thenReturn(Maybe.just(document()));

        adapter.findActiveById(ID).test()
                .assertValue(customer -> ID.equals(customer.getId())
                        && Long.valueOf(3L).equals(customer.getVersion())
                        && customer.getCreatedAt() != null);
    }

    @Test
    @DisplayName("findActiveByDocumentNumber devuelve vacío si no existe")
    void findActiveByDocumentNumberEmpty() {
        when(repository.findByDocumentNumberAndActiveTrue(DNI)).thenReturn(Maybe.empty());

        adapter.findActiveByDocumentNumber(DNI).test().assertComplete().assertNoValues();
    }

    @Test
    @DisplayName("existsByDocumentNumber delega en el repositorio")
    void existsByDocumentNumber() {
        when(repository.existsByDocumentNumber(DNI)).thenReturn(Single.just(true));

        adapter.existsByDocumentNumber(DNI).test().assertValue(true);
    }

    @Test
    @DisplayName("save guarda el documento con su versión y devuelve el dominio")
    void save() {
        when(repository.save(any(CustomerDocument.class)))
                .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

        adapter.save(customer()).test()
                .assertComplete()
                .assertValue(saved -> ID.equals(saved.getId()) && saved.getProfile() == CustomerProfile.VIP);

        ArgumentCaptor<CustomerDocument> captor = ArgumentCaptor.forClass(CustomerDocument.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo(3L);
        assertThat(captor.getValue().getActive()).isTrue();
    }

    @Test
    @DisplayName("save traduce DuplicateKeyException de Mongo a DuplicateDocumentException")
    void translatesDuplicateKey() {
        when(repository.save(any(CustomerDocument.class)))
                .thenReturn(Single.error(new DuplicateKeyException("E11000 duplicate key")));

        adapter.save(customer()).test().assertError(DuplicateDocumentException.class);
    }

    @Test
    @DisplayName("save deja pasar los demás errores sin cambiarlos")
    void propagatesOtherErrors() {
        when(repository.save(any(CustomerDocument.class)))
                .thenReturn(Single.error(new DataAccessResourceFailureException("Mongo caído")));

        adapter.save(customer()).test().assertError(DataAccessResourceFailureException.class);
    }

    private static CustomerDocument document() {
        return CustomerDocument.builder()
                .id(ID)
                .documentType(DocumentType.DNI)
                .documentNumber(DNI)
                .customerType(CustomerType.PERSONAL)
                .profile(CustomerProfile.STANDARD)
                .firstName("Juan")
                .lastName("Pérez")
                .email("juan.perez@mail.com")
                .phone("987654321")
                .active(Boolean.TRUE)
                .createdAt(LocalDateTime.of(2026, 10, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 10, 2, 9, 0))
                .version(3L)
                .build();
    }

    private static Customer customer() {
        return Customer.builder()
                .id(ID)
                .documentType(DocumentType.DNI)
                .documentNumber(DNI)
                .customerType(CustomerType.PERSONAL)
                .profile(CustomerProfile.VIP)
                .firstName("Juan")
                .lastName("Pérez")
                .email("juan.perez@mail.com")
                .phone("987654321")
                .address("Av. Lima 123")
                .active(Boolean.TRUE)
                .version(3L)
                .build();
    }
}
