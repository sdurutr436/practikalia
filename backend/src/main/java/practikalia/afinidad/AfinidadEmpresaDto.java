package practikalia.afinidad;

import practikalia.empresa.EmpresaAlumnoDto;
import practikalia.etiqueta.EtiquetaDto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Afinidad de un alumno con una empresa concreta, con el detalle que explica el score (para poder justificarlo en pantalla). */
public record AfinidadEmpresaDto(
        EmpresaAlumnoDto empresa,
        @Schema(description = "Coeficiente de solapamiento `|A∩B| / min(|A|,|B|)` (no Jaccard) más un bonus si coincide el sector; `0.0` si el alumno o la empresa no tienen etiquetas")
        double score,
        List<EtiquetaDto> etiquetasCoincidentes,
        boolean sectorCoincide) {
}
