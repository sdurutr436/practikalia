package practikalia.usuario;

import practikalia.usuario.correo.CorreoPermitidoRepository;
import practikalia.usuario.jwt.JwtService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class UsuarioServiceTest {

    private static final Pattern POLITICA = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$");

    private UsuarioRepository usuarioRepository;
    private CorreoPermitidoRepository correoPermitidoRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private JwtService jwtService;
    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        correoPermitidoRepository = mock(CorreoPermitidoRepository.class);
        jwtService = mock(JwtService.class);
        usuarioService = new UsuarioService(
                usuarioRepository, correoPermitidoRepository, passwordEncoder, jwtService,
                "iesejemplo.es");
    }

    private Usuario profesor() {
        Usuario profesor = new Usuario("prof@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.PROFESOR);
        profesor.setId(1L);
        return profesor;
    }

    @Test
    void creaAlumnoConDominioPermitidoYContrasenaTemporalQueCumplePolitica() {
        when(usuarioRepository.findByCorreo("ana@iesejemplo.es")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        CrearUsuarioResponse response = usuarioService.crearUsuario(
                new CrearUsuarioRequest("ana@iesejemplo.es", Rol.ALUMNO), profesor());

        assertThat(response.correo()).isEqualTo("ana@iesejemplo.es");
        assertThat(response.contrasenaTemporal()).hasSize(12);
        assertThat(POLITICA.matcher(response.contrasenaTemporal()).matches()).isTrue();
    }

    @Test
    void rechazaAltaConCorreoFueraDeDominioYWhitelist() {
        when(correoPermitidoRepository.existsByCorreo("ana@gmail.com")).thenReturn(false);

        assertThatThrownBy(() -> usuarioService.crearUsuario(
                new CrearUsuarioRequest("ana@gmail.com", Rol.ALUMNO), profesor()))
                .isInstanceOf(UsuarioException.class)
                .hasFieldOrPropertyWithValue("codigo", "CORREO_NO_PERMITIDO");
    }

    @Test
    void permiteAltaPorWhitelistAunqueElDominioNoEsteEnAllowedDomains() {
        when(correoPermitidoRepository.existsByCorreo("ana@gmail.com")).thenReturn(true);
        when(usuarioRepository.findByCorreo("ana@gmail.com")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        CrearUsuarioResponse response = usuarioService.crearUsuario(
                new CrearUsuarioRequest("ana@gmail.com", Rol.ALUMNO), profesor());

        assertThat(response.correo()).isEqualTo("ana@gmail.com");
    }

    @Test
    void profesorSinEsAdminNoPuedeCrearOtroProfesor() {
        assertThatThrownBy(() -> usuarioService.crearUsuario(
                new CrearUsuarioRequest("otro@iesejemplo.es", Rol.PROFESOR), profesor()))
                .isInstanceOf(UsuarioException.class)
                .hasFieldOrPropertyWithValue("codigo", "ACCESO_DENEGADO");
    }

    @Test
    void esAdminSiPuedeCrearOtroProfesor() {
        Usuario admin = profesor();
        admin.setEsAdmin(true);
        when(usuarioRepository.findByCorreo("otro@iesejemplo.es")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        CrearUsuarioResponse response = usuarioService.crearUsuario(
                new CrearUsuarioRequest("otro@iesejemplo.es", Rol.PROFESOR), admin);

        assertThat(response.rol()).isEqualTo(Rol.PROFESOR);
    }

    @Test
    void rechazaAltaConCorreoYaRegistrado() {
        when(usuarioRepository.findByCorreo("ana@iesejemplo.es")).thenReturn(Optional.of(profesor()));

        assertThatThrownBy(() -> usuarioService.crearUsuario(
                new CrearUsuarioRequest("ana@iesejemplo.es", Rol.ALUMNO), profesor()))
                .isInstanceOf(UsuarioException.class)
                .hasFieldOrPropertyWithValue("codigo", "CORREO_YA_REGISTRADO");
    }

    @Test
    void loginFallidoIncrementaIntentosYBloqueaAlQuintoFallo() {
        Usuario usuario = new Usuario("ana@iesejemplo.es", passwordEncoder.encode("Correcta123!"), Rol.ALUMNO);
        usuario.setIntentosFallidos(4);
        when(usuarioRepository.findByCorreo("ana@iesejemplo.es")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> usuarioService.login(new LoginRequest("ana@iesejemplo.es", "incorrecta", ""), "127.0.0.1"))
                .isInstanceOf(UsuarioException.class)
                .hasFieldOrPropertyWithValue("codigo", "CREDENCIALES_INVALIDAS");

        assertThat(usuario.getIntentosFallidos()).isEqualTo(5);
        assertThat(usuario.getBloqueadoHasta()).isAfter(Instant.now());
    }

    @Test
    void loginRechazadoMientrasLaCuentaEstaBloqueada() {
        Usuario usuario = new Usuario("ana@iesejemplo.es", passwordEncoder.encode("Correcta123!"), Rol.ALUMNO);
        usuario.setBloqueadoHasta(Instant.now().plusSeconds(60));
        when(usuarioRepository.findByCorreo("ana@iesejemplo.es")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> usuarioService.login(new LoginRequest("ana@iesejemplo.es", "Correcta123!", ""), "127.0.0.1"))
                .isInstanceOf(UsuarioException.class)
                .hasFieldOrPropertyWithValue("codigo", "DEMASIADOS_INTENTOS");
    }

    @Test
    void loginCorrectoReseteaIntentosFallidos() {
        Usuario usuario = new Usuario("ana@iesejemplo.es", passwordEncoder.encode("Correcta123!"), Rol.ALUMNO);
        usuario.setIntentosFallidos(3);
        when(usuarioRepository.findByCorreo("ana@iesejemplo.es")).thenReturn(Optional.of(usuario));

        Usuario resultado = usuarioService.login(new LoginRequest("ana@iesejemplo.es", "Correcta123!", ""), "127.0.0.1");

        assertThat(resultado.getIntentosFallidos()).isZero();
        assertThat(resultado.getBloqueadoHasta()).isNull();
    }

    @Test
    void loginConHoneypotRellenoEsRechazadoComoCredencialesInvalidas() {
        assertThatThrownBy(() -> usuarioService.login(new LoginRequest("ana@iesejemplo.es", "cualquiera", "bot"), "127.0.0.1"))
                .isInstanceOf(UsuarioException.class)
                .hasFieldOrPropertyWithValue("codigo", "CREDENCIALES_INVALIDAS");
    }

    @Test
    void emitirTokenUsaTokenRestringidoSiDebeCambiarContrasena() {
        Usuario usuario = profesor();
        usuario.setDebeCambiarContrasena(true);
        when(jwtService.generarTokenRestringido(usuario)).thenReturn("token-restringido");

        assertThat(usuarioService.emitirToken(usuario)).isEqualTo("token-restringido");
        verify(jwtService, never()).generarTokenNormal(any());
    }

    @Test
    void emitirTokenUsaTokenNormalSiNoDebeCambiarContrasena() {
        Usuario usuario = profesor();
        usuario.setDebeCambiarContrasena(false);
        when(jwtService.generarTokenNormal(usuario)).thenReturn("token-normal");

        assertThat(usuarioService.emitirToken(usuario)).isEqualTo("token-normal");
        verify(jwtService, never()).generarTokenRestringido(any());
    }

    @Test
    void cambiarContrasenaConActualIncorrectaLanzaExcepcion() {
        Usuario usuario = new Usuario("ana@iesejemplo.es", passwordEncoder.encode("Correcta123!"), Rol.ALUMNO);
        when(usuarioRepository.findByCorreo("ana@iesejemplo.es")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> usuarioService.cambiarContrasena(
                "ana@iesejemplo.es", new CambiarContrasenaRequest("incorrecta", "Nueva123!")))
                .isInstanceOf(UsuarioException.class)
                .hasFieldOrPropertyWithValue("codigo", "CONTRASENA_ACTUAL_INCORRECTA");
    }

    @Test
    void cambiarContrasenaConNuevaQueNoCumplePoliticaLanzaExcepcion() {
        Usuario usuario = new Usuario("ana@iesejemplo.es", passwordEncoder.encode("Correcta123!"), Rol.ALUMNO);
        when(usuarioRepository.findByCorreo("ana@iesejemplo.es")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> usuarioService.cambiarContrasena(
                "ana@iesejemplo.es", new CambiarContrasenaRequest("Correcta123!", "simple")))
                .isInstanceOf(UsuarioException.class)
                .hasFieldOrPropertyWithValue("codigo", "CONTRASENA_NO_CUMPLE_POLITICA");
    }

    @Test
    void cambiarContrasenaExitosaActualizaHashYQuitaFlag() {
        Usuario usuario = new Usuario("ana@iesejemplo.es", passwordEncoder.encode("Correcta123!"), Rol.ALUMNO);
        when(usuarioRepository.findByCorreo("ana@iesejemplo.es")).thenReturn(Optional.of(usuario));

        usuarioService.cambiarContrasena("ana@iesejemplo.es", new CambiarContrasenaRequest("Correcta123!", "Nueva123!"));

        assertThat(usuario.isDebeCambiarContrasena()).isFalse();
        assertThat(passwordEncoder.matches("Nueva123!", usuario.getContrasenaHash())).isTrue();
    }
}
