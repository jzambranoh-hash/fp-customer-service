package com.nttdata.customer_service.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Reglas de la arquitectura hexagonal verificadas en cada build: si alguien importa Spring o MongoDB en el dominio,
 * esta prueba falla. Las dependencias solo apuntan hacia el centro: infraestructura → aplicación → dominio.
 */
@AnalyzeClasses(packages = "com.nttdata.customer_service", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    private static final String DOMAIN = "com.nttdata.customer_service.domain..";
    private static final String APPLICATION = "com.nttdata.customer_service.application..";
    private static final String INFRASTRUCTURE = "com.nttdata.customer_service.infrastructure..";
    private static final String API_DTOS = "com.nttdata.bootcamp.customer.dto..";

    /** El dominio no conoce frameworks, adaptadores ni el contrato de la API. */
    @ArchTest
    static final ArchRule domainIsIsolated = noClasses().that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "com.mongodb..", APPLICATION, INFRASTRUCTURE, API_DTOS);

    /** Los casos de uso solo usan el dominio: nada de Spring, MongoDB ni DTOs. */
    @ArchTest
    static final ArchRule applicationDependsOnlyOnDomain = noClasses().that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "com.mongodb..", INFRASTRUCTURE, API_DTOS);

    /** El adaptador web no accede a la persistencia: pasa siempre por los casos de uso. */
    @ArchTest
    static final ArchRule webDoesNotUsePersistence = noClasses()
            .that().resideInAPackage("com.nttdata.customer_service.infrastructure.adapter.in..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.nttdata.customer_service.infrastructure.adapter.out..");
}
