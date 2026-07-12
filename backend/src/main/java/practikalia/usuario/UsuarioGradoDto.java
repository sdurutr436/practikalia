package practikalia.usuario;

import practikalia.grado.GradoDto;

/** Perfil de grado/año de un usuario. */
public record UsuarioGradoDto(Long id, String correo, GradoDto grado, Integer anio) {

    static UsuarioGradoDto de(Usuario usuario) {
        return new UsuarioGradoDto(usuario.getId(), usuario.getCorreo(), GradoDto.de(usuario.getGrado()), usuario.getAnio());
    }
}
