package practikalia.empresa;

import practikalia.etiqueta.EtiquetaDto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Vista de empresa para alumnado: solo campos orientativos, sin datos de contacto ni de gestión interna. */
public record EmpresaAlumnoDto(
        Long id,
        String nombre,
        String descripcion,
        @Schema(description = "Ruta relativa servida por nginx en `/uploads/`; `null` si la empresa no tiene imagen todavía")
        String imagen,
        String direccion,
        EtiquetaDto sector,
        List<EtiquetaDto> etiquetas) {

    public static EmpresaAlumnoDto de(Empresa empresa) {
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
