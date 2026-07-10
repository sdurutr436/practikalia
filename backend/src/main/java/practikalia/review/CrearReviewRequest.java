package practikalia.review;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CrearReviewRequest(
        @NotNull Long empresaId,
        @NotNull Long alumnoId,
        @NotBlank String contenido,
        int calificacion) {
}
