package practikalia.common;

import org.springframework.http.HttpStatus;

/**
 * Base de toda excepción de negocio de la API. Cada feature define su propia
 * subclase con factories estáticos por caso (p. ej. {@code noEncontrado()}),
 * que fijan el {@link HttpStatus} y el código de error devueltos al cliente.
 * {@link GlobalExceptionHandler} la captura de forma genérica, sin conocer
 * las excepciones concretas de cada paquete.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    protected ApiException(HttpStatus status, String codigo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }
}
