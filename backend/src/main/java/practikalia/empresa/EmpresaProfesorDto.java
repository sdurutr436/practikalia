package practikalia.empresa;

import practikalia.etiqueta.EtiquetaDto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/** Vista de empresa para profesorado/admin: detalle completo, incluidos contacto y datos de gestión. */
public record EmpresaProfesorDto(
        Long id,
        String nombre,
        String descripcion,
        @Schema(description = "Ruta relativa servida por nginx en `/uploads/`; `null` si la empresa no tiene imagen todavía")
        String imagen,
        String direccion,
        EtiquetaDto sector,
        List<EtiquetaDto> etiquetas,
        String observaciones,
        String contactoNombre,
        String contactoTelefono,
        String contactoEmail,
        @Schema(description = "Si es `false`, la empresa no aparece en el listado ni el detalle del alumnado")
        boolean publicada,
        String creadaPorCorreo,
        Instant fechaCreacion) {

    static EmpresaProfesorDto de(Empresa empresa) {
        return new EmpresaProfesorDto(
                empresa.getId(),
                empresa.getNombre(),
                empresa.getDescripcion(),
                empresa.getImagen(),
                empresa.getDireccion(),
                EtiquetaDto.de(empresa.getSector()),
                empresa.getEtiquetas().stream().map(EtiquetaDto::de).toList(),
                empresa.getObservaciones(),
                empresa.getContactoNombre(),
                empresa.getContactoTelefono(),
                empresa.getContactoEmail(),
                empresa.isPublicada(),
                empresa.getCreadaPor().getCorreo(),
                empresa.getFechaCreacion());
    }
}
