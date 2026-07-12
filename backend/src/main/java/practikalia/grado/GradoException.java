package practikalia.grado;

import practikalia.common.ApiException;

import org.springframework.http.HttpStatus;

/** Errores del catálogo de grados. */
public class GradoException extends ApiException {

    private GradoException(HttpStatus status, String codigo, String mensaje) {
        super(status, codigo, mensaje);
    }

    /** El `gradoId` indicado al actualizar el perfil de un usuario no existe. */
    public static GradoException noEncontrado() {
        return new GradoException(HttpStatus.NOT_FOUND, "GRADO_NO_ENCONTRADO", "El grado no existe");
    }
}
