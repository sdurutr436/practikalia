package practikalia.asignacion;

import practikalia.grado.GradoDto;

import java.time.LocalDate;

public record AsignacionDto(
        Long id,
        Long alumnoId,
        String alumnoCorreo,
        Long empresaId,
        String empresaNombre,
        Long tutorCentroId,
        String tutorCentroCorreo,
        GradoDto grado,
        Integer anio,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Boolean contratadoPosterior) {

    static AsignacionDto de(Asignacion asignacion) {
        return new AsignacionDto(
                asignacion.getId(),
                asignacion.getAlumno().getId(),
                asignacion.getAlumno().getCorreo(),
                asignacion.getEmpresa().getId(),
                asignacion.getEmpresa().getNombre(),
                asignacion.getTutorCentro().getId(),
                asignacion.getTutorCentro().getCorreo(),
                GradoDto.de(asignacion.getGrado()),
                asignacion.getAnio(),
                asignacion.getFechaInicio(),
                asignacion.getFechaFin(),
                asignacion.getContratadoPosterior());
    }
}
