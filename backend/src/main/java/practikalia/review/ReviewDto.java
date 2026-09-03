package practikalia.review;

import practikalia.usuario.Usuario;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Review de un alumno sobre una empresa, derivada de su asignación. */
public record ReviewDto(
        Long id,
        Long asignacionId,
        Long empresaId,
        String empresaNombre,
        String alumnoCorreo,
        @Schema(description = "Nombre y apellidos del alumno, o `null` si su cuenta todavía no los tiene "
                + "(las creadas por un profesor desde `POST /api/usuarios` no los piden)")
        String alumnoNombre,
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
                review.getAsignacion().getEmpresa().getNombre(),
                review.getAsignacion().getAlumno().getCorreo(),
                nombreCompleto(review.getAsignacion().getAlumno()),
                review.getAutor().getCorreo(),
                review.getContenido(),
                review.getCalificacion(),
                review.getEstado(),
                review.getModeradaPor() != null ? review.getModeradaPor().getCorreo() : null,
                review.getMotivoRechazo(),
                review.getFechaCreacion(),
                review.getFechaModeracion());
    }

    /** `null` en vez de cadena vacía: la pantalla decide si pinta la línea del nombre o solo el correo. */
    private static String nombreCompleto(Usuario alumno) {
        String nombre = Stream.of(alumno.getNombre(), alumno.getApellido1(), alumno.getApellido2())
                .filter(parte -> parte != null && !parte.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
        return nombre.isBlank() ? null : nombre;
    }
}
