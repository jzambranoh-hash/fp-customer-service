package com.nttdata.customer_service.domain.port.in;

import com.nttdata.customer_service.domain.model.CustomerProfile;

/**
 * Datos editables de un cliente. El tipo de cliente y el documento no se pueden cambiar.
 *
 * <p>profile, firstName, lastName, businessName y address en null conservan su valor actual;
 * email y phone son obligatorios en el contrato y siempre se reemplazan.</p>
 *
 * @param profile      nuevo perfil
 * @param firstName    nuevos nombres
 * @param lastName     nuevos apellidos
 * @param businessName nueva razón social
 * @param email        correo
 * @param phone        celular
 * @param address      nueva dirección
 */
public record UpdateCustomerCommand(CustomerProfile profile, String firstName, String lastName,
                                    String businessName, String email, String phone, String address) {
}
