package com.nttdata.customer_service.config;

import com.nttdata.bootcamp.customer.dto.ErrorResponse;
import com.nttdata.customer_service.exception.BusinessRuleException;
import com.nttdata.customer_service.exception.CustomerNotFoundException;
import com.nttdata.customer_service.exception.DuplicateDocumentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Validaciones de {@code @Valid} sobre el cuerpo de la petición. */
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleValidation(WebExchangeBindException ex, ServerWebExchange exchange) {
        List<String> details = ex.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        log.warn("Validación fallida en {}: {}", path(exchange), details);
        return build(HttpStatus.BAD_REQUEST, "Datos de entrada inválidos", exchange, details);
    }

    /** JSON mal formado o valor de enum inexistente (por ejemplo customerType = "OTRO"). */
    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ErrorResponse> handleInput(ServerWebInputException ex, ServerWebExchange exchange) {
        log.warn("Petición inválida en {}: {}", path(exchange), ex.getReason());
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido", exchange, List.of());
    }

    /** Reglas de negocio incumplidas. */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex, ServerWebExchange exchange) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), exchange, List.of());
    }

    /** Cliente no encontrado. */
    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(CustomerNotFoundException ex, ServerWebExchange exchange) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), exchange, List.of());
    }

    /** Número de documento repetido. */
    @ExceptionHandler(DuplicateDocumentException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateDocumentException ex, ServerWebExchange exchange) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), exchange, List.of());
    }

    /** Dos actualizaciones simultáneas del mismo cliente ({@code @Version}). */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockingFailureException ex, ServerWebExchange exchange) {
        log.warn("Conflicto de concurrencia en {}", path(exchange));
        return build(HttpStatus.CONFLICT,
                "El cliente fue modificado por otra operación; vuelva a intentarlo", exchange, List.of());
    }

    /** Cualquier otro error: se registra completo y no se exponen detalles internos. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, ServerWebExchange exchange) {
        log.error("Error inesperado en {}", path(exchange), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno", exchange, List.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, ServerWebExchange exchange, List<String> details) {
        ErrorResponse body = new ErrorResponse();
        body.setTimestamp(OffsetDateTime.now());
        body.setStatus(status.value());
        body.setError(status.getReasonPhrase());
        body.setMessage(message);
        body.setPath(path(exchange));
        body.setDetails(details);
        return ResponseEntity.status(status).body(body);
    }

    private String path(ServerWebExchange exchange) {
        return exchange.getRequest().getPath().value();
    }

}
