package practikalia.review;

import java.time.Instant;

public record ReviewDto(
        Long id,
        Long asignacionId,
        Long empresaId,
        String alumnoCorreo,
        String autorCorreo,
        String contenido,
        int calificacion,
        EstadoReview estado,
        String moderadaPorCorreo,
        String motivoRechazo,
        Instant fechaCreacion,
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
