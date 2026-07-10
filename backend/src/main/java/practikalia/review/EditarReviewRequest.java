package practikalia.review;

import jakarta.validation.constraints.NotBlank;

public record EditarReviewRequest(@NotBlank String contenido, int calificacion) {
}
