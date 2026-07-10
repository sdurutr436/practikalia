package practikalia.interes;

import java.time.Instant;

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
