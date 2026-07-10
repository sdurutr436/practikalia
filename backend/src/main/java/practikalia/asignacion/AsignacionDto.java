package practikalia.asignacion;

import java.time.LocalDate;

public record AsignacionDto(
        Long id,
        Long alumnoId,
        String alumnoCorreo,
        Long empresaId,
        String empresaNombre,
        Long tutorCentroId,
        String tutorCentroCorreo,
        LocalDate fechaInicio,
        LocalDate fechaFin) {

    static AsignacionDto de(Asignacion asignacion) {
        return new AsignacionDto(
                asignacion.getId(),
                asignacion.getAlumno().getId(),
                asignacion.getAlumno().getCorreo(),
                asignacion.getEmpresa().getId(),
                asignacion.getEmpresa().getNombre(),
                asignacion.getTutorCentro().getId(),
                asignacion.getTutorCentro().getCorreo(),
                asignacion.getFechaInicio(),
                asignacion.getFechaFin());
    }
}
