package practikalia.review;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

/** Petición de moderación de una review pendiente. */
public record ModerarReviewRequest(
        @Schema(description = "Solo `APROBADA` o `RECHAZADA`; la review debe estar en `PENDIENTE` para poder moderarse")
        @NotNull EstadoReview estado,
        @Schema(description = "Obligatorio si `estado` es `RECHAZADA`; ignorado si es `APROBADA`")
        String motivoRechazo) {
}
