package practikalia.asignacion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Petición de la pantalla de asignaciones: a qué empresa va este alumno y con qué tutores. */
public record AsignarEmpresaRequest(
        @NotNull Long empresaId,
        @Schema(description = "El profesor que le tutoriza las prácticas. Sin él se toma el tutor de su clase, "
                + "y si su clase no tiene ninguno, quien está guardando.")
        Long tutorCentroId,
        @Schema(description = "Quién le tutoriza en la empresa; tiene que ser uno de los tutores de esa empresa. "
                + "`null` para dejarle sin tutor de empresa.")
        Long tutorEmpresaId) {
}
