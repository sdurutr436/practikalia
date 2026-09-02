package practikalia.usuario.alumnado;

import practikalia.common.ApiException;

import org.springframework.http.HttpStatus;

/** Errores propios de la importación masiva de alumnado. */
public class AlumnoException extends ApiException {

    private AlumnoException(HttpStatus status, String codigo, String mensaje) {
        super(status, codigo, mensaje);
    }

    /**
     * El CSV no se puede importar. El mensaje lleva la línea y el motivo
     * concretos porque es lo único con lo que quien lo subió puede corregirlo;
     * la importación es todo o nada, así que no se ha creado ninguna cuenta.
     */
    public static AlumnoException csvInvalido(String detalle) {
        return new AlumnoException(HttpStatus.BAD_REQUEST, "CSV_INVALIDO", detalle);
    }
}
