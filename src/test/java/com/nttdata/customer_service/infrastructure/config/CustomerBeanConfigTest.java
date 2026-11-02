package com.nttdata.customer_service.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.nttdata.customer_service.application.service.CustomerApplicationService;
import com.nttdata.customer_service.domain.model.CustomerType;
import com.nttdata.customer_service.domain.port.out.CustomerRepositoryPort;
import com.nttdata.customer_service.domain.validation.CustomerValidator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifica el ensamblado del dominio sin levantar Spring.
 */
class CustomerBeanConfigTest {

    private final CustomerBeanConfig config = new CustomerBeanConfig();

    @Test
    @DisplayName("registra una estrategia por cada tipo de cliente y arma el servicio")
    void wiresDomain() {
        assertThat(config.personalCustomerRules().supports()).isEqualTo(CustomerType.PERSONAL);
        assertThat(config.businessCustomerRules().supports()).isEqualTo(CustomerType.BUSINESS);

        CustomerValidator validator = config.customerValidator(
                List.of(config.personalCustomerRules(), config.businessCustomerRules()),
                config.documentNumberValidator());
        CustomerApplicationService service =
                config.customerApplicationService(mock(CustomerRepositoryPort.class), validator);

        assertThat(service).isNotNull();
    }
}
