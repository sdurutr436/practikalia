package practikalia.interes;

import practikalia.common.ApiException;

import org.springframework.http.HttpStatus;

/** Errores de expresión de interés de alumno por empresa. Empresa/acceso reutilizan {@code EmpresaException}/{@code UsuarioException}. */
public class InteresException extends ApiException {

    private InteresException(HttpStatus status, String codigo, String mensaje) {
        super(status, codigo, mensaje);
    }

    /** El alumno intenta marcar interés sin tener grado/año establecidos en su perfil (necesarios como snapshot). */
    public static InteresException alumnoSinGrado() {
        return new InteresException(HttpStatus.BAD_REQUEST, "ALUMNO_SIN_GRADO",
                "El alumno no tiene grado y año establecidos en su perfil");
    }
}
