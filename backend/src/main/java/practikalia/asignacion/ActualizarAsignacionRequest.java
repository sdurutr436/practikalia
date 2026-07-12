package practikalia.asignacion;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

/** Petición de cierre de una asignación. */
public record ActualizarAsignacionRequest(
        @NotNull LocalDate fechaFin,
        @Schema(description = "`null` = sin dato informado todavía (distinto de `false`); puede corregirse en una llamada posterior")
        Boolean contratadoPosterior) {
}
