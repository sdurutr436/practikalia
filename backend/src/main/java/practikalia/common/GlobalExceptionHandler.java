package practikalia.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce toda excepción no capturada por un controller a un {@link ErrorBody}
 * JSON consistente. Cualquier {@link ApiException} de cualquier feature cae
 * aquí sin necesidad de un {@code @ExceptionHandler} por paquete; lo no
 * previsto se registra como {@code ERROR_INTERNO} (500) para no filtrar
 * detalles internos al cliente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Traduce cualquier {@link ApiException} de negocio a su status/código propios. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorBody> manejarApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ErrorBody(ex.getCodigo(), ex.getMessage()));
    }

    /** Fallo de validación {@code @Valid} de un DTO de request: 400 {@code CAMPO_INVALIDO}. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> manejarValidacion(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorBody("CAMPO_INVALIDO", "Campo requerido ausente o con formato inválido"));
    }

    /** Red de seguridad para cualquier excepción no prevista: 500 {@code ERROR_INTERNO}, sin detalle interno al cliente. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> manejarError(Exception ex) {
        log.error("Error interno no esperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorBody("ERROR_INTERNO", "Ha ocurrido un error inesperado"));
    }
}
