package practikalia.grado;

import practikalia.common.ApiException;

import org.springframework.http.HttpStatus;

public class GradoException extends ApiException {

    private GradoException(HttpStatus status, String codigo, String mensaje) {
        super(status, codigo, mensaje);
    }

    public static GradoException noEncontrado() {
        return new GradoException(HttpStatus.NOT_FOUND, "GRADO_NO_ENCONTRADO", "El grado no existe");
    }
}
