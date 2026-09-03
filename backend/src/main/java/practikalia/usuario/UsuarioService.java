package practikalia.usuario;

import practikalia.etiqueta.Etiqueta;
import practikalia.etiqueta.EtiquetaDto;
import practikalia.etiqueta.EtiquetaRepository;
import practikalia.grado.Grado;
import practikalia.grado.GradoException;
import practikalia.grado.GradoRepository;
import practikalia.usuario.correo.CorreoPermitido;
import practikalia.usuario.correo.CorreoPermitidoRepository;
import practikalia.usuario.jwt.JwtService;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private static final Pattern POLITICA_CONTRASENA = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$");
    private static final Pattern FORMATO_DNI = Pattern.compile("^(\\d{8})([A-Z])$");
    /** Letra de control del DNI: el resto de dividir el número entre 23 indexa esta cadena. */
    private static final String LETRAS_DNI = "TRWAGMYFPDXBNJZSQVHLCKE";
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
    private final GradoRepository gradoRepository;
    private final EtiquetaRepository etiquetaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Set<String> dominiosPermitidos;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            CorreoPermitidoRepository correoPermitidoRepository,
            GradoRepository gradoRepository,
            EtiquetaRepository etiquetaRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${allowed.domains:}") String dominiosPermitidosCsv) {
        this.usuarioRepository = usuarioRepository;
        this.correoPermitidoRepository = correoPermitidoRepository;
        this.gradoRepository = gradoRepository;
        this.etiquetaRepository = etiquetaRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dominiosPermitidos = Arrays.stream(dominiosPermitidosCsv.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(dominio -> !dominio.isBlank())
                .collect(Collectors.toSet());
    }

    @Transactional
    public CrearUsuarioResponse crearUsuario(CrearUsuarioRequest request, UsuarioDto creador) {
        String correo = request.correo().toLowerCase();

        if (request.rol() == Rol.PROFESOR && !creador.esAdmin()) {
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

        return new CrearUsuarioResponse(usuario.getId(), usuario.getCorreo(), usuario.getRol(), contrasenaTemporal);
    }

    /**
     * Alta que se hace el propio alumnado desde la pantalla de acceso. La cuenta
     * queda inactiva hasta que un admin la apruebe, así que la contraseña temporal
     * generada aquí no se devuelve a nadie: la aprobación genera otra.
     */
    @Transactional
    public void registrarAutoservicio(RegistroRequest request, String ipRemota) {
        String correo = request.correo().toLowerCase();

        if (request.web() != null && !request.web().isBlank()) {
            log.warn("Intento de registro sospechoso (honeypot relleno): correo={} ip={}", correo, ipRemota);
            throw UsuarioException.credencialesInvalidas();
        }

        String dni = request.dni().trim().toUpperCase();
        if (!dniValido(dni)) {
            throw UsuarioException.dniInvalido();
        }
        if (!dominioPermitido(correo)) {
            throw UsuarioException.correoDominioNoPermitido();
        }
        Grado grado = gradoRepository.findById(request.gradoId()).orElseThrow(GradoException::noEncontrado);
        if (usuarioRepository.findByCorreo(correo).isPresent()) {
            throw UsuarioException.correoYaExiste();
        }

        Usuario usuario = new Usuario(correo, passwordEncoder.encode(contrasenaInicial(dni)), Rol.ALUMNO);
        usuario.setNombre(request.nombre().trim());
        usuario.setApellido1(request.apellido1().trim());
        usuario.setApellido2(request.apellido2() == null || request.apellido2().isBlank() ? null : request.apellido2().trim());
        usuario.setDni(dni);
        usuario.setGrado(grado);
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        permitirCorreo(correo);
    }

    /**
     * Apunta el correo en la whitelist. Quien se da de alta con un dominio
     * permitido queda anotado uno a uno, de forma que si el centro estrecha
     * después `allowed.domains` las cuentas que ya existían siguen entrando.
     */
    public void permitirCorreo(String correo) {
        if (!correoPermitidoRepository.existsByCorreo(correo)) {
            correoPermitidoRepository.save(new CorreoPermitido(correo));
        }
    }

    /**
     * Confirmación de una cuenta por un admin: solo la activa. Ya no toca la
     * contraseña — las cuentas que nacen con DNI (auto-registro e importación)
     * arrancan con él como contraseña, y regenerarla aquí destruiría justo la
     * que el alumno conoce. Sobre una cuenta ya activa es un no-op.
     */
    @Transactional
    public void activarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(UsuarioException::noEncontrado);
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public LoginResultado login(LoginRequest request, String ipRemota) {
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

        String token = usuario.isDebeCambiarContrasena()
                ? jwtService.generarTokenRestringido(usuario)
                : jwtService.generarTokenNormal(usuario);
        return new LoginResultado(token, UsuarioDto.de(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioDto buscarPorCorreo(String correo) {
        return UsuarioDto.de(buscarUsuarioPorCorreo(correo));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResumenDto> listar(Rol rolFiltro) {
        List<Usuario> usuarios = rolFiltro != null ? usuarioRepository.findByRol(rolFiltro) : usuarioRepository.findAll();
        return usuarios.stream().map(UsuarioResumenDto::de).toList();
    }

    @Transactional
    public List<EtiquetaDto> actualizarEtiquetas(Long id, ActualizarEtiquetasRequest request,
            boolean esProfesor, String correoAutenticado) {
        verificarPropioOProfesor(id, esProfesor, correoAutenticado);
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(UsuarioException::noEncontrado);
        usuario.setEtiquetas(buscarEtiquetas(request.etiquetaIds()));
        usuarioRepository.save(usuario);
        return usuario.getEtiquetas().stream().map(EtiquetaDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<EtiquetaDto> obtenerEtiquetas(Long id, boolean esProfesor, String correoAutenticado) {
        verificarPropioOProfesor(id, esProfesor, correoAutenticado);
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(UsuarioException::noEncontrado);
        return usuario.getEtiquetas().stream().map(EtiquetaDto::de).toList();
    }

    @Transactional
    public UsuarioGradoDto actualizarGrado(Long id, ActualizarGradoRequest request) {
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(UsuarioException::noEncontrado);
        Grado grado = gradoRepository.findById(request.gradoId()).orElseThrow(GradoException::noEncontrado);

        usuario.setGrado(grado);
        usuario.setAnio(request.anio());
        usuarioRepository.save(usuario);
        return UsuarioGradoDto.de(usuario);
    }

    @Transactional
    public void cambiarContrasena(String correo, CambiarContrasenaRequest request) {
        Usuario usuario = buscarUsuarioPorCorreo(correo);

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

    private Usuario buscarUsuarioPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo).orElseThrow(UsuarioException::credencialesInvalidas);
    }

    private void verificarPropioOProfesor(Long id, boolean esProfesor, String correoAutenticado) {
        if (!esProfesor) {
            Usuario autenticado = usuarioRepository.findByCorreo(correoAutenticado).orElseThrow();
            if (!autenticado.getId().equals(id)) {
                throw UsuarioException.accesoDenegado();
            }
        }
    }

    private List<Etiqueta> buscarEtiquetas(List<Long> ids) {
        List<Etiqueta> etiquetas = new ArrayList<>();
        if (ids != null) {
            for (Long id : ids) {
                etiquetas.add(etiquetaRepository.findById(id).orElseThrow(UsuarioException::etiquetaNoEncontrada));
            }
        }
        return etiquetas;
    }

    private boolean correoPermitido(String correo) {
        return dominioPermitido(correo) || correoPermitidoRepository.existsByCorreo(correo);
    }

    /**
     * Solo `allowed.domains`, no la tabla de correos sueltos: es la puerta por la
     * que entra alguien que todavía no está en ninguna lista (auto-registro e
     * importación de alumnado). Quien pasa por aquí acaba añadido a la whitelist.
     */
    public boolean dominioPermitido(String correo) {
        return dominiosPermitidos.contains(correo.substring(correo.indexOf('@') + 1).toLowerCase());
    }

    /** Formato del DNI, no matriculación: que la persona exista lo comprueba el centro al confirmar la cuenta. */
    public static boolean dniValido(String dni) {
        Matcher coincide = FORMATO_DNI.matcher(dni);
        return coincide.matches()
                && LETRAS_DNI.charAt(Integer.parseInt(coincide.group(1)) % 23) == coincide.group(2).charAt(0);
    }

    /**
     * Contraseña con la que nace una cuenta que trae DNI: el número sin la letra.
     * Decisión de producto del centro — es débil a propósito, para que el alumno
     * pueda entrar la primera vez sin que nadie le reparta nada. Sigue viniendo
     * con {@code debeCambiarContrasena = true}, así que solo vale para ese primer
     * acceso.
     */
    public static String contrasenaInicial(String dni) {
        return dni.substring(0, 8);
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
