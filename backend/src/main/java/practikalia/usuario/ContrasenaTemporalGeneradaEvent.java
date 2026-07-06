package practikalia.usuario;

public record ContrasenaTemporalGeneradaEvent(Long usuarioId, String correo, String contrasenaTemporal) {
}
