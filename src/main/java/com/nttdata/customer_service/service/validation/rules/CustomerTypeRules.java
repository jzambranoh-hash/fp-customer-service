package com.nttdata.customer_service.service.validation.rules;

import com.nttdata.bootcamp.customer.dto.CustomerType;
import com.nttdata.customer_service.document.CustomerDocument;
import com.nttdata.customer_service.exception.BusinessRuleException;

/**
 * Estrategia de validación para un tipo de cliente (patrón Strategy).
 *
 * <p>Cada implementación encapsula las reglas de un {@link CustomerType}. Para agregar un tipo
 * nuevo se crea otra implementación anotada con {@code @Component}; {@code CustomerValidator}
 * la recibe automáticamente sin cambiar su código (principio abierto/cerrado).</p>
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
    void validate(CustomerDocument customer);

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