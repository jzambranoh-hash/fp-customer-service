package com.nttdata.customer_service.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nttdata.customer_service.domain.exception.BusinessRuleException;
import com.nttdata.customer_service.domain.exception.CustomerNotFoundException;
import com.nttdata.customer_service.domain.exception.DuplicateDocumentException;
import com.nttdata.customer_service.domain.model.Customer;
import com.nttdata.customer_service.domain.model.CustomerProfile;
import com.nttdata.customer_service.domain.model.CustomerType;
import com.nttdata.customer_service.domain.model.DocumentType;
import com.nttdata.customer_service.domain.port.in.CreateCustomerCommand;
import com.nttdata.customer_service.domain.port.in.CreateCustomerUseCase;
import com.nttdata.customer_service.domain.port.in.DeleteCustomerUseCase;
import com.nttdata.customer_service.domain.port.in.FindCustomerUseCase;
import com.nttdata.customer_service.domain.port.in.UpdateCustomerCommand;
import com.nttdata.customer_service.domain.port.in.UpdateCustomerUseCase;
import com.nttdata.customer_service.infrastructure.adapter.in.web.controller.CustomerController;
import com.nttdata.customer_service.infrastructure.adapter.in.web.handler.GlobalExceptionHandler;
import com.nttdata.customer_service.infrastructure.adapter.in.web.mapper.CustomerWebMapper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;

/**
 * Pruebas del adaptador REST y del manejo de errores, sin levantar Spring Boot ni MongoDB.
 *
 * <p>Los casos de uso se simulan; el mapper y el {@link GlobalExceptionHandler} son los reales, así que se verifica
 * el JSON y el código HTTP que recibe el cliente.</p>
 */
@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    private static final String ID = "6710f1c2a3b4c5d6e7f80912";
    private static final String DNI = "45678912";

    private static final String VALID_BODY = """
            {
              "documentType": "DNI",
              "documentNumber": "45678912",
              "customerType": "PERSONAL",
              "firstName": "  Juan ",
              "lastName": "Pérez",
              "email": "juan.perez@mail.com",
              "phone": "987654321"
            }
            """;

    @Mock
    private CreateCustomerUseCase createUseCase;
    @Mock
    private FindCustomerUseCase findUseCase;
    @Mock
    private UpdateCustomerUseCase updateUseCase;
    @Mock
    private DeleteCustomerUseCase deleteUseCase;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        CustomerController controller = new CustomerController(createUseCase, findUseCase, updateUseCase,
                deleteUseCase, new CustomerWebMapper());
        client = WebTestClient.bindToController(controller)
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /customers devuelve la lista con los enums del contrato")
    void findAll() {
        when(findUseCase.findAll()).thenReturn(Flowable.just(customer()));

        client.get().uri("/customers").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo(ID)
                .jsonPath("$[0].customerType").isEqualTo("PERSONAL")
                .jsonPath("$[0].profile").isEqualTo("STANDARD")
                .jsonPath("$[0].createdAt").exists();
    }

    @Test
    @DisplayName("GET /customers/{id} devuelve 404 con el formato ErrorResponse")
    void findByIdNotFound() {
        when(findUseCase.findById(ID)).thenReturn(Single.error(new CustomerNotFoundException("id", ID)));

        client.get().uri("/customers/{id}", ID).exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.path").isEqualTo("/customers/" + ID);
    }

    @Test
    @DisplayName("GET /customers/document/{n} devuelve el cliente")
    void findByDocumentNumber() {
        when(findUseCase.findByDocumentNumber(DNI)).thenReturn(Single.just(customer()));

        client.get().uri("/customers/document/{n}", DNI).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.documentNumber").isEqualTo(DNI);
    }

    @Test
    @DisplayName("POST /customers responde 201 y normaliza los textos (trim)")
    void create() {
        when(createUseCase.create(any(CreateCustomerCommand.class))).thenReturn(Single.just(customer()));

        client.post().uri("/customers").contentType(MediaType.APPLICATION_JSON).bodyValue(VALID_BODY).exchange()
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.id").isEqualTo(ID);

        verify(createUseCase).create(eq(new CreateCustomerCommand(DocumentType.DNI, DNI, CustomerType.PERSONAL,
                CustomerProfile.STANDARD, "Juan", "Pérez", null, "juan.perez@mail.com", "987654321", null)));
    }

    @Test
    @DisplayName("POST /customers sin campos obligatorios responde 400 sin llamar al caso de uso")
    void createInvalid() {
        client.post().uri("/customers").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"documentType\":\"DNI\"}").exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.details").isNotEmpty();

        verify(createUseCase, never()).create(any());
    }

    @Test
    @DisplayName("POST /customers con un enum inexistente responde 400")
    void createWithUnknownEnum() {
        client.post().uri("/customers").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(VALID_BODY.replace("\"PERSONAL\"", "\"OTRO\"")).exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /customers con documento repetido responde 409 sin exponer el documento")
    void createDuplicate() {
        when(createUseCase.create(any(CreateCustomerCommand.class)))
                .thenReturn(Single.error(new DuplicateDocumentException(DNI)));

        client.post().uri("/customers").contentType(MediaType.APPLICATION_JSON).bodyValue(VALID_BODY).exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody().jsonPath("$.message").isEqualTo("Ya existe un cliente con el documento ****8912");
    }

    @Test
    @DisplayName("POST /customers con una regla incumplida responde 400 con el mensaje de la regla")
    void createBusinessRule() {
        when(createUseCase.create(any(CreateCustomerCommand.class)))
                .thenReturn(Single.error(new BusinessRuleException("El perfil PYME solo aplica a clientes BUSINESS")));

        client.post().uri("/customers").contentType(MediaType.APPLICATION_JSON).bodyValue(VALID_BODY).exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.message").isEqualTo("El perfil PYME solo aplica a clientes BUSINESS");
    }

    @Test
    @DisplayName("PUT /customers/{id} actualiza y convierte el perfil al dominio")
    void update() {
        when(updateUseCase.update(eq(ID), any(UpdateCustomerCommand.class))).thenReturn(Single.just(customer()));

        client.put().uri("/customers/{id}", ID).contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"profile\":\"VIP\",\"email\":\"nuevo@mail.com\",\"phone\":\"912345678\"}").exchange()
                .expectStatus().isOk();

        verify(updateUseCase).update(ID, new UpdateCustomerCommand(CustomerProfile.VIP, null, null, null,
                "nuevo@mail.com", "912345678", null));
    }

    @Test
    @DisplayName("PUT /customers/{id} con conflicto de versión responde 409")
    void updateConcurrent() {
        when(updateUseCase.update(eq(ID), any(UpdateCustomerCommand.class)))
                .thenReturn(Single.error(new OptimisticLockingFailureException("versión distinta")));

        client.put().uri("/customers/{id}", ID).contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"nuevo@mail.com\",\"phone\":\"912345678\"}").exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("DELETE /customers/{id} responde 204")
    void delete() {
        when(deleteUseCase.delete(ID)).thenReturn(Completable.complete());

        client.delete().uri("/customers/{id}", ID).exchange().expectStatus().isNoContent();
    }

    @Test
    @DisplayName("un error HTTP con código propio conserva ese código")
    void responseStatus() {
        when(findUseCase.findById(ID))
                .thenReturn(Single.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)));

        client.get().uri("/customers/{id}", ID).exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("un error inesperado responde 500 sin exponer detalles internos")
    void unexpectedError() {
        when(findUseCase.findById(ID)).thenReturn(Single.error(new IllegalStateException("detalle interno")));

        client.get().uri("/customers/{id}", ID).exchange()
                .expectStatus().is5xxServerError()
                .expectBody().jsonPath("$.message").isEqualTo("Ocurrió un error interno");
    }

    @Test
    void responseStatusWithoutReasonUsesStandardPhrase() {
        when(findUseCase.findById(ID))
                .thenReturn(Single.error(new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED)));

        client.get().uri("/customers/{id}", ID)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
                .expectBody()
                .jsonPath("$.status").isEqualTo(405)
                .jsonPath("$.message").isEqualTo("Method Not Allowed");
    }

    @Test
    void responseStatusWithReasonUsesReason() {
        when(findUseCase.findById(ID))
                .thenReturn(Single.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurso no disponible")));

        client.get().uri("/customers/{id}", ID)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").isEqualTo("Recurso no disponible");
    }

    private static Customer customer() {
        return Customer.builder()
                .id(ID)
                .documentType(DocumentType.DNI)
                .documentNumber(DNI)
                .customerType(CustomerType.PERSONAL)
                .profile(CustomerProfile.STANDARD)
                .firstName("Juan")
                .lastName("Pérez")
                .email("juan.perez@mail.com")
                .phone("987654321")
                .active(Boolean.TRUE)
                .createdAt(LocalDateTime.of(2026, 10, 1, 9, 0))
                .build();
    }
}
