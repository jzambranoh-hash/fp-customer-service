package com.nttdata.customer_service.application.mapper;

import com.nttdata.customer_service.domain.model.Customer;
import com.nttdata.customer_service.domain.model.CustomerProfile;
import com.nttdata.customer_service.domain.port.in.CreateCustomerCommand;
import com.nttdata.customer_service.domain.port.in.UpdateCustomerCommand;

/**
 * Convierte los comandos de los casos de uso al modelo de dominio {@link Customer}.
 *
 * <p>No tiene anotaciones de Spring: se registra como bean en {@code CustomerBeanConfig}.</p>
 */
public class CustomerCommandMapper {

    /**
     * Crea un cliente nuevo y activo. Si el comando no trae perfil, se asigna {@link CustomerProfile#STANDARD}.
     *
     * @param command datos del registro
     * @return cliente sin persistir
     */
    public Customer toCustomer(CreateCustomerCommand command) {
        return Customer.builder()
                .documentType(command.documentType())
                .documentNumber(command.documentNumber())
                .customerType(command.customerType())
                .profile(command.profile() != null ? command.profile() : CustomerProfile.STANDARD)
                .firstName(command.firstName())
                .lastName(command.lastName())
                .businessName(command.businessName())
                .email(command.email())
                .phone(command.phone())
                .address(command.address())
                .active(Boolean.TRUE)
                .build();
    }

    /**
     * Aplica los cambios sobre el cliente existente. Los campos opcionales en null conservan su valor;
     * email y phone siempre se reemplazan.
     *
     * @param customer cliente a modificar
     * @param command  cambios solicitados
     * @return el mismo cliente, modificado
     */
    public Customer applyChanges(Customer customer, UpdateCustomerCommand command) {
        if (command.profile() != null) {
            customer.setProfile(command.profile());
        }
        if (command.firstName() != null) {
            customer.setFirstName(command.firstName());
        }
        if (command.lastName() != null) {
            customer.setLastName(command.lastName());
        }
        if (command.businessName() != null) {
            customer.setBusinessName(command.businessName());
        }
        if (command.address() != null) {
            customer.setAddress(command.address());
        }
        customer.setEmail(command.email());
        customer.setPhone(command.phone());
        return customer;
    }
}
