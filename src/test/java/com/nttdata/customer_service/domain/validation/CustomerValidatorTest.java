package com.nttdata.customer_service.domain.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nttdata.customer_service.domain.exception.BusinessRuleException;
import com.nttdata.customer_service.domain.model.Customer;
import com.nttdata.customer_service.domain.model.CustomerProfile;
import com.nttdata.customer_service.domain.model.CustomerType;
import com.nttdata.customer_service.domain.model.DocumentType;
import com.nttdata.customer_service.domain.validation.rules.BusinessCustomerRules;
import com.nttdata.customer_service.domain.validation.rules.CustomerTypeRules;
import com.nttdata.customer_service.domain.validation.rules.PersonalCustomerRules;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pruebas del dominio: reglas puras, sin Spring, sin mocks y sin flujos reactivos.
 */
class CustomerValidatorTest {

    private CustomerValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CustomerValidator(List.of(new PersonalCustomerRules(), new BusinessCustomerRules()),
                new DocumentNumberValidator());
    }

    @Test
    @DisplayName("falla al construirse si hay dos estrategias para el mismo tipo")
    void rejectsDuplicateStrategies() {
        List<CustomerTypeRules> duplicated = List.of(new PersonalCustomerRules(), new PersonalCustomerRules());
        DocumentNumberValidator documentValidator = new DocumentNumberValidator();

        assertThatThrownBy(() -> new CustomerValidator(duplicated, documentValidator))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("rechaza un tipo de cliente sin estrategia registrada")
    void rejectsUnsupportedType() {
        CustomerValidator onlyPersonal = new CustomerValidator(List.of(new PersonalCustomerRules()),
                new DocumentNumberValidator());

        assertThatThrownBy(() -> onlyPersonal.validate(business()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no soportado");
    }

    @Nested
    @DisplayName("cliente PERSONAL")
    class Personal {

        @Test
        @DisplayName("acepta un cliente válido y lo devuelve")
        void acceptsValidCustomer() {
            Customer customer = personal();
            assertThat(validator.validate(customer)).isSameAs(customer);
        }

        @Test
        @DisplayName("acepta el perfil VIP")
        void acceptsVip() {
            Customer customer = personal();
            customer.setProfile(CustomerProfile.VIP);
            assertThat(validator.validate(customer)).isSameAs(customer);
        }

        @Test
        @DisplayName("exige firstName")
        void requiresFirstName() {
            Customer customer = personal();
            customer.setFirstName("  ");
            assertRule(customer, "firstName es obligatorio");
        }

        @Test
        @DisplayName("exige lastName")
        void requiresLastName() {
            Customer customer = personal();
            customer.setLastName(null);
            assertRule(customer, "lastName es obligatorio");
        }

        @Test
        @DisplayName("rechaza RUC")
        void rejectsRuc() {
            Customer customer = personal();
            customer.setDocumentType(DocumentType.RUC);
            customer.setDocumentNumber("20123456789");
            assertRule(customer, "no puede registrarse con RUC");
        }

        @Test
        @DisplayName("rechaza el perfil PYME")
        void rejectsPyme() {
            Customer customer = personal();
            customer.setProfile(CustomerProfile.PYME);
            assertRule(customer, "PYME solo aplica");
        }

        @ParameterizedTest(name = "rechaza el DNI \"{0}\"")
        @ValueSource(strings = {"1234567", "123456789", "1234567A"})
        void rejectsInvalidDni(String dni) {
            Customer customer = personal();
            customer.setDocumentNumber(dni);
            assertRule(customer, "El DNI debe tener 8 dígitos");
        }

        @Test
        @DisplayName("no valida el formato del pasaporte")
        void acceptsPassportWithoutFormat() {
            Customer customer = personal();
            customer.setDocumentType(DocumentType.PASSPORT);
            customer.setDocumentNumber("AB123456");
            assertThat(validator.validate(customer)).isSameAs(customer);
        }
    }

    @Nested
    @DisplayName("cliente BUSINESS")
    class Business {

        @Test
        @DisplayName("acepta una empresa válida con perfil PYME")
        void acceptsValidCustomer() {
            Customer customer = business();
            customer.setProfile(CustomerProfile.PYME);
            assertThat(validator.validate(customer)).isSameAs(customer);
        }

        @Test
        @DisplayName("exige businessName")
        void requiresBusinessName() {
            Customer customer = business();
            customer.setBusinessName("");
            assertRule(customer, "businessName es obligatorio");
        }

        @Test
        @DisplayName("exige RUC")
        void requiresRuc() {
            Customer customer = business();
            customer.setDocumentType(DocumentType.DNI);
            customer.setDocumentNumber("45678912");
            assertRule(customer, "debe registrarse con RUC");
        }

        @Test
        @DisplayName("rechaza el perfil VIP")
        void rejectsVip() {
            Customer customer = business();
            customer.setProfile(CustomerProfile.VIP);
            assertRule(customer, "VIP solo aplica");
        }

        @Test
        @DisplayName("rechaza un RUC que no tiene 11 dígitos")
        void rejectsInvalidRuc() {
            Customer customer = business();
            customer.setDocumentNumber("2012345678");
            assertRule(customer, "El RUC debe tener 11 dígitos");
        }
    }

    private void assertRule(Customer customer, String expectedMessage) {
        assertThatThrownBy(() -> validator.validate(customer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(expectedMessage);
    }

    static Customer personal() {
        return Customer.builder()
                .customerType(CustomerType.PERSONAL)
                .profile(CustomerProfile.STANDARD)
                .documentType(DocumentType.DNI)
                .documentNumber("45678912")
                .firstName("Ana")
                .lastName("Torres")
                .build();
    }

    static Customer business() {
        return Customer.builder()
                .customerType(CustomerType.BUSINESS)
                .profile(CustomerProfile.STANDARD)
                .documentType(DocumentType.RUC)
                .documentNumber("20123456789")
                .businessName("Comercial Lurín SAC")
                .build();
    }
}
