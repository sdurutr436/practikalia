package practikalia.usuario;

public record MeResponse(String correo, Rol rol, boolean esAdmin, boolean debeCambiarContrasena) {
}
