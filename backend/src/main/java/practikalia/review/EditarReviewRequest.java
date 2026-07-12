package practikalia.review;

import jakarta.validation.constraints.NotBlank;

/** Petición de edición de una review propia. Si el autor es `ALUMNO`, reenvía la review a `PENDIENTE`. */
public record EditarReviewRequest(@NotBlank String contenido, int calificacion) {
}
