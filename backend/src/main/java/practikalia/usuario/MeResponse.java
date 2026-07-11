package practikalia.usuario;

import practikalia.etiqueta.EtiquetaDto;

import java.util.List;

public record MeResponse(String correo, Rol rol, boolean esAdmin, boolean debeCambiarContrasena,
        List<EtiquetaDto> etiquetas) {
}
