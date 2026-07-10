package practikalia.asignacion;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record CrearAsignacionRequest(
        @NotNull Long alumnoId,
        @NotNull Long empresaId,
        @NotNull Long tutorCentroId,
        @NotNull Long gradoId,
        @NotNull Integer anio,
        @NotNull LocalDate fechaInicio) {
}
