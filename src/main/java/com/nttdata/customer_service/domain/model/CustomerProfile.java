package com.nttdata.customer_service.domain.model;

/**
 * Perfil del cliente. VIP solo aplica a clientes PERSONAL y PYME solo a clientes BUSINESS.
 */
public enum CustomerProfile {
    /** Perfil por defecto. */
    STANDARD,
    /** Cliente personal preferente (Parte II). */
    VIP,
    /** Pequeña o mediana empresa (Parte II). */
    PYME
}
