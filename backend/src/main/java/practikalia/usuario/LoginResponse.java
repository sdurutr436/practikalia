package practikalia.usuario;

public record LoginResponse(Rol rol, boolean esAdmin, boolean debeCambiarContrasena) {
}
