package practikalia.usuario;

import io.swagger.v3.oas.annotations.media.Schema;

/** Respuesta de login. El token de sesión viaja en la cookie httpOnly, no en este cuerpo. */
public record LoginResponse(
        Rol rol,
        @Schema(description = "Flag independiente del rol: da acceso a los menús de administración, no es un rol propio")
        boolean esAdmin,
        @Schema(description = "Si es `true`, el token emitido solo autoriza `POST /api/auth/cambiar-contrasena` hasta que se cambie la contraseña")
        boolean debeCambiarContrasena) {
}
