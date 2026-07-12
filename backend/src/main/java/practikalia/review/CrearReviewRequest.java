package practikalia.review;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Petición de creación de una review, colgada de una asignación existente. */
public record CrearReviewRequest(
        @Schema(description = "Alumno, empresa, grado y año se derivan de esta asignación, no se envían sueltos")
        @NotNull Long asignacionId,
        @NotBlank String contenido,
        @Schema(description = "Debe estar dentro del rango de `GET /api/reviews/calificacion-config`")
        int calificacion) {
}
