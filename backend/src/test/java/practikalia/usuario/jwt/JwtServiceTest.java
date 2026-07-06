package practikalia.usuario.jwt;

import practikalia.usuario.Rol;
import practikalia.usuario.Usuario;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService("clave-de-pruebas-larga-para-hs256-1234567890", 3_600_000);

    @Test
    void tokenNormalIncluyeRolYAdminCuandoCorresponde() {
        Usuario usuario = new Usuario("ana@iesejemplo.es", "hash", Rol.PROFESOR);
        usuario.setEsAdmin(true);

        var jws = jwtService.parsear(jwtService.generarTokenNormal(usuario)).orElseThrow();

        assertThat(jwtService.correo(jws)).isEqualTo("ana@iesejemplo.es");
        assertThat(jwtService.authorities(jws)).containsExactlyInAnyOrder("ROLE_PROFESOR", "ADMIN");
    }

    @Test
    void tokenNormalSinAdminNoIncluyeAuthorityAdmin() {
        Usuario usuario = new Usuario("bea@iesejemplo.es", "hash", Rol.ALUMNO);

        var jws = jwtService.parsear(jwtService.generarTokenNormal(usuario)).orElseThrow();

        assertThat(jwtService.authorities(jws)).containsExactly("ROLE_ALUMNO");
    }

    @Test
    void tokenRestringidoSoloTieneAutoridadDeCambioPendiente() {
        Usuario usuario = new Usuario("carla@iesejemplo.es", "hash", Rol.ALUMNO);

        var jws = jwtService.parsear(jwtService.generarTokenRestringido(usuario)).orElseThrow();

        assertThat(jwtService.authorities(jws)).containsExactly(JwtService.AUTORIDAD_CAMBIO_PENDIENTE);
    }

    @Test
    void parsearTokenCorruptoDevuelveVacio() {
        assertThat(jwtService.parsear("esto-no-es-un-jwt")).isEmpty();
    }

    @Test
    void parsearTokenExpiradoDevuelveVacio() {
        JwtService servicioExpirado = new JwtService("clave-de-pruebas-larga-para-hs256-1234567890", -1000);
        Usuario usuario = new Usuario("dani@iesejemplo.es", "hash", Rol.ALUMNO);

        String token = servicioExpirado.generarTokenNormal(usuario);

        assertThat(jwtService.parsear(token)).isEmpty();
    }
}
