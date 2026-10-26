package com.nttdata.customer_service.exception;

/**
 * Se lanza cuando ya existe un cliente con el mismo número de documento. Se responde con HTTP 409.
 */
public class DuplicateDocumentException extends RuntimeException {

    /**
     * @param documentNumber número de documento repetido
     */
    public DuplicateDocumentException(String documentNumber) {
        super("Ya existe un cliente con el documento " + documentNumber);
    }

}
