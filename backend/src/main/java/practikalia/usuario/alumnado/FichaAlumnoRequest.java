package practikalia.usuario.alumnado;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Ficha de un alumno, tal y como la pide el modal del listado. Sirve al alta y a la edición. */
public record FichaAlumnoRequest(
        @NotBlank String nombre,
        @NotBlank String apellido1,
        String apellido2,
        @Schema(description = "8 dígitos y letra de control. Cambiarlo **no** recalcula la contraseña: "
                + "a estas alturas el alumno puede haber entrado ya y puesto la suya.")
        @NotBlank String dni,
        @Schema(description = "Cambiarlo cambia con qué correo inicia sesión esa persona")
        @NotBlank @Email String correo,
        Long gradoId,
        Integer anio) {
}
