package practikalia.usuario;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String correo,
        @NotBlank String contrasena,
        String web) {
}
