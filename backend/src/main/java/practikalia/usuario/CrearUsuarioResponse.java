package practikalia.usuario;

public record CrearUsuarioResponse(Long id, String correo, Rol rol, String contrasenaTemporal) {
}
