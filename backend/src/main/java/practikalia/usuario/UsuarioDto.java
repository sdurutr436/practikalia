package practikalia.usuario;

public record UsuarioDto(String correo, Rol rol, boolean esAdmin, boolean debeCambiarContrasena) {

    static UsuarioDto de(Usuario usuario) {
        return new UsuarioDto(usuario.getCorreo(), usuario.getRol(), usuario.isEsAdmin(), usuario.isDebeCambiarContrasena());
    }
}
