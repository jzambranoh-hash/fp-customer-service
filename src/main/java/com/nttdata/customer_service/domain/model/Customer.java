package com.nttdata.customer_service.domain.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cliente del banco: modelo de dominio.
 *
 * <p>No conoce MongoDB, Spring ni los DTOs de la API. La persistencia lo convierte a
 * {@code CustomerDocument} y la API a {@code CustomerResponse} en sus propios adaptadores.</p>
 *
 * <p>Los clientes personales usan {@code firstName} y {@code lastName}; los empresariales, {@code businessName}.
 * {@code version}, {@code createdAt} y {@code updatedAt} los gestiona la persistencia, pero viajan en el modelo
 * para no perder el control de concurrencia ni la fecha de alta al actualizar.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    /** Identificador; null hasta que se guarda por primera vez. */
    private String id;

    /** Tipo de documento de identidad. */
    private DocumentType documentType;

    /** Número de documento; único en todo el sistema. */
    private String documentNumber;

    /** Personal o empresarial. */
    private CustomerType customerType;

    /** STANDARD, VIP (solo personal) o PYME (solo empresarial). */
    private CustomerProfile profile;

    /** Nombres (clientes personales). */
    private String firstName;

    /** Apellidos (clientes personales). */
    private String lastName;

    /** Razón social (clientes empresariales). */
    private String businessName;

    /** Correo electrónico de contacto. */
    private String email;

    /** Teléfono celular de contacto. */
    private String phone;

    /** Dirección. */
    private String address;

    /** false cuando el cliente fue eliminado (borrado lógico). */
    private Boolean active;

    /** Fecha de registro. */
    private LocalDateTime createdAt;

    /** Fecha de la última modificación. */
    private LocalDateTime updatedAt;

    /** Versión para control de concurrencia optimista. */
    private Long version;

    /**
     * Borrado lógico: el cliente deja de estar activo pero se conserva su historial.
     */
    public void deactivate() {
        this.active = Boolean.FALSE;
    }
}
