package practikalia.usuario;

import practikalia.grado.Grado;
import practikalia.grado.GradoRepository;
import practikalia.usuario.jwt.JwtService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
    @Autowired
    private GradoRepository gradoRepository;

    /** 12345678 % 23 = 14, y la letra 14 de la cadena de control es la Z. */
    private static final String DNI_VALIDO = "12345678Z";

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
        Usuario usuario = guardarUsuario("luis@iesejemplo.es", "Correcta123!", Rol.PROFESOR, false);

        mockMvc.perform(get("/api/auth/me")
                        .with(user("luis@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuario.getId()))
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

    private RegistroRequest registro(String correo, String dni, Long gradoId) {
        return new RegistroRequest("Lucía", "Pérez", "Gómez", dni, gradoId, correo, "");
    }

    @Test
    void registroValidoDejaLaCuentaPendienteDeAprobacion() throws Exception {
        Grado grado = gradoRepository.save(new Grado("DAM"));

        mockMvc.perform(post("/api/auth/registro")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registro("lucia@iesejemplo.es", DNI_VALIDO, grado.getId()))))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        Usuario creado = usuarioRepository.findByCorreo("lucia@iesejemplo.es").orElseThrow();
        assertThat(creado.isActivo()).isFalse();
        assertThat(creado.getRol()).isEqualTo(Rol.ALUMNO);
        assertThat(creado.isEsAdmin()).isFalse();
        assertThat(creado.isDebeCambiarContrasena()).isTrue();
        assertThat(creado.getDni()).isEqualTo(DNI_VALIDO);
        // La contraseña inicial es el DNI sin la letra: el alumno la sabe sin
        // que el centro tenga que repartirle nada.
        assertThat(passwordEncoder.matches("12345678", creado.getContrasenaHash())).isTrue();
        assertThat(creado.getGrado().getId()).isEqualTo(grado.getId());
    }

    @Test
    void registroConLetraDeDniIncorrectaDevuelve400() throws Exception {
        Grado grado = gradoRepository.save(new Grado("DAM"));

        mockMvc.perform(post("/api/auth/registro")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registro("lucia@iesejemplo.es", "12345678A", grado.getId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DNI_INVALIDO"));

        assertThat(usuarioRepository.findByCorreo("lucia@iesejemplo.es")).isEmpty();
    }

    @Test
    void registroConDominioFueraDeLaListaDevuelve400() throws Exception {
        Grado grado = gradoRepository.save(new Grado("DAM"));

        mockMvc.perform(post("/api/auth/registro")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registro("lucia@gmail.com", DNI_VALIDO, grado.getId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CORREO_DOMINIO_NO_PERMITIDO"));
    }

    @Test
    void registroConGradoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registro("lucia@iesejemplo.es", DNI_VALIDO, 9999L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("GRADO_NO_ENCONTRADO"));
    }

    @Test
    void registroConCorreoYaRegistradoDevuelve409() throws Exception {
        Grado grado = gradoRepository.save(new Grado("DAM"));
        guardarUsuario("lucia@iesejemplo.es", "Correcta123!", Rol.ALUMNO, false);

        mockMvc.perform(post("/api/auth/registro")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registro("lucia@iesejemplo.es", DNI_VALIDO, grado.getId()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CORREO_YA_EXISTE"));
    }

    @Test
    void registroConHoneypotRellenoNoCreaLaCuenta() throws Exception {
        Grado grado = gradoRepository.save(new Grado("DAM"));

        mockMvc.perform(post("/api/auth/registro")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegistroRequest(
                                "Lucía", "Pérez", null, DNI_VALIDO, grado.getId(), "lucia@iesejemplo.es", "soy-un-bot"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));

        assertThat(usuarioRepository.findByCorreo("lucia@iesejemplo.es")).isEmpty();
    }

    @Test
    void loginConCuentaRecienAutoregistradaDevuelve403() throws Exception {
        Grado grado = gradoRepository.save(new Grado("DAM"));
        mockMvc.perform(post("/api/auth/registro")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registro("lucia@iesejemplo.es", DNI_VALIDO, grado.getId()))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("lucia@iesejemplo.es", "loQueSea1!", ""))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CUENTA_NO_DISPONIBLE"));
    }
}
