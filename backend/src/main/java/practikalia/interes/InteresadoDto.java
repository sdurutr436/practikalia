package practikalia.interes;

import java.time.Instant;

/** Un alumno interesado en una empresa, con el grado/año snapshot de cuando marcó el interés. */
public record InteresadoDto(
        Long alumnoId,
        String alumnoCorreo,
        String gradoNombre,
        int anio,
        Instant fechaCreacion) {

    static InteresadoDto de(Interes interes) {
        return new InteresadoDto(
                interes.getAlumno().getId(),
                interes.getAlumno().getCorreo(),
                interes.getGrado().getNombre(),
                interes.getAnio(),
                interes.getFechaCreacion());
    }
}
