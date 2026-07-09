package practikalia.usuario;

import practikalia.common.ApiException;

import org.springframework.http.HttpStatus;

public class UsuarioException extends ApiException {

    private UsuarioException(HttpStatus status, String codigo, String mensaje) {
        super(status, codigo, mensaje);
    }

    public static UsuarioException credencialesInvalidas() {
        return new UsuarioException(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS", "Correo o contraseña incorrectos");
    }

    public static UsuarioException correoNoPermitido() {
        return new UsuarioException(HttpStatus.FORBIDDEN, "CORREO_NO_PERMITIDO", "El correo no está permitido en esta instancia");
    }

    public static UsuarioException accesoDenegado() {
        return new UsuarioException(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", "No tienes permisos para esta acción");
    }

    public static UsuarioException correoYaRegistrado() {
        return new UsuarioException(HttpStatus.CONFLICT, "CORREO_YA_REGISTRADO", "El correo ya está registrado");
    }

    public static UsuarioException cuentaNoDisponible() {
        return new UsuarioException(HttpStatus.FORBIDDEN, "CUENTA_NO_DISPONIBLE", "La cuenta no está disponible");
    }

    public static UsuarioException demasiadosIntentos() {
        return new UsuarioException(HttpStatus.TOO_MANY_REQUESTS, "DEMASIADOS_INTENTOS", "Demasiados intentos, inténtalo más tarde");
    }

    public static UsuarioException contrasenaActualIncorrecta() {
        return new UsuarioException(HttpStatus.UNAUTHORIZED, "CONTRASENA_ACTUAL_INCORRECTA", "La contraseña actual no es correcta");
    }

    public static UsuarioException contrasenaNoCumplePolitica() {
        return new UsuarioException(HttpStatus.BAD_REQUEST, "CONTRASENA_NO_CUMPLE_POLITICA",
                "La contraseña no cumple la política requerida (mínimo 8 caracteres, mayúscula, minúscula, número y carácter especial)");
    }
}
