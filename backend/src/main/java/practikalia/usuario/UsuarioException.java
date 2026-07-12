package practikalia.usuario;

import practikalia.common.ApiException;

import org.springframework.http.HttpStatus;

/**
 * Errores de autenticación, alta de cuentas y perfil de usuario (grado/año,
 * etiquetas de interés).
 */
public class UsuarioException extends ApiException {

    private UsuarioException(HttpStatus status, String codigo, String mensaje) {
        super(status, codigo, mensaje);
    }

    /** Correo inexistente, contraseña incorrecta, o honeypot de bot relleno en el login. */
    public static UsuarioException credencialesInvalidas() {
        return new UsuarioException(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS", "Correo o contraseña incorrectos");
    }

    /** El dominio/correo no está en la whitelist de la instancia (ni en `allowed.domains` ni en `CorreoPermitido`). */
    public static UsuarioException correoNoPermitido() {
        return new UsuarioException(HttpStatus.FORBIDDEN, "CORREO_NO_PERMITIDO", "El correo no está permitido en esta instancia");
    }

    /** Acción sobre un recurso ajeno sin ser profesor/admin, o alta de `PROFESOR` por quien no es admin. */
    public static UsuarioException accesoDenegado() {
        return new UsuarioException(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", "No tienes permisos para esta acción");
    }

    /** El id de usuario indicado no existe. */
    public static UsuarioException noEncontrado() {
        return new UsuarioException(HttpStatus.NOT_FOUND, "USUARIO_NO_ENCONTRADO", "El usuario no existe");
    }

    /** Alguno de los `etiquetaIds` indicados al reemplazar etiquetas de interés no existe. */
    public static UsuarioException etiquetaNoEncontrada() {
        return new UsuarioException(HttpStatus.NOT_FOUND, "ETIQUETA_NO_ENCONTRADA",
                "Alguna de las etiquetas indicadas no existe");
    }

    /** Ya existe una cuenta con ese correo. */
    public static UsuarioException correoYaRegistrado() {
        return new UsuarioException(HttpStatus.CONFLICT, "CORREO_YA_REGISTRADO", "El correo ya está registrado");
    }

    /** La cuenta existe pero está desactivada, o su correo dejó de estar en la whitelist. */
    public static UsuarioException cuentaNoDisponible() {
        return new UsuarioException(HttpStatus.FORBIDDEN, "CUENTA_NO_DISPONIBLE", "La cuenta no está disponible");
    }

    /** Bloqueo temporal (15 min) tras 5 intentos de login fallidos consecutivos. */
    public static UsuarioException demasiadosIntentos() {
        return new UsuarioException(HttpStatus.TOO_MANY_REQUESTS, "DEMASIADOS_INTENTOS", "Demasiados intentos, inténtalo más tarde");
    }

    /** La contraseña actual indicada al cambiarla no coincide con la almacenada. */
    public static UsuarioException contrasenaActualIncorrecta() {
        return new UsuarioException(HttpStatus.UNAUTHORIZED, "CONTRASENA_ACTUAL_INCORRECTA", "La contraseña actual no es correcta");
    }

    /** La contraseña nueva no cumple la política (mínimo 8 caracteres, mayúscula, minúscula, número y carácter especial). */
    public static UsuarioException contrasenaNoCumplePolitica() {
        return new UsuarioException(HttpStatus.BAD_REQUEST, "CONTRASENA_NO_CUMPLE_POLITICA",
                "La contraseña no cumple la política requerida (mínimo 8 caracteres, mayúscula, minúscula, número y carácter especial)");
    }
}
