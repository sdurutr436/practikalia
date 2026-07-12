package practikalia.afinidad;

import java.util.List;

public record AfinidadListadoDto(boolean alumnoConEtiquetas, List<AfinidadEmpresaDto> empresas) {
}
