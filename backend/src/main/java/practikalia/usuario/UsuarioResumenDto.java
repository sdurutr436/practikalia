package practikalia.usuario;

/** Vista mínima de usuario para listados (pickers de alumno/tutor), sin datos sensibles. */
public record UsuarioResumenDto(Long id, String correo, Rol rol) {

    static UsuarioResumenDto de(Usuario usuario) {
        return new UsuarioResumenDto(usuario.getId(), usuario.getCorreo(), usuario.getRol());
    }
}
