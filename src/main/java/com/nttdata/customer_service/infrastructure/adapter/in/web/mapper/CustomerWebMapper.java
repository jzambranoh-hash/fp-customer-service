package com.nttdata.customer_service.infrastructure.adapter.in.web.mapper;

import com.nttdata.bootcamp.customer.dto.CustomerRequest;
import com.nttdata.bootcamp.customer.dto.CustomerResponse;
import com.nttdata.bootcamp.customer.dto.CustomerUpdateRequest;
import com.nttdata.customer_service.domain.model.Customer;
import com.nttdata.customer_service.domain.model.CustomerProfile;
import com.nttdata.customer_service.domain.model.CustomerType;
import com.nttdata.customer_service.domain.model.DocumentType;
import com.nttdata.customer_service.domain.port.in.CreateCustomerCommand;
import com.nttdata.customer_service.domain.port.in.UpdateCustomerCommand;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/**
 * Convierte entre los DTOs del contrato OpenAPI y el dominio.
 *
 * <p>Los enums del contrato y los del dominio tienen los mismos nombres pero son tipos distintos:
 * se convierten por nombre. También normaliza los textos de entrada (trim).</p>
 */
@Component
public class CustomerWebMapper {

    /**
     * @param request DTO de registro
     * @return comando del caso de uso
     */
    public CreateCustomerCommand toCreateCommand(CustomerRequest request) {
        return new CreateCustomerCommand(
                convert(request.getDocumentType(), DocumentType.class),
                trim(request.getDocumentNumber()),
                convert(request.getCustomerType(), CustomerType.class),
                convert(request.getProfile(), CustomerProfile.class),
                trim(request.getFirstName()),
                trim(request.getLastName()),
                trim(request.getBusinessName()),
                trim(request.getEmail()),
                trim(request.getPhone()),
                trim(request.getAddress()));
    }

    /**
     * @param request DTO de actualización
     * @return comando del caso de uso
     */
    public UpdateCustomerCommand toUpdateCommand(CustomerUpdateRequest request) {
        return new UpdateCustomerCommand(
                convert(request.getProfile(), CustomerProfile.class),
                trim(request.getFirstName()),
                trim(request.getLastName()),
                trim(request.getBusinessName()),
                trim(request.getEmail()),
                trim(request.getPhone()),
                trim(request.getAddress()));
    }

    /**
     * @param customer modelo de dominio
     * @return DTO de respuesta
     */
    public CustomerResponse toResponse(Customer customer) {
        CustomerResponse response = new CustomerResponse();
        response.setId(customer.getId());
        response.setDocumentType(convert(customer.getDocumentType(),
                com.nttdata.bootcamp.customer.dto.DocumentType.class));
        response.setDocumentNumber(customer.getDocumentNumber());
        response.setCustomerType(convert(customer.getCustomerType(),
                com.nttdata.bootcamp.customer.dto.CustomerType.class));
        response.setProfile(convert(customer.getProfile(),
                com.nttdata.bootcamp.customer.dto.CustomerProfile.class));
        response.setFirstName(customer.getFirstName());
        response.setLastName(customer.getLastName());
        response.setBusinessName(customer.getBusinessName());
        response.setEmail(customer.getEmail());
        response.setPhone(customer.getPhone());
        response.setAddress(customer.getAddress());
        response.setActive(customer.getActive());
        response.setCreatedAt(toOffsetDateTime(customer.getCreatedAt()));
        response.setUpdatedAt(toOffsetDateTime(customer.getUpdatedAt()));
        return response;
    }

    /** Convierte un enum en otro con el mismo nombre de constante (contrato ↔ dominio). */
    private static <E extends Enum<E>> E convert(Enum<?> source, Class<E> target) {
        return source == null ? null : Enum.valueOf(target, source.name());
    }

    private static OffsetDateTime toOffsetDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
