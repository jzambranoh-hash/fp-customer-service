package com.nttdata.customer_service.exception;

/**
 * Se lanza cuando una petición incumple una regla de negocio. Se responde con HTTP 400.
 */
public class BusinessRuleException extends RuntimeException {

    /**
     * @param message descripción de la regla incumplida
     */
    public BusinessRuleException(String message) {
        super(message);
    }

}
