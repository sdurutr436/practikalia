package practikalia.usuario;

import io.swagger.v3.oas.annotations.media.Schema;

/** Confirmación de alta de usuario. */
public record CrearUsuarioResponse(
        Long id, String correo, Rol rol,
        @Schema(description = "Contraseña generada en claro, visible solo en esta respuesta. El usuario deberá cambiarla en su primer login")
        String contrasenaTemporal) {
}
