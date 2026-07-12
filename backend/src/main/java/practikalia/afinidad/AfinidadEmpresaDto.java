package practikalia.afinidad;

import practikalia.empresa.EmpresaAlumnoDto;
import practikalia.etiqueta.EtiquetaDto;

import java.util.List;

public record AfinidadEmpresaDto(
        EmpresaAlumnoDto empresa,
        double score,
        List<EtiquetaDto> etiquetasCoincidentes,
        boolean sectorCoincide) {
}
