package practikalia.asignacion;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

/**
 * Petición de creación de una asignación. `gradoId`/`anio` se guardan como
 * snapshot del momento de crear la asignación: si el alumno cambia de
 * grado/año después, esta asignación no se actualiza.
 */
public record CrearAsignacionRequest(
        @NotNull Long alumnoId,
        @NotNull Long empresaId,
        @Schema(description = "Debe ser un usuario con rol `PROFESOR`")
        @NotNull Long tutorCentroId,
        @NotNull Long gradoId,
        @NotNull Integer anio,
        @NotNull LocalDate fechaInicio) {
}
