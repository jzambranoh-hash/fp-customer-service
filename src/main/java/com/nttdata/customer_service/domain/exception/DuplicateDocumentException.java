package com.nttdata.customer_service.domain.exception;

import com.nttdata.customer_service.domain.util.DataMasker;

/**
 * Se lanza cuando ya existe un cliente con el mismo número de documento. La API responde con HTTP 409.
 *
 * <p>El número se enmascara en el mensaje porque va a la respuesta y a los logs.</p>
 */
public class DuplicateDocumentException extends RuntimeException {

    /**
     * @param documentNumber número de documento repetido
     */
    public DuplicateDocumentException(String documentNumber) {
        super("Ya existe un cliente con el documento " + DataMasker.document(documentNumber));
    }
}
