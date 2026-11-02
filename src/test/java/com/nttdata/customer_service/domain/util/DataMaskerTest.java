package com.nttdata.customer_service.domain.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.nttdata.customer_service.domain.exception.CustomerNotFoundException;
import com.nttdata.customer_service.domain.exception.DuplicateDocumentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El documento nunca debe aparecer completo en mensajes ni logs.
 */
class DataMaskerTest {

    @Test
    @DisplayName("deja visibles solo los últimos 4 caracteres")
    void masksDocument() {
        assertThat(DataMasker.document("97873773")).isEqualTo("****3773");
        assertThat(DataMasker.document("20123456789")).isEqualTo("****6789");
    }

    @Test
    @DisplayName("oculta por completo valores nulos o cortos")
    void masksShortValues() {
        assertThat(DataMasker.document(null)).isEqualTo("****");
        assertThat(DataMasker.document("1234")).isEqualTo("****");
    }

    @Test
    @DisplayName("las excepciones no exponen el documento completo")
    void exceptionsMaskDocument() {
        assertThat(new DuplicateDocumentException("97873773").getMessage())
                .contains("****3773").doesNotContain("97873773");
        assertThat(new CustomerNotFoundException(CustomerNotFoundException.DOCUMENT_NUMBER, "97873773").getMessage())
                .contains("****3773").doesNotContain("97873773");
        assertThat(new CustomerNotFoundException("id", "abc123").getMessage())
                .contains("abc123");
    }
}
