package com.nttdata.customer_service.document;

import com.nttdata.bootcamp.customer.dto.CustomerProfile;
import com.nttdata.bootcamp.customer.dto.CustomerType;
import com.nttdata.bootcamp.customer.dto.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Documento de MongoDB que representa a un cliente del banco.
 *
 * <p>Un cliente puede ser {@link CustomerType#PERSONAL} o {@link CustomerType#BUSINESS}.
 * Los clientes personales usan {@code firstName} y {@code lastName};
 * los empresariales usan {@code businessName}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "customers")
public class CustomerDocument {

    /** Identificador generado por MongoDB. */
    @Id
    private String id;

    /** Tipo de documento de identidad (DNI, CEX, PASSPORT o RUC). */
    private DocumentType documentType;

    /** Número de documento; único en todo el sistema. */
    @Indexed(unique = true)
    private String documentNumber;

    /** Tipo de cliente: personal o empresarial. */
    private CustomerType customerType;

    /** Perfil del cliente: STANDARD, VIP (solo personal) o PYME (solo empresarial). */
    private CustomerProfile profile;

    /** Nombres; obligatorio para clientes personales. */
    private String firstName;

    /** Apellidos; obligatorio para clientes personales. */
    private String lastName;

    /** Razón social; obligatoria para clientes empresariales. */
    private String businessName;

    /** Correo electrónico de contacto. */
    private String email;

    /** Teléfono celular de contacto. */
    private String phone;

    /** Dirección del cliente. */
    private String address;

    /** Indica si el cliente está activo; false cuando se elimina (borrado lógico). */
    private Boolean active;

    /** Fecha de registro; la asigna Spring Data automáticamente. */
    @CreatedDate
    private LocalDateTime createdAt;

    /** Fecha de la última modificación; la asigna Spring Data automáticamente. */
    @LastModifiedDate
    private LocalDateTime updatedAt;

    /** Versión del documento para control de concurrencia optimista. */
    @Version
    private Long version;

}
