package com.nttdata.customer_service.domain.validation.rules;

import com.nttdata.customer_service.domain.exception.BusinessRuleException;
import com.nttdata.customer_service.domain.model.Customer;
import com.nttdata.customer_service.domain.model.CustomerType;

/**
 * Estrategia de validación para un tipo de cliente (patrón Strategy).
 *
 * <p>Cada implementación encapsula las reglas de un {@link CustomerType}. Para agregar un tipo nuevo se crea
 * otra implementación y se registra como bean; {@code CustomerValidator} la recibe sin cambiar su código
 * (principio abierto/cerrado).</p>
 */
public interface CustomerTypeRules {

    /**
     * @return tipo de cliente que valida esta estrategia
     */
    CustomerType supports();

    /**
     * Valida las reglas propias del tipo de cliente.
     *
     * @param customer cliente a validar
     * @throws BusinessRuleException si incumple alguna regla
     */
    void validate(Customer customer);

    /**
     * Exige que un campo de texto tenga contenido.
     *
     * @param value   valor a revisar
     * @param message mensaje si está vacío
     * @throws BusinessRuleException si es null o solo espacios
     */
    static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(message);
        }
    }
}
