package com.nttdata.customer_service.mapper;

import com.nttdata.bootcamp.customer.dto.CustomerProfile;
import com.nttdata.bootcamp.customer.dto.CustomerRequest;
import com.nttdata.bootcamp.customer.dto.CustomerResponse;
import com.nttdata.bootcamp.customer.dto.CustomerUpdateRequest;
import com.nttdata.customer_service.document.CustomerDocument;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Convierte entre los DTOs del contrato OpenAPI y la entidad {@link CustomerDocument}.
 */
@Component
public class CustomerMapper {

    /**
     * Crea un documento nuevo a partir de la petición de registro.
     * El perfil por defecto es STANDARD y el cliente queda activo.
     *
     * @param request datos del cliente
     * @return documento listo para guardar
     */
    public CustomerDocument toDocument(CustomerRequest request) {
        return CustomerDocument.builder()
                .documentType(request.getDocumentType())
                .documentNumber(trim(request.getDocumentNumber()))
                .customerType(request.getCustomerType())
                .profile(request.getProfile() != null ? request.getProfile() : CustomerProfile.STANDARD)
                .firstName(trim(request.getFirstName()))
                .lastName(trim(request.getLastName()))
                .businessName(trim(request.getBusinessName()))
                .email(trim(request.getEmail()))
                .phone(trim(request.getPhone()))
                .address(trim(request.getAddress()))
                .active(Boolean.TRUE)
                .build();
    }

    /**
     * Aplica los cambios de una actualización. Los campos opcionales en null
     * conservan su valor actual; el tipo de cliente y el documento no cambian.
     *
     * @param document documento existente
     * @param request  nuevos datos
     */
    public void updateDocument(CustomerDocument document, CustomerUpdateRequest request) {
        if (request.getProfile() != null) {
            document.setProfile(request.getProfile());
        }
        if (request.getFirstName() != null) {
            document.setFirstName(trim(request.getFirstName()));
        }
        if (request.getLastName() != null) {
            document.setLastName(trim(request.getLastName()));
        }
        if (request.getBusinessName() != null) {
            document.setBusinessName(trim(request.getBusinessName()));
        }
        if (request.getAddress() != null) {
            document.setAddress(trim(request.getAddress()));
        }
        document.setEmail(trim(request.getEmail()));
        document.setPhone(trim(request.getPhone()));
    }

    /**
     * Convierte el documento en la respuesta de la API.
     *
     * @param document documento de MongoDB
     * @return DTO de respuesta
     */
    public CustomerResponse toResponse(CustomerDocument document) {
        CustomerResponse response = new CustomerResponse();
        response.setId(document.getId());
        response.setDocumentType(document.getDocumentType());
        response.setDocumentNumber(document.getDocumentNumber());
        response.setCustomerType(document.getCustomerType());
        response.setProfile(document.getProfile());
        response.setFirstName(document.getFirstName());
        response.setLastName(document.getLastName());
        response.setBusinessName(document.getBusinessName());
        response.setEmail(document.getEmail());
        response.setPhone(document.getPhone());
        response.setAddress(document.getAddress());
        response.setActive(document.getActive());
        response.setCreatedAt(toOffsetDateTime(document.getCreatedAt()));
        response.setUpdatedAt(toOffsetDateTime(document.getUpdatedAt()));
        return response;
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }


}
