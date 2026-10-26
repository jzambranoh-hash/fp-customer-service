package com.nttdata.customer_service.service.impl;

import com.nttdata.bootcamp.customer.dto.*;
import com.nttdata.customer_service.document.CustomerDocument;
import com.nttdata.customer_service.exception.BusinessRuleException;
import com.nttdata.customer_service.exception.CustomerNotFoundException;
import com.nttdata.customer_service.exception.DuplicateDocumentException;
import com.nttdata.customer_service.mapper.CustomerMapper;
import com.nttdata.customer_service.repository.CustomerRepository;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceImplTest {

    private static final String ID = "6710f1c2a3b4c5d6e7f80912";
    private static final String DNI = "45678912";
    private static final String RUC = "20123456789";


    @Mock
    private CustomerRepository customerRepository;

    private CustomerServiceImpl customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerServiceImpl(customerRepository, new CustomerMapper());
    }

    // ---------------------------------------------------------------- create

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("registra un cliente PERSONAL válido con perfil STANDARD por defecto")
        void createsPersonalCustomer() {
            CustomerRequest request = personalRequest();
            request.setProfile(null);
            when(customerRepository.existsByDocumentNumber(DNI)).thenReturn(Single.just(false));
            when(customerRepository.save(any(CustomerDocument.class))).thenAnswer(invocation -> {
                CustomerDocument document = invocation.getArgument(0);
                document.setId(ID);
                return Single.just(document);
            });
            customerService.create(request).test()
                    .assertComplete()
                    .assertValue(response -> ID.equals(response.getId())
                            && response.getProfile() == CustomerProfile.STANDARD
                            && Boolean.TRUE.equals(response.getActive()));
        }

        @Test
        @DisplayName("registra un cliente BUSINESS válido con RUC y perfil PYME")
        void createsBusinessCustomer() {
            CustomerRequest request = businessRequest();
            request.setProfile(CustomerProfile.PYME);
            when(customerRepository.existsByDocumentNumber(RUC)).thenReturn(Single.just(false));
            when(customerRepository.save(any(CustomerDocument.class)))
                    .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

            customerService.create(request).test()
                    .assertComplete()
                    .assertValue(response -> response.getCustomerType() == CustomerType.BUSINESS
                            && response.getProfile() == CustomerProfile.PYME);
        }

        @Test
        @DisplayName("falla con DuplicateDocumentException si el documento ya existe")
        void failsWhenDocumentExists() {
            when(customerRepository.existsByDocumentNumber(DNI)).thenReturn(Single.just(true));

            customerService.create(personalRequest()).test()
                    .assertError(DuplicateDocumentException.class);

            verify(customerRepository, never()).save(any(CustomerDocument.class));
        }

        @Test
        @DisplayName("convierte DuplicateKeyException de MongoDB en DuplicateDocumentException")
        void translatesDuplicateKey() {
            when(customerRepository.existsByDocumentNumber(DNI)).thenReturn(Single.just(false));
            when(customerRepository.save(any(CustomerDocument.class)))
                    .thenReturn(Single.error(new DuplicateKeyException("E11000 duplicate key")));

            customerService.create(personalRequest()).test()
                    .assertError(DuplicateDocumentException.class);
        }

        @Test
        @DisplayName("rechaza el perfil VIP en un cliente BUSINESS")
        void rejectsVipForBusiness() {
            CustomerRequest request = businessRequest();
            request.setProfile(CustomerProfile.VIP);

            assertBusinessRuleViolation(request, "VIP");
        }

        @Test
        @DisplayName("rechaza el perfil PYME en un cliente PERSONAL")
        void rejectsPymeForPersonal() {
            CustomerRequest request = personalRequest();
            request.setProfile(CustomerProfile.PYME);

            assertBusinessRuleViolation(request, "PYME");
        }

        @Test
        @DisplayName("rechaza un cliente PERSONAL con RUC")
        void rejectsRucForPersonal() {
            CustomerRequest request = personalRequest();
            request.setDocumentType(DocumentType.RUC);
            request.setDocumentNumber(RUC);

            assertBusinessRuleViolation(request, "RUC");
        }

        @Test
        @DisplayName("rechaza un cliente BUSINESS sin RUC")
        void rejectsBusinessWithoutRuc() {
            CustomerRequest request = businessRequest();
            request.setDocumentType(DocumentType.DNI);
            request.setDocumentNumber(DNI);

            assertBusinessRuleViolation(request, "RUC");
        }

        @Test
        @DisplayName("rechaza un cliente PERSONAL sin nombres")
        void rejectsPersonalWithoutFirstName() {
            CustomerRequest request = personalRequest();
            request.setFirstName("  ");

            assertBusinessRuleViolation(request, "firstName");
        }

        @Test
        @DisplayName("rechaza un cliente BUSINESS sin razón social")
        void rejectsBusinessWithoutBusinessName() {
            CustomerRequest request = businessRequest();
            request.setBusinessName(null);

            assertBusinessRuleViolation(request, "businessName");
        }

        @Test
        @DisplayName("rechaza un DNI que no tiene 8 dígitos")
        void rejectsInvalidDni() {
            CustomerRequest request = personalRequest();
            request.setDocumentNumber("4567891A");

            assertBusinessRuleViolation(request, "DNI");
        }

        @Test
        @DisplayName("rechaza un RUC que no tiene 11 dígitos")
        void rejectsInvalidRuc() {
            CustomerRequest request = businessRequest();
            request.setDocumentNumber("2012345678");

            assertBusinessRuleViolation(request, "RUC");
        }

        private void assertBusinessRuleViolation(CustomerRequest request, String expectedText) {
            customerService.create(request).test()
                    .assertError(error -> error instanceof BusinessRuleException
                            && error.getMessage().contains(expectedText));

            verify(customerRepository, never()).existsByDocumentNumber(any());
            verify(customerRepository, never()).save(any(CustomerDocument.class));
        }
    }

    // ---------------------------------------------------------------- consultas

    @Nested
    @DisplayName("consultas")
    class Queries {

        @Test
        @DisplayName("findAll devuelve los clientes activos")
        void findAllReturnsActiveCustomers() {
            when(customerRepository.findByActiveTrue())
                    .thenReturn(Flowable.just(personalDocument(), businessDocument()));

            customerService.findAll().test()
                    .assertComplete()
                    .assertValueCount(2);
        }

        @Test
        @DisplayName("findById devuelve el cliente si existe")
        void findByIdReturnsCustomer() {
            when(customerRepository.findByIdAndActiveTrue(ID)).thenReturn(Maybe.just(personalDocument()));

            customerService.findById(ID).test()
                    .assertComplete()
                    .assertValue(response -> DNI.equals(response.getDocumentNumber()));
        }

        @Test
        @DisplayName("findById falla con CustomerNotFoundException si no existe")
        void findByIdFailsWhenMissing() {
            when(customerRepository.findByIdAndActiveTrue(ID)).thenReturn(Maybe.empty());

            customerService.findById(ID).test()
                    .assertError(CustomerNotFoundException.class);
        }

        @Test
        @DisplayName("findByDocumentNumber devuelve el cliente si existe")
        void findByDocumentNumberReturnsCustomer() {
            when(customerRepository.findByDocumentNumberAndActiveTrue(DNI))
                    .thenReturn(Maybe.just(personalDocument()));

            customerService.findByDocumentNumber(DNI).test()
                    .assertComplete()
                    .assertValue(response -> ID.equals(response.getId()));
        }

        @Test
        @DisplayName("findByDocumentNumber falla con CustomerNotFoundException si no existe")
        void findByDocumentNumberFailsWhenMissing() {
            when(customerRepository.findByDocumentNumberAndActiveTrue(DNI)).thenReturn(Maybe.empty());

            customerService.findByDocumentNumber(DNI).test()
                    .assertError(CustomerNotFoundException.class);
        }
    }

    // ---------------------------------------------------------------- update

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("actualiza los datos editables y conserva los que llegan en null")
        void updatesCustomer() {
            CustomerUpdateRequest request = new CustomerUpdateRequest();
            request.setEmail("nuevo@mail.com");
            request.setPhone("912345678");
            request.setProfile(CustomerProfile.VIP);
            when(customerRepository.findByIdAndActiveTrue(ID)).thenReturn(Maybe.just(personalDocument()));
            when(customerRepository.save(any(CustomerDocument.class)))
                    .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

            customerService.update(ID, request).test()
                    .assertComplete()
                    .assertValue(response -> "nuevo@mail.com".equals(response.getEmail())
                            && response.getProfile() == CustomerProfile.VIP
                            && "Juan".equals(response.getFirstName()));
        }

        @Test
        @DisplayName("rechaza cambiar a VIP un cliente BUSINESS")
        void rejectsVipForBusiness() {
            CustomerUpdateRequest request = new CustomerUpdateRequest();
            request.setEmail("empresa@mail.com");
            request.setPhone("912345678");
            request.setProfile(CustomerProfile.VIP);
            when(customerRepository.findByIdAndActiveTrue(ID)).thenReturn(Maybe.just(businessDocument()));

            customerService.update(ID, request).test()
                    .assertError(BusinessRuleException.class);

            verify(customerRepository, never()).save(any(CustomerDocument.class));
        }

        @Test
        @DisplayName("falla con CustomerNotFoundException si el cliente no existe")
        void failsWhenMissing() {
            CustomerUpdateRequest request = new CustomerUpdateRequest();
            request.setEmail("nuevo@mail.com");
            request.setPhone("912345678");
            when(customerRepository.findByIdAndActiveTrue(ID)).thenReturn(Maybe.empty());

            customerService.update(ID, request).test()
                    .assertError(CustomerNotFoundException.class);
        }
    }

    // ---------------------------------------------------------------- delete

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("hace un borrado lógico (active = false)")
        void softDeletesCustomer() {
            when(customerRepository.findByIdAndActiveTrue(ID)).thenReturn(Maybe.just(personalDocument()));
            when(customerRepository.save(any(CustomerDocument.class)))
                    .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

            customerService.delete(ID).test().assertComplete();

            ArgumentCaptor<CustomerDocument> captor = ArgumentCaptor.forClass(CustomerDocument.class);
            verify(customerRepository).save(captor.capture());
            assertThat(captor.getValue().getActive()).isFalse();
        }

        @Test
        @DisplayName("falla con CustomerNotFoundException si el cliente no existe")
        void failsWhenMissing() {
            when(customerRepository.findByIdAndActiveTrue(ID)).thenReturn(Maybe.empty());

            customerService.delete(ID).test()
                    .assertError(CustomerNotFoundException.class);

            verify(customerRepository, never()).save(any(CustomerDocument.class));
        }
    }

    // ---------------------------------------------------------------- datos de prueba

    private static CustomerRequest personalRequest() {
        CustomerRequest request = new CustomerRequest();
        request.setDocumentType(DocumentType.DNI);
        request.setDocumentNumber(DNI);
        request.setCustomerType(CustomerType.PERSONAL);
        request.setProfile(CustomerProfile.STANDARD);
        request.setFirstName("Juan");
        request.setLastName("Pérez");
        request.setEmail("juan.perez@mail.com");
        request.setPhone("987654321");
        return request;
    }

    private static CustomerRequest businessRequest() {
        CustomerRequest request = new CustomerRequest();
        request.setDocumentType(DocumentType.RUC);
        request.setDocumentNumber(RUC);
        request.setCustomerType(CustomerType.BUSINESS);
        request.setProfile(CustomerProfile.STANDARD);
        request.setBusinessName("Comercial Lurín SAC");
        request.setEmail("contacto@lurin.com");
        request.setPhone("987654321");
        return request;
    }

    private static CustomerDocument personalDocument() {
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
                .build();
    }

    private static CustomerDocument businessDocument() {
        return CustomerDocument.builder()
                .id(ID)
                .documentType(DocumentType.RUC)
                .documentNumber(RUC)
                .customerType(CustomerType.BUSINESS)
                .profile(CustomerProfile.STANDARD)
                .businessName("Comercial Lurín SAC")
                .email("contacto@lurin.com")
                .phone("987654321")
                .active(Boolean.TRUE)
                .build();
    }

}
