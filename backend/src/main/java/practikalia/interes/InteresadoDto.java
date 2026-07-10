package practikalia.interes;

import java.time.Instant;

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
