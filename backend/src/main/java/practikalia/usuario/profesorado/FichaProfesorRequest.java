package practikalia.usuario.profesorado;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Ficha de un profesor, tal y como la pide el modal del listado. Sirve al alta y a la edición. */
public record FichaProfesorRequest(
        @NotBlank String nombre,
        @NotBlank String apellido1,
        String apellido2,
        @Schema(description = "8 dígitos y letra de control. Es su contraseña inicial (sin la letra); "
                + "cambiarlo **no** la recalcula.")
        @NotBlank String dni,
        @Schema(description = "Cambiarlo cambia con qué correo inicia sesión esa persona")
        @NotBlank @Email String correo,
        @Schema(description = "La clase de la que es tutor. Se la quita a quien la tuviera, y él suelta la suya "
                + "anterior: un tutor por clase y una clase por tutor. `null` para no tutorizar ninguna.")
        Long gradoId,
        @Schema(description = "Administrador del centro. Nunca puede quedarse el centro sin ninguno.")
        Boolean esAdmin,
        @Schema(description = "Alumnos que pasan a tenerle como tutor de prácticas: se le pone como tutor de la "
                + "asignación abierta de cada uno. Solo añade — quitarle un alumno es dárselo a otro tutor.")
        List<Long> alumnosPractica) {
}
