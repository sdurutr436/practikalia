package practikalia.usuario;

import practikalia.etiqueta.EtiquetaDto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Perfil completo del usuario autenticado. */
public record MeResponse(String correo, Rol rol,
        @Schema(description = "Flag independiente del rol: da acceso a los menús de administración") boolean esAdmin,
        @Schema(description = "Si es `true`, la cuenta solo puede llamar a `POST /api/auth/cambiar-contrasena` hasta cambiarla") boolean debeCambiarContrasena,
        List<EtiquetaDto> etiquetas) {
}
