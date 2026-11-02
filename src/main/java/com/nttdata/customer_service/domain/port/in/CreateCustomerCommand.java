package com.nttdata.customer_service.domain.port.in;

import com.nttdata.customer_service.domain.model.CustomerProfile;
import com.nttdata.customer_service.domain.model.CustomerType;
import com.nttdata.customer_service.domain.model.DocumentType;

/**
 * Datos para registrar un cliente. Lo arma el adaptador web a partir del DTO de la API.
 *
 * @param documentType   tipo de documento
 * @param documentNumber número de documento
 * @param customerType   personal o empresarial
 * @param profile        perfil; null = STANDARD
 * @param firstName      nombres (personal)
 * @param lastName       apellidos (personal)
 * @param businessName   razón social (empresarial)
 * @param email          correo
 * @param phone          celular
 * @param address        dirección
 */
public record CreateCustomerCommand(DocumentType documentType, String documentNumber, CustomerType customerType,
                                    CustomerProfile profile, String firstName, String lastName,
                                    String businessName, String email, String phone, String address) {
}
