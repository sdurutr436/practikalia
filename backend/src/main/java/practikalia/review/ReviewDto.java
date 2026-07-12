package practikalia.review;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/** Review de un alumno sobre una empresa, derivada de su asignación. */
public record ReviewDto(
        Long id,
        Long asignacionId,
        Long empresaId,
        String alumnoCorreo,
        @Schema(description = "Quien escribió la review: el propio alumno, o su tutor de centro")
        String autorCorreo,
        String contenido,
        int calificacion,
        EstadoReview estado,
        @Schema(description = "`null` mientras sigue `PENDIENTE`")
        String moderadaPorCorreo,
        @Schema(description = "Solo informado si `estado` es `RECHAZADA`")
        String motivoRechazo,
        Instant fechaCreacion,
        @Schema(description = "`null` mientras sigue `PENDIENTE`")
        Instant fechaModeracion) {

    static ReviewDto de(Review review) {
        return new ReviewDto(
                review.getId(),
                review.getAsignacion().getId(),
                review.getAsignacion().getEmpresa().getId(),
                review.getAsignacion().getAlumno().getCorreo(),
                review.getAutor().getCorreo(),
                review.getContenido(),
                review.getCalificacion(),
                review.getEstado(),
                review.getModeradaPor() != null ? review.getModeradaPor().getCorreo() : null,
                review.getMotivoRechazo(),
                review.getFechaCreacion(),
                review.getFechaModeracion());
    }
}
