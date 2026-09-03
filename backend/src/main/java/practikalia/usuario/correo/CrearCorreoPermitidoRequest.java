package practikalia.usuario.correo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CrearCorreoPermitidoRequest(@NotBlank @Email String correo) {
}
