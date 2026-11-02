package com.nttdata.customer_service.domain.validation.rules;

import static com.nttdata.customer_service.domain.validation.rules.CustomerTypeRules.requireText;

import com.nttdata.customer_service.domain.exception.BusinessRuleException;
import com.nttdata.customer_service.domain.model.Customer;
import com.nttdata.customer_service.domain.model.CustomerProfile;
import com.nttdata.customer_service.domain.model.CustomerType;
import com.nttdata.customer_service.domain.model.DocumentType;

/**
 * Reglas de un cliente BUSINESS (empresa).
 *
 * <ul>
 *   <li>Razón social obligatoria.</li>
 *   <li>Solo puede registrarse con RUC.</li>
 *   <li>Perfiles permitidos: STANDARD o PYME (VIP es solo personal).</li>
 * </ul>
 */
public class BusinessCustomerRules implements CustomerTypeRules {

    @Override
    public CustomerType supports() {
        return CustomerType.BUSINESS;
    }

    @Override
    public void validate(Customer customer) {
        requireText(customer.getBusinessName(), "businessName es obligatorio para clientes BUSINESS");
        if (customer.getDocumentType() != DocumentType.RUC) {
            throw new BusinessRuleException("Un cliente BUSINESS debe registrarse con RUC");
        }
        if (customer.getProfile() == CustomerProfile.VIP) {
            throw new BusinessRuleException("El perfil VIP solo aplica a clientes PERSONAL");
        }
    }
}
