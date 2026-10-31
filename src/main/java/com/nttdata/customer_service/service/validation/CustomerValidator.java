package com.nttdata.customer_service.service.validation;

import com.nttdata.bootcamp.customer.dto.CustomerType;
import com.nttdata.customer_service.document.CustomerDocument;
import com.nttdata.customer_service.exception.BusinessRuleException;
import com.nttdata.customer_service.service.validation.rules.CustomerTypeRules;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Punto único de validación de reglas de negocio del cliente (contexto del patrón Strategy).
 *
 * <p>Elige la estrategia según el tipo de cliente y luego valida el número de documento.
 * Solo contiene reglas puras: la unicidad del documento, que consulta la base, queda en el servicio.</p>
 */
@Component
public class CustomerValidator {

    private final Map<CustomerType, CustomerTypeRules> rulesByType;
    private final DocumentNumberValidator documentNumberValidator;

    /**
     * Spring inyecta todas las implementaciones de {@link CustomerTypeRules} que existan como {@code @Component}.
     *
     * @param rules                   estrategias por tipo de cliente
     * @param documentNumberValidator validador del número de documento
     */
    public CustomerValidator(List<CustomerTypeRules> rules, DocumentNumberValidator documentNumberValidator) {
        this.rulesByType = rules.stream()
                .collect(Collectors.toMap(CustomerTypeRules::supports, Function.identity(),
                        (first, duplicate) -> {
                            throw new IllegalStateException("Hay dos estrategias para " + first.supports());
                        },
                        () -> new EnumMap<>(CustomerType.class)));
        this.documentNumberValidator = documentNumberValidator;
    }

    /**
     * Valida el cliente; pensado para usarse dentro de un {@code map} de RxJava.
     *
     * @param customer cliente a validar
     * @return el mismo cliente, si es válido
     * @throws BusinessRuleException si incumple alguna regla
     */
    public CustomerDocument validate(CustomerDocument customer) {
        CustomerTypeRules rules = rulesByType.get(customer.getCustomerType());
        if (rules == null) {
            throw new BusinessRuleException("Tipo de cliente no soportado: " + customer.getCustomerType());
        }
        rules.validate(customer);
        documentNumberValidator.validate(customer.getDocumentType(), customer.getDocumentNumber());
        return customer;
    }
}
