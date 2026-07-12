package practikalia.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Petición de alta de usuario. Crear con rol `PROFESOR` requiere que quien crea sea admin. */
public record CrearUsuarioRequest(
        @NotBlank @Email String correo,
        @Schema(description = "Crear con `PROFESOR` requiere que quien hace la petición sea admin")
        @NotNull Rol rol) {
}
