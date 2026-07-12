package practikalia.afinidad;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Listado de afinidad de un alumno con todas las empresas publicadas, ordenado por score descendente (desempate alfabético). */
public record AfinidadListadoDto(
        @Schema(description = "Si es `false`, el alumno no tiene etiquetas de interés marcadas: todos los scores son `0.0` y el orden es alfabético puro")
        boolean alumnoConEtiquetas,
        List<AfinidadEmpresaDto> empresas) {
}
