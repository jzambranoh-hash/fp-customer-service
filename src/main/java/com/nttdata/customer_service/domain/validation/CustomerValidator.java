package com.nttdata.customer_service.domain.validation;

import com.nttdata.customer_service.domain.exception.BusinessRuleException;
import com.nttdata.customer_service.domain.model.Customer;
import com.nttdata.customer_service.domain.model.CustomerType;
import com.nttdata.customer_service.domain.validation.rules.CustomerTypeRules;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Punto único de validación de reglas de negocio del cliente (contexto del patrón Strategy).
 *
 * <p>Elige la estrategia según el tipo de cliente y luego valida el número de documento.
 * Solo contiene reglas puras: la unicidad del documento, que consulta la base, queda en el caso de uso.</p>
 */
public class CustomerValidator {

    private final Map<CustomerType, CustomerTypeRules> rulesByType;
    private final DocumentNumberValidator documentNumberValidator;

    /**
     * @param rules                   estrategias por tipo de cliente
     * @param documentNumberValidator validador del número de documento
     * @throws IllegalStateException si hay dos estrategias para el mismo tipo
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
    public Customer validate(Customer customer) {
        CustomerTypeRules rules = rulesByType.get(customer.getCustomerType());
        if (rules == null) {
            throw new BusinessRuleException("Tipo de cliente no soportado: " + customer.getCustomerType());
        }
        rules.validate(customer);
        documentNumberValidator.validate(customer.getDocumentType(), customer.getDocumentNumber());
        return customer;
    }
}
