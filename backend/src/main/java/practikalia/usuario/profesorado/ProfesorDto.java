package practikalia.usuario.profesorado;

import practikalia.grado.GradoDto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Fila del listado de profesorado: lo que pinta una tarjeta. */
public record ProfesorDto(
        Long id,
        String nombre,
        String apellido1,
        String apellido2,
        String dni,
        String correo,
        @Schema(description = "Administrador del centro. Viaja sobre el rol `PROFESOR`, no es un rol aparte")
        boolean esAdmin,
        @Schema(description = "La clase que tutoriza, o `null` si no tutoriza ninguna")
        GradoDto clase,
        @Schema(description = "Cuántos alumnos tiene como tutor de prácticas (asignaciones abiertas)")
        int alumnosPractica) {
}
