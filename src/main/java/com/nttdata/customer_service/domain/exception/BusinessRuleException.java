package com.nttdata.customer_service.domain.exception;

/**
 * Se lanza cuando una petición incumple una regla de negocio. La API responde con HTTP 400.
 */
public class BusinessRuleException extends RuntimeException {

    /**
     * @param message descripción de la regla incumplida
     */
    public BusinessRuleException(String message) {
        super(message);
    }
}
