package practikalia.usuario.correo;

/** Una fila de la whitelist. Dato personal: solo lo ve un admin. */
public record CorreoPermitidoDto(Long id, String correo) {

    static CorreoPermitidoDto de(CorreoPermitido correoPermitido) {
        return new CorreoPermitidoDto(correoPermitido.getId(), correoPermitido.getCorreo());
    }
}
