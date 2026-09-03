package practikalia.asignacion;

import practikalia.grado.GradoDto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/** Asignación de un alumno a una empresa, con el grado/año snapshot de cuando se creó. */
public record AsignacionDto(
        Long id,
        Long alumnoId,
        String alumnoCorreo,
        Long empresaId,
        String empresaNombre,
        @Schema(description = "El profesor que le tutoriza las prácticas desde el centro")
        Long tutorCentroId,
        String tutorCentroCorreo,
        @Schema(description = "Quién le tutoriza en la empresa; `null` si no se ha elegido a nadie")
        Long tutorEmpresaId,
        String tutorEmpresaNombre,
        GradoDto grado,
        Integer anio,
        LocalDate fechaInicio,
        @Schema(description = "`null` mientras la asignación sigue abierta")
        LocalDate fechaFin,
        @Schema(description = "`null` = sin dato informado todavía; solo tiene sentido una vez cerrada (`fechaFin` no nula)")
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
                asignacion.getTutorEmpresa() == null ? null : asignacion.getTutorEmpresa().getId(),
                asignacion.getTutorEmpresa() == null ? null : asignacion.getTutorEmpresa().getNombre(),
                GradoDto.de(asignacion.getGrado()),
                asignacion.getAnio(),
                asignacion.getFechaInicio(),
                asignacion.getFechaFin(),
                asignacion.getContratadoPosterior());
    }
}
