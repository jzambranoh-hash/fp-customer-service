package com.nttdata.customer_service.domain.util;

/**
 * Enmascara datos personales antes de mostrarlos en mensajes o logs.
 */
public final class DataMasker {

    private static final int VISIBLE_DIGITS = 4;
    private static final String MASK = "****";

    private DataMasker() {
    }

    /**
     * @param value número de documento (DNI, RUC, etc.)
     * @return el documento con solo los últimos 4 caracteres visibles: {@code 97873773 → ****3773}
     */
    public static String document(String value) {
        if (value == null || value.length() <= VISIBLE_DIGITS) {
            return MASK;
        }
        return MASK + value.substring(value.length() - VISIBLE_DIGITS);
    }
}
