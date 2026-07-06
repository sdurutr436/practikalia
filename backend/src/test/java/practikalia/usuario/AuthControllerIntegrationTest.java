package practikalia.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "allowed.domains=iesejemplo.es")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private Usuario guardarUsuario(String correo, String contrasena, Rol rol, boolean debeCambiarContrasena) {
        Usuario usuario = new Usuario(correo, passwordEncoder.encode(contrasena), rol);
        usuario.setDebeCambiarContrasena(debeCambiarContrasena);
        return usuarioRepository.save(usuario);
    }

    @Test
    void loginConCredencialesCorrectasDevuelveCookieConRolNormal() throws Exception {
        guardarUsuario("ana@iesejemplo.es", "Correcta123!", Rol.ALUMNO, false);

        var resultado = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("ana@iesejemplo.es", "Correcta123!", ""))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ALUMNO"))
                .andExpect(jsonPath("$.debeCambiarContrasena").value(false))
                .andReturn();

        assertQueLaCookieTieneAutoridad(resultado.getResponse().getCookie("practikalia_token"), "ROLE_ALUMNO");
    }

    @Test
    void loginConCambioPendienteDevuelveTokenRestringido() throws Exception {
        guardarUsuario("bea@iesejemplo.es", "Correcta123!", Rol.ALUMNO, true);

        var resultado = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("bea@iesejemplo.es", "Correcta123!", ""))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debeCambiarContrasena").value(true))
                .andReturn();

        assertQueLaCookieTieneAutoridad(resultado.getResponse().getCookie("practikalia_token"), JwtService.AUTORIDAD_CAMBIO_PENDIENTE);
    }

    private void assertQueLaCookieTieneAutoridad(Cookie cookieToken, String autoridad) {
        assertThat(cookieToken).isNotNull();
        var jws = jwtService.parsear(cookieToken.getValue()).orElseThrow();
        assertThat(jwtService.authorities(jws)).contains(autoridad);
    }

    @Test
    void loginConContrasenaIncorrectaDevuelveCredencialesInvalidas() throws Exception {
        guardarUsuario("carla@iesejemplo.es", "Correcta123!", Rol.ALUMNO, false);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("carla@iesejemplo.es", "Incorrecta1!", ""))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void loginConCorreoInexistenteDevuelveCredencialesInvalidas() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("nadie@iesejemplo.es", "Cualquiera1!", ""))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void loginConHoneypotRellenoDevuelveCredencialesInvalidas() throws Exception {
        guardarUsuario("dani@iesejemplo.es", "Correcta123!", Rol.ALUMNO, false);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("dani@iesejemplo.es", "Correcta123!", "bot"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void loginConCuentaBloqueadaDevuelveDemasiadosIntentos() throws Exception {
        Usuario usuario = guardarUsuario("elena@iesejemplo.es", "Correcta123!", Rol.ALUMNO, false);
        usuario.setBloqueadoHasta(Instant.now().plusSeconds(600));
        usuarioRepository.save(usuario);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("elena@iesejemplo.es", "Correcta123!", ""))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigo").value("DEMASIADOS_INTENTOS"));
    }

    @Test
    void loginConCuentaInactivaDevuelveCuentaNoDisponible() throws Exception {
        Usuario usuario = guardarUsuario("fran@iesejemplo.es", "Correcta123!", Rol.ALUMNO, false);
        usuario.setActivo(false);
        usuarioRepository.save(usuario);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("fran@iesejemplo.es", "Correcta123!", ""))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CUENTA_NO_DISPONIBLE"));
    }

    @Test
    void loginConCorreoRetiradoDeDominioYWhitelistDevuelveCuentaNoDisponible() throws Exception {
        guardarUsuario("gema@dominio-retirado.es", "Correcta123!", Rol.ALUMNO, false);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("gema@dominio-retirado.es", "Correcta123!", ""))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CUENTA_NO_DISPONIBLE"));
    }

    @Test
    void cambiarContrasenaConTokenRestringidoFunciona() throws Exception {
        guardarUsuario("hugo@iesejemplo.es", "Correcta123!", Rol.ALUMNO, true);

        mockMvc.perform(post("/api/auth/cambiar-contrasena")
                        .with(csrf())
                        .with(user("hugo@iesejemplo.es").authorities(new SimpleGrantedAuthority(JwtService.AUTORIDAD_CAMBIO_PENDIENTE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CambiarContrasenaRequest("Correcta123!", "Nueva123!"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void cambiarContrasenaConRolNormalFunciona() throws Exception {
        guardarUsuario("ines@iesejemplo.es", "Correcta123!", Rol.ALUMNO, false);

        mockMvc.perform(post("/api/auth/cambiar-contrasena")
                        .with(csrf())
                        .with(user("ines@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CambiarContrasenaRequest("Correcta123!", "Nueva123!"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void cambiarContrasenaConActualIncorrectaDevuelve401() throws Exception {
        guardarUsuario("jose@iesejemplo.es", "Correcta123!", Rol.ALUMNO, false);

        mockMvc.perform(post("/api/auth/cambiar-contrasena")
                        .with(csrf())
                        .with(user("jose@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CambiarContrasenaRequest("Mala123!", "Nueva123!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CONTRASENA_ACTUAL_INCORRECTA"));
    }

    @Test
    void cambiarContrasenaConPoliticaIncumplidaDevuelve400() throws Exception {
        guardarUsuario("karla@iesejemplo.es", "Correcta123!", Rol.ALUMNO, false);

        mockMvc.perform(post("/api/auth/cambiar-contrasena")
                        .with(csrf())
                        .with(user("karla@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CambiarContrasenaRequest("Correcta123!", "simple"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CONTRASENA_NO_CUMPLE_POLITICA"));
    }

    @Test
    void meConSesionValidaDevuelveDatosDelUsuario() throws Exception {
        guardarUsuario("luis@iesejemplo.es", "Correcta123!", Rol.PROFESOR, false);

        mockMvc.perform(get("/api/auth/me")
                        .with(user("luis@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("luis@iesejemplo.es"))
                .andExpect(jsonPath("$.rol").value("PROFESOR"));
    }

    @Test
    void meSinSesionDevuelveNoAutenticado() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void logoutDejaLaCookieExpirada() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .with(user("cualquiera@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("practikalia_token", 0));
    }

    @Test
    void tokenRestringidoContraEndpointDeRolNormalDevuelve403() throws Exception {
        Usuario usuario = guardarUsuario("mario@iesejemplo.es", "Correcta123!", Rol.ALUMNO, true);
        String tokenRestringido = jwtService.generarTokenRestringido(usuario);

        mockMvc.perform(get("/api/auth/me")
                        .cookie(new Cookie("practikalia_token", tokenRestringido)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void mutacionSinTokenCsrfDevuelve403() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(user("cualquiera@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }
}
