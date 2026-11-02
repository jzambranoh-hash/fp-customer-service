package com.nttdata.customer_service.infrastructure.adapter.in.web.handler;

import com.nttdata.bootcamp.customer.dto.ErrorResponse;
import com.nttdata.customer_service.domain.exception.BusinessRuleException;
import com.nttdata.customer_service.domain.exception.CustomerNotFoundException;
import com.nttdata.customer_service.domain.exception.DuplicateDocumentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Adaptador de entrada: convierte las excepciones en respuestas HTTP con el formato {@link ErrorResponse}.
 *
 * <p>Es el único lugar donde se registran los errores, con logs estructurados:</p>
 * <ul>
 *   <li>4xx (rechazos esperados): WARN, {@code event=request.rejected}, con un {@code reason} estable.</li>
 *   <li>5xx (fallos inesperados): ERROR, {@code event=request.failed}, con stack trace.</li>
 * </ul>
 * <p>Se ejecuta en el contexto de Reactor, así que estos logs llevan el traceId de la petición.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Validaciones de {@code @Valid} sobre el cuerpo de la petición. */
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleValidation(WebExchangeBindException ex, ServerWebExchange exchange) {
        List<String> details = ex.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return reject(HttpStatus.BAD_REQUEST, "Datos de entrada inválidos", "VALIDATION_ERROR", ex, exchange, details);
    }

    /** JSON mal formado o valor de enum inexistente (por ejemplo customerType = "OTRO"). */
    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ErrorResponse> handleInput(ServerWebInputException ex, ServerWebExchange exchange) {
        return reject(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido", "INVALID_BODY",
                ex, exchange, List.of());
    }

    /** Errores HTTP que ya traen su código: ruta inexistente (404), método no permitido (405), etc. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex, ServerWebExchange exchange) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return reject(status, ex.getReason() != null ? ex.getReason() : status.getReasonPhrase(),
                "HTTP_" + status.value(), ex, exchange, List.of());
    }

    /** Reglas de negocio incumplidas. */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex, ServerWebExchange exchange) {
        return reject(HttpStatus.BAD_REQUEST, ex.getMessage(), "BUSINESS_RULE", ex, exchange, List.of());
    }

    /** Cliente no encontrado. */
    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(CustomerNotFoundException ex, ServerWebExchange exchange) {
        return reject(HttpStatus.NOT_FOUND, ex.getMessage(), "CUSTOMER_NOT_FOUND", ex, exchange, List.of());
    }

    /** Número de documento repetido. */
    @ExceptionHandler(DuplicateDocumentException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateDocumentException ex, ServerWebExchange exchange) {
        return reject(HttpStatus.CONFLICT, ex.getMessage(), "DUPLICATE_DOCUMENT", ex, exchange, List.of());
    }

    /** Dos actualizaciones simultáneas del mismo cliente ({@code @Version}). */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockingFailureException ex, ServerWebExchange exchange) {
        return reject(HttpStatus.CONFLICT, "El cliente fue modificado por otra operación; vuelva a intentarlo",
                "CONCURRENT_UPDATE", ex, exchange, List.of());
    }

    /** Cualquier otro error: se registra completo y no se exponen detalles internos. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, ServerWebExchange exchange) {
        log.atError()
                .setMessage("Error inesperado")
                .addKeyValue("event", "request.failed")
                .addKeyValue("status", HttpStatus.INTERNAL_SERVER_ERROR.value())
                .addKeyValue("method", exchange.getRequest().getMethod().name())
                .addKeyValue("path", path(exchange))
                .addKeyValue("error", ex.getClass().getSimpleName())
                .setCause(ex)                                   // stack trace completo, solo en los 5xx
                .log();
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno", exchange, List.of());
    }

    /** Rechazo esperado (4xx): WARN, sin stack trace. */
    private ResponseEntity<ErrorResponse> reject(HttpStatus status, String message, String reason, Exception ex,
                                                 ServerWebExchange exchange, List<String> details) {
        log.atWarn()
                .setMessage("Petición rechazada")
                .addKeyValue("event", "request.rejected")
                .addKeyValue("status", status.value())
                .addKeyValue("reason", reason)
                .addKeyValue("method", exchange.getRequest().getMethod().name())
                .addKeyValue("path", path(exchange))
                .addKeyValue("error", ex.getClass().getSimpleName())
                .addKeyValue("detail", message)
                .log();
        return build(status, message, exchange, details);
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
