package com.nttdata.customer_service.service.validation.rules;

import static com.nttdata.customer_service.service.validation.rules.CustomerTypeRules.requireText;

import com.nttdata.bootcamp.customer.dto.CustomerProfile;
import com.nttdata.bootcamp.customer.dto.CustomerType;
import com.nttdata.bootcamp.customer.dto.DocumentType;
import com.nttdata.customer_service.document.CustomerDocument;
import com.nttdata.customer_service.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

/**
 * Reglas de un cliente PERSONAL.
 *
 * <ul>
 *   <li>Nombres y apellidos obligatorios.</li>
 *   <li>No puede registrarse con RUC.</li>
 *   <li>Perfiles permitidos: STANDARD o VIP (PYME es solo empresarial).</li>
 * </ul>
 */
@Component
public class PersonalCustomerRules implements CustomerTypeRules {

    @Override
    public CustomerType supports() {
        return CustomerType.PERSONAL;
    }

    @Override
    public void validate(CustomerDocument customer) {
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
