package com.nttdata.customer_service.service.validation.rules;

import static com.nttdata.customer_service.service.validation.rules.CustomerTypeRules.requireText;

import com.nttdata.bootcamp.customer.dto.CustomerProfile;
import com.nttdata.bootcamp.customer.dto.CustomerType;
import com.nttdata.bootcamp.customer.dto.DocumentType;
import com.nttdata.customer_service.document.CustomerDocument;
import com.nttdata.customer_service.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

/**
 * Reglas de un cliente BUSINESS (empresa).
 *
 * <ul>
 *   <li>Razón social obligatoria.</li>
 *   <li>Solo puede registrarse con RUC.</li>
 *   <li>Perfiles permitidos: STANDARD o PYME (VIP es solo personal).</li>
 * </ul>
 */
@Component
public class BusinessCustomerRules implements CustomerTypeRules {

    @Override
    public CustomerType supports() {
        return CustomerType.BUSINESS;
    }

    @Override
    public void validate(CustomerDocument customer) {
        requireText(customer.getBusinessName(), "businessName es obligatorio para clientes BUSINESS");
        if (customer.getDocumentType() != DocumentType.RUC) {
            throw new BusinessRuleException("Un cliente BUSINESS debe registrarse con RUC");
        }
        if (customer.getProfile() == CustomerProfile.VIP) {
            throw new BusinessRuleException("El perfil VIP solo aplica a clientes PERSONAL");
        }
    }
}
