package practikalia.asignacion;

import jakarta.validation.constraints.NotNull;

/** Petición de la pantalla de asignaciones: a qué empresa va este alumno. */
public record AsignarEmpresaRequest(@NotNull Long empresaId) {
}
