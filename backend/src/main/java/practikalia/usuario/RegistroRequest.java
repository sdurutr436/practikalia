package practikalia.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Petición de auto-registro de alumnado. La cuenta nace inactiva, a la espera de que un admin la apruebe. */
public record RegistroRequest(
        @NotBlank String nombre,
        @NotBlank String apellido1,
        @Schema(description = "Vacío o ausente si no se tiene") String apellido2,
        @Schema(description = "8 dígitos y letra de control. Se comprueba el formato, no que la persona esté matriculada")
        @NotBlank String dni,
        @NotNull Long gradoId,
        @NotBlank @Email String correo,
        @Schema(description = "Campo honeypot: debe llegar vacío. Un formulario real no lo rellena "
                + "(oculto vía CSS); si viene relleno se trata como intento de bot y se rechaza sin crear la cuenta.")
        String web) {
}
