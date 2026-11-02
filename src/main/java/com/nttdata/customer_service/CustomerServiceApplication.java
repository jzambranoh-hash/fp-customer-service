package com.nttdata.customer_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * customer-service con arquitectura hexagonal.
 *
 * <ul>
 *   <li>{@code domain}: modelo, reglas, excepciones y puertos. Sin Spring ni MongoDB.</li>
 *   <li>{@code application}: casos de uso; usan solo los puertos.</li>
 *   <li>{@code infrastructure}: adaptadores (REST, MongoDB) y configuración de Spring.</li>
 * </ul>
 */
@SpringBootApplication
public class CustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
}
