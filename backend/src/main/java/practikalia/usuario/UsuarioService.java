package practikalia.usuario;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private static final Pattern POLITICA_CONTRASENA = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$");
    private static final String MAYUSCULAS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghijkmnpqrstuvwxyz";
    private static final String NUMEROS = "23456789";
    private static final String ESPECIALES = "!@#$%^&*-_+=?";
    private static final String TODOS_LOS_CARACTERES = MAYUSCULAS + MINUSCULAS + NUMEROS + ESPECIALES;
    private static final int LONGITUD_CONTRASENA_TEMPORAL = 12;
    private static final int INTENTOS_MAXIMOS = 5;
    private static final java.time.Duration DURACION_BLOQUEO = java.time.Duration.ofMinutes(15);

    private final SecureRandom random = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final CorreoPermitidoRepository correoPermitidoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;
    private final Set<String> dominiosPermitidos;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            CorreoPermitidoRepository correoPermitidoRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            ApplicationEventPublisher eventPublisher,
            @Value("${allowed.domains:}") String dominiosPermitidosCsv) {
        this.usuarioRepository = usuarioRepository;
        this.correoPermitidoRepository = correoPermitidoRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.eventPublisher = eventPublisher;
        this.dominiosPermitidos = Arrays.stream(dominiosPermitidosCsv.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(dominio -> !dominio.isBlank())
                .collect(Collectors.toSet());
    }

    @Transactional
    public CrearUsuarioResponse crearUsuario(CrearUsuarioRequest request, Usuario creador) {
        String correo = request.correo().toLowerCase();

        if (request.rol() == Rol.PROFESOR && !creador.isEsAdmin()) {
            throw UsuarioException.accesoDenegado();
        }
        if (!correoPermitido(correo)) {
            throw UsuarioException.correoNoPermitido();
        }
        if (usuarioRepository.findByCorreo(correo).isPresent()) {
            throw UsuarioException.correoYaRegistrado();
        }

        String contrasenaTemporal = generarContrasenaTemporal();
        Usuario usuario = new Usuario(correo, passwordEncoder.encode(contrasenaTemporal), request.rol());
        usuarioRepository.save(usuario);

        eventPublisher.publishEvent(new ContrasenaTemporalGeneradaEvent(usuario.getId(), correo, contrasenaTemporal));

        return new CrearUsuarioResponse(usuario.getId(), usuario.getCorreo(), usuario.getRol(), contrasenaTemporal);
    }

    @Transactional
    public Usuario login(LoginRequest request, String ipRemota) {
        String correo = request.correo().toLowerCase();

        if (!request.web().isBlank()) {
            log.warn("Intento de login sospechoso (honeypot relleno): correo={} ip={}", correo, ipRemota);
            throw UsuarioException.credencialesInvalidas();
        }

        Usuario usuario = usuarioRepository.findByCorreo(correo).orElseGet(() -> {
            log.info("Login fallido, correo inexistente: correo={} ip={}", correo, ipRemota);
            throw UsuarioException.credencialesInvalidas();
        });

        if (!usuario.isActivo() || !correoPermitido(usuario.getCorreo())) {
            log.info("Login rechazado, cuenta no disponible: correo={} ip={}", correo, ipRemota);
            throw UsuarioException.cuentaNoDisponible();
        }

        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(Instant.now())) {
            log.info("Login rechazado, cuenta bloqueada: correo={} ip={}", correo, ipRemota);
            throw UsuarioException.demasiadosIntentos();
        }

        if (!passwordEncoder.matches(request.contrasena(), usuario.getContrasenaHash())) {
            registrarIntentoFallido(usuario);
            log.info("Login fallido, contraseña incorrecta: correo={} ip={}", correo, ipRemota);
            throw UsuarioException.credencialesInvalidas();
        }

        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);
        return usuario;
    }

    public String emitirToken(Usuario usuario) {
        return usuario.isDebeCambiarContrasena()
                ? jwtService.generarTokenRestringido(usuario)
                : jwtService.generarTokenNormal(usuario);
    }

    public Usuario buscarPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo).orElseThrow(UsuarioException::credencialesInvalidas);
    }

    @Transactional
    public void cambiarContrasena(String correo, CambiarContrasenaRequest request) {
        Usuario usuario = buscarPorCorreo(correo);

        if (!passwordEncoder.matches(request.contrasenaActual(), usuario.getContrasenaHash())) {
            throw UsuarioException.contrasenaActualIncorrecta();
        }
        if (!cumplePolitica(request.contrasenaNueva())) {
            throw UsuarioException.contrasenaNoCumplePolitica();
        }

        usuario.setContrasenaHash(passwordEncoder.encode(request.contrasenaNueva()));
        usuario.setDebeCambiarContrasena(false);
        usuarioRepository.save(usuario);
    }

    private void registrarIntentoFallido(Usuario usuario) {
        usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
        if (usuario.getIntentosFallidos() >= INTENTOS_MAXIMOS) {
            usuario.setBloqueadoHasta(Instant.now().plus(DURACION_BLOQUEO));
        }
        usuarioRepository.save(usuario);
    }

    private boolean correoPermitido(String correo) {
        String dominio = correo.substring(correo.indexOf('@') + 1).toLowerCase();
        return dominiosPermitidos.contains(dominio) || correoPermitidoRepository.existsByCorreo(correo);
    }

    private boolean cumplePolitica(String contrasena) {
        return POLITICA_CONTRASENA.matcher(contrasena).matches();
    }

    private String generarContrasenaTemporal() {
        List<Character> caracteres = new ArrayList<>();
        caracteres.add(MAYUSCULAS.charAt(random.nextInt(MAYUSCULAS.length())));
        caracteres.add(MINUSCULAS.charAt(random.nextInt(MINUSCULAS.length())));
        caracteres.add(NUMEROS.charAt(random.nextInt(NUMEROS.length())));
        caracteres.add(ESPECIALES.charAt(random.nextInt(ESPECIALES.length())));
        for (int i = caracteres.size(); i < LONGITUD_CONTRASENA_TEMPORAL; i++) {
            caracteres.add(TODOS_LOS_CARACTERES.charAt(random.nextInt(TODOS_LOS_CARACTERES.length())));
        }
        Collections.shuffle(caracteres, random);
        StringBuilder resultado = new StringBuilder();
        caracteres.forEach(resultado::append);
        return resultado.toString();
    }
}
