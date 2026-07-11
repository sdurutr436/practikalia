package practikalia.usuario;

import practikalia.etiqueta.EtiquetaDto;

import java.util.List;

public record UsuarioDto(String correo, Rol rol, boolean esAdmin, boolean debeCambiarContrasena,
        List<EtiquetaDto> etiquetas) {

    static UsuarioDto de(Usuario usuario) {
        return new UsuarioDto(usuario.getCorreo(), usuario.getRol(), usuario.isEsAdmin(),
                usuario.isDebeCambiarContrasena(),
                usuario.getEtiquetas().stream().map(EtiquetaDto::de).toList());
    }
}
