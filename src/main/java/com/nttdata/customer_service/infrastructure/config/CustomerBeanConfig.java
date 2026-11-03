package com.nttdata.customer_service.infrastructure.config;

import com.nttdata.customer_service.application.service.CustomerApplicationService;
import com.nttdata.customer_service.domain.port.out.CustomerRepositoryPort;
import com.nttdata.customer_service.domain.validation.CustomerValidator;
import com.nttdata.customer_service.domain.validation.DocumentNumberValidator;
import com.nttdata.customer_service.domain.validation.rules.BusinessCustomerRules;
import com.nttdata.customer_service.domain.validation.rules.CustomerTypeRules;
import com.nttdata.customer_service.domain.validation.rules.PersonalCustomerRules;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensambla el dominio y la aplicación como beans de Spring.
 *
 * <p>El dominio y la capa de aplicación no tienen anotaciones de Spring: aquí, en la infraestructura,
 * se decide cómo se crean y se conectan. Así el núcleo no depende del framework.</p>
 */
@Configuration
public class CustomerBeanConfig {

    /**
     * @return estrategia de validación de clientes PERSONAL
     */
    @Bean
    public CustomerTypeRules personalCustomerRules() {
        return new PersonalCustomerRules();
    }

    /**
     * @return estrategia de validación de clientes BUSINESS
     */
    @Bean
    public CustomerTypeRules businessCustomerRules() {
        return new BusinessCustomerRules();
    }

    /**
     * @return validador del formato del documento
     */
    @Bean
    public DocumentNumberValidator documentNumberValidator() {
        return new DocumentNumberValidator();
    }

    /**
     * @param rules                   todas las estrategias registradas
     * @param documentNumberValidator validador del documento
     * @return validador de reglas de negocio (contexto del patrón Strategy)
     */
    @Bean
    public CustomerValidator customerValidator(List<CustomerTypeRules> rules, DocumentNumberValidator documentNumberValidator) {
        return new CustomerValidator(rules, documentNumberValidator);
    }

    /**
     * Un solo bean implementa los cuatro casos de uso; el controlador los recibe por su interfaz.
     *
     * @param customerRepository puerto de salida (lo implementa el adaptador de MongoDB)
     * @param customerValidator  validador de reglas de negocio
     * @return servicio de aplicación
     */
    @Bean
    public CustomerApplicationService customerApplicationService(CustomerRepositoryPort customerRepository,
                                                                 CustomerValidator customerValidator) {
        return new CustomerApplicationService(customerRepository, customerValidator);
    }
}
