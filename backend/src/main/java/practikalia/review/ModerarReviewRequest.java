package practikalia.review;

import jakarta.validation.constraints.NotNull;

public record ModerarReviewRequest(
        @NotNull EstadoReview estado,
        String motivoRechazo) {
}
