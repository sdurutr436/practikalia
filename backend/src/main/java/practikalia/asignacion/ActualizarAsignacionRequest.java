package practikalia.asignacion;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record ActualizarAsignacionRequest(@NotNull LocalDate fechaFin, Boolean contratadoPosterior) {
}
