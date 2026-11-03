package com.nttdata.customer_service.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nttdata.customer_service.domain.exception.BusinessRuleException;
import com.nttdata.customer_service.domain.exception.CustomerNotFoundException;
import com.nttdata.customer_service.domain.exception.DuplicateDocumentException;
import com.nttdata.customer_service.domain.model.Customer;
import com.nttdata.customer_service.domain.model.CustomerProfile;
import com.nttdata.customer_service.domain.model.CustomerType;
import com.nttdata.customer_service.domain.model.DocumentType;
import com.nttdata.customer_service.domain.port.in.CreateCustomerCommand;
import com.nttdata.customer_service.domain.port.in.UpdateCustomerCommand;
import com.nttdata.customer_service.domain.port.out.CustomerRepositoryPort;
import com.nttdata.customer_service.domain.validation.CustomerValidator;
import com.nttdata.customer_service.domain.validation.DocumentNumberValidator;
import com.nttdata.customer_service.domain.validation.rules.BusinessCustomerRules;
import com.nttdata.customer_service.domain.validation.rules.PersonalCustomerRules;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pruebas de los casos de uso.
 *
 * <p>Solo se simula el puerto de salida ({@link CustomerRepositoryPort}), una interfaz del dominio: los casos de uso
 * no saben que existe MongoDB. El validador es el real, así que también se prueban las reglas de punta a punta.</p>
 */
@ExtendWith(MockitoExtension.class)
class CustomerApplicationServiceTest {

    private static final String ID = "6710f1c2a3b4c5d6e7f80912";
    private static final String DNI = "45678912";
    private static final String RUC = "20123456789";

    @Mock
    private CustomerRepositoryPort customerRepository;

    private CustomerApplicationService service;

    @BeforeEach
    void setUp() {
        CustomerValidator validator = new CustomerValidator(
                List.of(new PersonalCustomerRules(), new BusinessCustomerRules()), new DocumentNumberValidator());
        service = new CustomerApplicationService(customerRepository, validator);
    }

    // ---------------------------------------------------------------- create

    @Nested
    @DisplayName("create")
    class Create {
        @Test
        @DisplayName("registra un cliente PERSONAL válido con perfil STANDARD por defecto")
        void createsPersonalCustomer() {
            when(customerRepository.existsByDocumentNumber(DNI)).thenReturn(Single.just(false));
            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
                Customer customer = invocation.getArgument(0);
                customer.setId(ID);
                return Single.just(customer);
            });

            service.create(personalCommand(null, DocumentType.DNI, DNI)).test()
                    .assertComplete()
                    .assertValue(customer -> ID.equals(customer.getId())
                            && customer.getProfile() == CustomerProfile.STANDARD
                            && Boolean.TRUE.equals(customer.getActive()));
        }

        @Test
        @DisplayName("registra un cliente BUSINESS válido con RUC y perfil PYME")
        void createsBusinessCustomer() {
            when(customerRepository.existsByDocumentNumber(RUC)).thenReturn(Single.just(false));
            when(customerRepository.save(any(Customer.class)))
                    .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

            service.create(businessCommand(CustomerProfile.PYME, DocumentType.RUC, RUC, "Comercial Lurín SAC")).test()
                    .assertComplete()
                    .assertValue(customer -> customer.getCustomerType() == CustomerType.BUSINESS
                            && customer.getProfile() == CustomerProfile.PYME);
        }

        @Test
        @DisplayName("falla con DuplicateDocumentException si el documento ya existe")
        void failsWhenDocumentExists() {
            when(customerRepository.existsByDocumentNumber(DNI)).thenReturn(Single.just(true));

            service.create(personalCommand(CustomerProfile.STANDARD, DocumentType.DNI, DNI)).test()
                    .assertError(DuplicateDocumentException.class);

            verify(customerRepository, never()).save(any(Customer.class));
        }

        @Test
        @DisplayName("propaga DuplicateDocumentException si otra alta ganó la carrera al guardar")
        void propagatesDuplicateOnSave() {
            when(customerRepository.existsByDocumentNumber(DNI)).thenReturn(Single.just(false));
            when(customerRepository.save(any(Customer.class)))
                    .thenReturn(Single.error(new DuplicateDocumentException(DNI)));

            service.create(personalCommand(CustomerProfile.STANDARD, DocumentType.DNI, DNI)).test()
                    .assertError(DuplicateDocumentException.class);
        }

        @Test
        @DisplayName("rechaza el perfil VIP en un cliente BUSINESS sin consultar la base")
        void rejectsVipForBusiness() {
            assertRuleViolation(businessCommand(CustomerProfile.VIP, DocumentType.RUC, RUC, "Comercial Lurín SAC"),
                    "VIP");
        }

        @Test
        @DisplayName("rechaza el perfil PYME en un cliente PERSONAL")
        void rejectsPymeForPersonal() {
            assertRuleViolation(personalCommand(CustomerProfile.PYME, DocumentType.DNI, DNI), "PYME");
        }

        @Test
        @DisplayName("rechaza un cliente PERSONAL con RUC")
        void rejectsRucForPersonal() {
            assertRuleViolation(personalCommand(CustomerProfile.STANDARD, DocumentType.RUC, RUC), "RUC");
        }

        @Test
        @DisplayName("rechaza un cliente BUSINESS sin razón social")
        void rejectsBusinessWithoutName() {
            assertRuleViolation(businessCommand(CustomerProfile.STANDARD, DocumentType.RUC, RUC, null),
                    "businessName");
        }

        @Test
        @DisplayName("rechaza un DNI que no tiene 8 dígitos")
        void rejectsInvalidDni() {
            assertRuleViolation(personalCommand(CustomerProfile.STANDARD, DocumentType.DNI, "4567891A"), "DNI");
        }

        private void assertRuleViolation(CreateCustomerCommand command, String expectedText) {
            service.create(command).test()
                    .assertError(error -> error instanceof BusinessRuleException
                            && error.getMessage().contains(expectedText));

            verify(customerRepository, never()).existsByDocumentNumber(any());
            verify(customerRepository, never()).save(any(Customer.class));
        }
    }

    // ---------------------------------------------------------------- consultas

    @Nested
    @DisplayName("consultas")
    class Queries {

        @Test
        @DisplayName("findAll devuelve los clientes activos")
        void findAllReturnsActiveCustomers() {
            when(customerRepository.findAllActive()).thenReturn(Flowable.just(personal(), business()));

            service.findAll().test().assertComplete().assertValueCount(2);
        }

        @Test
        @DisplayName("findById devuelve el cliente si existe")
        void findByIdReturnsCustomer() {
            when(customerRepository.findActiveById(ID)).thenReturn(Maybe.just(personal()));

            service.findById(ID).test()
                    .assertComplete()
                    .assertValue(customer -> DNI.equals(customer.getDocumentNumber()));
        }

        @Test
        @DisplayName("findById falla con CustomerNotFoundException si no existe")
        void findByIdFailsWhenMissing() {
            when(customerRepository.findActiveById(ID)).thenReturn(Maybe.empty());

            service.findById(ID).test().assertError(CustomerNotFoundException.class);
        }

        @Test
        @DisplayName("findByDocumentNumber devuelve el cliente si existe")
        void findByDocumentNumberReturnsCustomer() {
            when(customerRepository.findActiveByDocumentNumber(DNI)).thenReturn(Maybe.just(personal()));

            service.findByDocumentNumber(DNI).test()
                    .assertComplete()
                    .assertValue(customer -> ID.equals(customer.getId()));
        }

        @Test
        @DisplayName("findByDocumentNumber falla con un mensaje que no expone el documento")
        void findByDocumentNumberFailsWhenMissing() {
            when(customerRepository.findActiveByDocumentNumber(DNI)).thenReturn(Maybe.empty());

            service.findByDocumentNumber(DNI).test()
                    .assertError(error -> error instanceof CustomerNotFoundException
                            && !error.getMessage().contains(DNI));
        }

        @Test
        @DisplayName("actualiza el nombre de una persona")
        void updatesFirstName() {
            when(customerRepository.findActiveById(ID)).thenReturn(Maybe.just(personal()));
            when(customerRepository.save(any(Customer.class)))
                    .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));
            UpdateCustomerCommand command = new UpdateCustomerCommand(null, "Ana", null, null,
                    "nuevo@mail.com", "912345678", null);

            service.update(ID, command).test()
                    .assertComplete()
                    .assertValue(customer -> "Ana".equals(customer.getFirstName()));
        }
    }

    // ---------------------------------------------------------------- update

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("actualiza los datos editables y conserva los que llegan en null")
        void updatesCustomer() {
            when(customerRepository.findActiveById(ID)).thenReturn(Maybe.just(personal()));
            when(customerRepository.save(any(Customer.class)))
                    .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));
            UpdateCustomerCommand command = new UpdateCustomerCommand(CustomerProfile.VIP, null, "Pérez Díaz",
                    null, "nuevo@mail.com", "912345678", "Av. Lima 123");

            service.update(ID, command).test()
                    .assertComplete()
                    .assertValue(customer -> "nuevo@mail.com".equals(customer.getEmail())
                            && customer.getProfile() == CustomerProfile.VIP
                            && "Juan".equals(customer.getFirstName())
                            && "Pérez Díaz".equals(customer.getLastName())
                            && "Av. Lima 123".equals(customer.getAddress()));
        }

        @Test
        @DisplayName("actualiza la razón social de una empresa")
        void updatesBusinessName() {
            when(customerRepository.findActiveById(ID)).thenReturn(Maybe.just(business()));
            when(customerRepository.save(any(Customer.class)))
                    .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));
            UpdateCustomerCommand command = new UpdateCustomerCommand(null, null, null, "Lurín Express SAC",
                    "empresa@mail.com", "912345678", null);

            service.update(ID, command).test()
                    .assertComplete()
                    .assertValue(customer -> "Lurín Express SAC".equals(customer.getBusinessName()));
        }

        @Test
        @DisplayName("rechaza cambiar a VIP un cliente BUSINESS")
        void rejectsVipForBusiness() {
            when(customerRepository.findActiveById(ID)).thenReturn(Maybe.just(business()));
            UpdateCustomerCommand command = new UpdateCustomerCommand(CustomerProfile.VIP, null, null, null,
                    "empresa@mail.com", "912345678", null);

            service.update(ID, command).test().assertError(BusinessRuleException.class);

            verify(customerRepository, never()).save(any(Customer.class));
        }

        @Test
        @DisplayName("falla con CustomerNotFoundException si el cliente no existe")
        void failsWhenMissing() {
            when(customerRepository.findActiveById(ID)).thenReturn(Maybe.empty());
            UpdateCustomerCommand command = new UpdateCustomerCommand(null, null, null, null,
                    "nuevo@mail.com", "912345678", null);

            service.update(ID, command).test().assertError(CustomerNotFoundException.class);
        }
    }

    // ---------------------------------------------------------------- delete

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("hace un borrado lógico (active = false)")
        void softDeletesCustomer() {
            when(customerRepository.findActiveById(ID)).thenReturn(Maybe.just(personal()));
            when(customerRepository.save(any(Customer.class)))
                    .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

            service.delete(ID).test().assertComplete();

            ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
            verify(customerRepository).save(captor.capture());
            assertThat(captor.getValue().getActive()).isFalse();
        }

        @Test
        @DisplayName("falla con CustomerNotFoundException si el cliente no existe")
        void failsWhenMissing() {
            when(customerRepository.findActiveById(ID)).thenReturn(Maybe.empty());

            service.delete(ID).test().assertError(CustomerNotFoundException.class);

            verify(customerRepository, never()).save(any(Customer.class));
        }
    }

    // ---------------------------------------------------------------- datos de prueba

    private static CreateCustomerCommand personalCommand(CustomerProfile profile, DocumentType type, String number) {
        return new CreateCustomerCommand(type, number, CustomerType.PERSONAL, profile, "Juan", "Pérez", null,
                "juan.perez@mail.com", "987654321", null);
    }

    private static CreateCustomerCommand businessCommand(CustomerProfile profile, DocumentType type, String number,
                                                         String businessName) {
        return new CreateCustomerCommand(type, number, CustomerType.BUSINESS, profile, null, null, businessName,
                "contacto@lurin.com", "987654321", null);
    }

    private static Customer personal() {
        return Customer.builder()
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
                .version(0L)
                .build();
    }

    private static Customer business() {
        return Customer.builder()
                .id(ID)
                .documentType(DocumentType.RUC)
                .documentNumber(RUC)
                .customerType(CustomerType.BUSINESS)
                .profile(CustomerProfile.STANDARD)
                .businessName("Comercial Lurín SAC")
                .email("contacto@lurin.com")
                .phone("987654321")
                .active(Boolean.TRUE)
                .version(0L)
                .build();
    }
}
