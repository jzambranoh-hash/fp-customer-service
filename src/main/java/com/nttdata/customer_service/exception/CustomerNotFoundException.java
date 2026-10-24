package com.nttdata.customer_service.exception;

/**
 * Se lanza cuando no existe un cliente activo con el criterio buscado. Se responde con HTTP 404.
 */
public class CustomerNotFoundException extends RuntimeException {

    /**
     * @param field campo usado en la búsqueda (id, documentNumber)
     * @param value valor buscado
     */
    public CustomerNotFoundException(String field, String value) {
        super(String.format("No se encontró un cliente con %s = %s", field, value));
    }

}
