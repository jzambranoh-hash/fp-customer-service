package com.nttdata.customer_service.domain.model;

/**
 * Tipo de documento de identidad. RUC solo aplica a clientes BUSINESS.
 */
public enum DocumentType {
    /** Documento Nacional de Identidad (8 dígitos). */
    DNI,
    /** Carné de extranjería. */
    CEX,
    /** Pasaporte. */
    PASSPORT,
    /** Registro Único de Contribuyentes (11 dígitos). */
    RUC
}
