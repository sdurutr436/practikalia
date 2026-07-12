package practikalia.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Petición de login. */
public record LoginRequest(
        @NotBlank String correo,
        @NotBlank String contrasena,
        @Schema(description = "Campo honeypot: debe llegar vacío. Un formulario real no lo rellena "
                + "(oculto vía CSS); si viene relleno se trata como intento de bot y se rechaza como credenciales inválidas.")
        String web) {
}
