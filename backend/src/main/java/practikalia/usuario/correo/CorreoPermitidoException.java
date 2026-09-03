package practikalia.usuario.correo;

import practikalia.common.ApiException;

import org.springframework.http.HttpStatus;

/** Errores de la gestión de la whitelist de correos permitidos. */
public class CorreoPermitidoException extends ApiException {

    private CorreoPermitidoException(HttpStatus status, String codigo, String mensaje) {
        super(status, codigo, mensaje);
    }

    /** Ese correo ya está en la whitelist. */
    public static CorreoPermitidoException yaExiste() {
        return new CorreoPermitidoException(HttpStatus.CONFLICT, "CORREO_PERMITIDO_YA_EXISTE",
                "Ese correo ya está en la whitelist");
    }

    /** El id indicado no existe en la whitelist. */
    public static CorreoPermitidoException noEncontrado() {
        return new CorreoPermitidoException(HttpStatus.NOT_FOUND, "CORREO_PERMITIDO_NO_ENCONTRADO",
                "Ese correo no está en la whitelist");
    }
}
