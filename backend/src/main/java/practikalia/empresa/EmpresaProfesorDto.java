package practikalia.empresa;

import practikalia.etiqueta.EtiquetaDto;

import java.time.Instant;
import java.util.List;

public record EmpresaProfesorDto(
        Long id,
        String nombre,
        String descripcion,
        String imagen,
        String direccion,
        EtiquetaDto sector,
        List<EtiquetaDto> etiquetas,
        String observaciones,
        String contactoNombre,
        String contactoTelefono,
        String contactoEmail,
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
