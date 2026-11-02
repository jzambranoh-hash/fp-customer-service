package com.nttdata.customer_service.domain.exception;

import com.nttdata.customer_service.domain.util.DataMasker;

/**
 * Se lanza cuando no existe un cliente activo con el criterio buscado. La API responde con HTTP 404.
 *
 * <p>Si la búsqueda fue por documento, el número se enmascara en el mensaje (va a la respuesta y a los logs).</p>
 */
public class CustomerNotFoundException extends RuntimeException {

    /** Campo de búsqueda cuyo valor se enmascara. */
    public static final String DOCUMENT_NUMBER = "documentNumber";

    /**
     * @param field campo usado en la búsqueda (id, documentNumber)
     * @param value valor buscado
     */
    public CustomerNotFoundException(String field, String value) {
        super(String.format("No se encontró un cliente con %s = %s", field,
                DOCUMENT_NUMBER.equals(field) ? DataMasker.document(value) : value));
    }
}
