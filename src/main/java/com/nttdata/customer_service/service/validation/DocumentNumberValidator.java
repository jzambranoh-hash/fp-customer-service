package com.nttdata.customer_service.service.validation;

import com.nttdata.bootcamp.customer.dto.DocumentType;
import com.nttdata.customer_service.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Valida el formato del número según el tipo de documento.
 *
 * <p>Los formatos están en un mapa en lugar de una cadena de {@code if}: agregar un tipo de documento
 * es agregar una entrada. Los tipos sin formato definido (CEX, PASSPORT) no se validan.</p>
 */
@Component
public class DocumentNumberValidator {

    private final Map<DocumentType, DocumentFormat> formats = new EnumMap<>(DocumentType.class);

    /**
     * Registra los formatos conocidos.
     */
    public DocumentNumberValidator() {
        formats.put(DocumentType.DNI, new DocumentFormat(Pattern.compile("^\\d{8}$"), "El DNI debe tener 8 dígitos"));
        formats.put(DocumentType.RUC, new DocumentFormat(Pattern.compile("^\\d{11}$"), "El RUC debe tener 11 dígitos"));
    }

    /**
     * @param type   tipo de documento
     * @param number número de documento
     * @throws BusinessRuleException si el número no cumple el formato del tipo
     */
    public void validate(DocumentType type, String number) {
        DocumentFormat format = formats.get(type);
        if (format != null && (number == null || !format.pattern().matcher(number).matches())) {
            throw new BusinessRuleException(format.message());
        }
    }

    /** Formato esperado de un tipo de documento y el mensaje si no se cumple. */
    private record DocumentFormat(Pattern pattern, String message) {
    }
}
