package practikalia.usuario;

import jakarta.validation.constraints.NotBlank;

/** Petición de cambio de contraseña propia. */
public record CambiarContrasenaRequest(
        @NotBlank String contrasenaActual,
        @NotBlank String contrasenaNueva) {
}
