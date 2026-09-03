package practikalia.usuario.alumnado;

import practikalia.grado.GradoDto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Fila del listado de alumnado: lo que pinta una tarjeta, sin datos sensibles. */
public record AlumnoDto(
        Long id,
        String nombre,
        String apellido1,
        String apellido2,
        String dni,
        String correo,
        @Schema(description = "`null` si todavía no se le ha asignado clase")
        GradoDto grado,
        Integer anio,
        @Schema(description = "`false` = pendiente de confirmar por el centro (importado o auto-registrado)")
        boolean activo,
        @Schema(description = "Empresa de su asignación abierta, o `null` si no tiene ninguna en curso")
        Long empresaId,
        String empresaNombre,
        @Schema(description = "Tutor de prácticas de su asignación abierta, o `null` si no tiene ninguna en curso")
        Long tutorId,
        String tutorNombre) {
}
