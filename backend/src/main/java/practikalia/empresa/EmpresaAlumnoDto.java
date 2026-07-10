package practikalia.empresa;

import practikalia.etiqueta.EtiquetaDto;

import java.util.List;

public record EmpresaAlumnoDto(
        Long id,
        String nombre,
        String descripcion,
        String imagen,
        String direccion,
        EtiquetaDto sector,
        List<EtiquetaDto> etiquetas) {

    static EmpresaAlumnoDto de(Empresa empresa) {
        return new EmpresaAlumnoDto(
                empresa.getId(),
                empresa.getNombre(),
                empresa.getDescripcion(),
                empresa.getImagen(),
                empresa.getDireccion(),
                EtiquetaDto.de(empresa.getSector()),
                empresa.getEtiquetas().stream().map(EtiquetaDto::de).toList());
    }
}
