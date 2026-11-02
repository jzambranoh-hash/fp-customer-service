package com.nttdata.customer_service.infrastructure.adapter.out.persistence.mapper;

import com.nttdata.customer_service.domain.model.Customer;
import com.nttdata.customer_service.infrastructure.adapter.out.persistence.document.CustomerDocument;
import org.springframework.stereotype.Component;

/**
 * Convierte entre el modelo de dominio {@link Customer} y el documento de MongoDB {@link CustomerDocument}.
 *
 * <p>Copia también id, fechas y versión: sin la versión, Spring Data trataría una actualización como un alta
 * nueva, y sin createdAt se perdería la fecha de registro.</p>
 */
@Component
public class CustomerPersistenceMapper {

    /**
     * @param customer modelo de dominio
     * @return documento para guardar
     */
    public CustomerDocument toDocument(Customer customer) {
        return CustomerDocument.builder()
                .id(customer.getId())
                .documentType(customer.getDocumentType())
                .documentNumber(customer.getDocumentNumber())
                .customerType(customer.getCustomerType())
                .profile(customer.getProfile())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .businessName(customer.getBusinessName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .active(customer.getActive())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .version(customer.getVersion())
                .build();
    }

    /**
     * @param document documento leído de MongoDB
     * @return modelo de dominio
     */
    public Customer toDomain(CustomerDocument document) {
        return Customer.builder()
                .id(document.getId())
                .documentType(document.getDocumentType())
                .documentNumber(document.getDocumentNumber())
                .customerType(document.getCustomerType())
                .profile(document.getProfile())
                .firstName(document.getFirstName())
                .lastName(document.getLastName())
                .businessName(document.getBusinessName())
                .email(document.getEmail())
                .phone(document.getPhone())
                .address(document.getAddress())
                .active(document.getActive())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .version(document.getVersion())
                .build();
    }
}
