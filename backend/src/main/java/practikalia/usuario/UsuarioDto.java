package practikalia.usuario;

import practikalia.etiqueta.EtiquetaDto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Representación interna completa de un usuario, usada por los servicios (no expuesta tal cual salvo vía {@code /me}). */
public record UsuarioDto(String correo, Rol rol,
        @Schema(description = "Flag independiente del rol: da acceso a los menús de administración") boolean esAdmin,
        @Schema(description = "Si es `true`, la cuenta solo puede llamar a `POST /api/auth/cambiar-contrasena` hasta cambiarla") boolean debeCambiarContrasena,
        List<EtiquetaDto> etiquetas) {

    static UsuarioDto de(Usuario usuario) {
        return new UsuarioDto(usuario.getCorreo(), usuario.getRol(), usuario.isEsAdmin(),
                usuario.isDebeCambiarContrasena(),
                usuario.getEtiquetas().stream().map(EtiquetaDto::de).toList());
    }
}
