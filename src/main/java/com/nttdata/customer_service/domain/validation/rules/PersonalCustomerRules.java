package com.nttdata.customer_service.domain.validation.rules;

import static com.nttdata.customer_service.domain.validation.rules.CustomerTypeRules.requireText;

import com.nttdata.customer_service.domain.exception.BusinessRuleException;
import com.nttdata.customer_service.domain.model.Customer;
import com.nttdata.customer_service.domain.model.CustomerProfile;
import com.nttdata.customer_service.domain.model.CustomerType;
import com.nttdata.customer_service.domain.model.DocumentType;

/**
 * Reglas de un cliente PERSONAL.
 *
 * <ul>
 *   <li>Nombres y apellidos obligatorios.</li>
 *   <li>No puede registrarse con RUC.</li>
 *   <li>Perfiles permitidos: STANDARD o VIP (PYME es solo empresarial).</li>
 * </ul>
 */
public class PersonalCustomerRules implements CustomerTypeRules {

    @Override
    public CustomerType supports() {
        return CustomerType.PERSONAL;
    }

    @Override
    public void validate(Customer customer) {
        requireText(customer.getFirstName(), "firstName es obligatorio para clientes PERSONAL");
        requireText(customer.getLastName(), "lastName es obligatorio para clientes PERSONAL");
        if (customer.getDocumentType() == DocumentType.RUC) {
            throw new BusinessRuleException("Un cliente PERSONAL no puede registrarse con RUC");
        }
        if (customer.getProfile() == CustomerProfile.PYME) {
            throw new BusinessRuleException("El perfil PYME solo aplica a clientes BUSINESS");
        }
    }
}
