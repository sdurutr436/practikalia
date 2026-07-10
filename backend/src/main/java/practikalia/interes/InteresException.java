package practikalia.interes;

import practikalia.common.ApiException;

import org.springframework.http.HttpStatus;

public class InteresException extends ApiException {

    private InteresException(HttpStatus status, String codigo, String mensaje) {
        super(status, codigo, mensaje);
    }

    public static InteresException alumnoSinGrado() {
        return new InteresException(HttpStatus.BAD_REQUEST, "ALUMNO_SIN_GRADO",
                "El alumno no tiene grado y año establecidos en su perfil");
    }
}
