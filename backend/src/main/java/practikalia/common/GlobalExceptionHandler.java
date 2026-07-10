package practikalia.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorBody> manejarApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ErrorBody(ex.getCodigo(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> manejarValidacion(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorBody("CAMPO_INVALIDO", "Campo requerido ausente o con formato inválido"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> manejarError(Exception ex) {
        log.error("Error interno no esperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorBody("ERROR_INTERNO", "Ha ocurrido un error inesperado"));
    }
}
