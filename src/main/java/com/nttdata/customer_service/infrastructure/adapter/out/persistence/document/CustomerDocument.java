package com.nttdata.customer_service.infrastructure.adapter.out.persistence.document;

import com.nttdata.customer_service.domain.model.CustomerProfile;
import com.nttdata.customer_service.domain.model.CustomerType;
import com.nttdata.customer_service.domain.model.DocumentType;
import java.time.LocalDateTime;
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

/**
 * Representación del cliente en MongoDB (colección {@code customers}). Es un detalle del adaptador de
 * persistencia: el dominio trabaja con {@code Customer}.
 *
 * <p>Los enums se guardan por nombre, igual que antes de la migración: los datos existentes siguen siendo
 * compatibles.</p>
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

    /** Tipo de documento de identidad. */
    private DocumentType documentType;

    /** Número de documento; único en toda la colección. */
    @Indexed(unique = true)
    private String documentNumber;

    /** Personal o empresarial. */
    private CustomerType customerType;

    /** STANDARD, VIP o PYME. */
    private CustomerProfile profile;

    /** Nombres. */
    private String firstName;

    /** Apellidos. */
    private String lastName;

    /** Razón social. */
    private String businessName;

    /** Correo. */
    private String email;

    /** Celular. */
    private String phone;

    /** Dirección. */
    private String address;

    /** false = borrado lógico. */
    private Boolean active;

    /** Fecha de registro; la asigna Spring Data. */
    @CreatedDate
    private LocalDateTime createdAt;

    /** Fecha de la última modificación; la asigna Spring Data. */
    @LastModifiedDate
    private LocalDateTime updatedAt;

    /** Versión para control de concurrencia optimista. */
    @Version
    private Long version;
}
