package practikalia.interes;

import java.time.Instant;

/** Interés de un alumno en una empresa, con el grado/año snapshot de cuando se marcó. */
public record InteresDto(
        Long empresaId,
        String empresaNombre,
        String gradoNombre,
        int anio,
        Instant fechaCreacion) {

    static InteresDto de(Interes interes) {
        return new InteresDto(
                interes.getEmpresa().getId(),
                interes.getEmpresa().getNombre(),
                interes.getGrado().getNombre(),
                interes.getAnio(),
                interes.getFechaCreacion());
    }
}
