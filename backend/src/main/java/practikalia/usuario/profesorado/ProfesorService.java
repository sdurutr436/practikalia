package practikalia.usuario.profesorado;

import practikalia.asignacion.Asignacion;
import practikalia.asignacion.AsignacionException;
import practikalia.asignacion.AsignacionRepository;
import practikalia.common.PaginaDto;
import practikalia.grado.Grado;
import practikalia.grado.GradoDto;
import practikalia.grado.GradoException;
import practikalia.grado.GradoRepository;
import practikalia.usuario.Rol;
import practikalia.usuario.Usuario;
import practikalia.usuario.UsuarioException;
import practikalia.usuario.UsuarioRepository;
import practikalia.usuario.UsuarioService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listado y ficha del profesorado del centro, con sus dos tutorías: la de clase
 * —que vive en {@code Grado.tutor} y alcanza a todo el alumnado de esa clase sin
 * guardar nada por alumno— y la de prácticas, que es el {@code tutorCentro} de
 * la asignación abierta de cada alumno.
 *
 * Vive en un subpaquete de {@code usuario} y no en uno propio por lo mismo que
 * {@code alumnado}: la entidad sigue siendo {@code Usuario}, aquí solo está la
 * vista "profesor" de ella.
 */
@Service
public class ProfesorService {

    private final UsuarioRepository usuarioRepository;
    private final AsignacionRepository asignacionRepository;
    private final GradoRepository gradoRepository;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;

    public ProfesorService(
            UsuarioRepository usuarioRepository,
            AsignacionRepository asignacionRepository,
            GradoRepository gradoRepository,
            UsuarioService usuarioService,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.asignacionRepository = asignacionRepository;
        this.gradoRepository = gradoRepository;
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
    }

    /** @param conClase {@code null} para la pastilla "Todos". */
    @Transactional(readOnly = true)
    public PaginaDto<ProfesorDto> listar(Boolean conClase, int pagina, int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano,
                Sort.by(Sort.Direction.ASC, "apellido1", "nombre", "correo"));
        Page<Usuario> profesores = usuarioRepository.buscarProfesorado(Rol.PROFESOR, conClase, pageable);

        List<Long> ids = profesores.getContent().stream().map(Usuario::getId).toList();
        Map<Long, Grado> clases = new HashMap<>();
        if (!ids.isEmpty()) {
            gradoRepository.findByTutorIdIn(ids).forEach(clase -> clases.put(clase.getTutor().getId(), clase));
        }
        Map<Long, Integer> practicas = contarPracticas(ids);

        return PaginaDto.de(profesores, profesor -> de(
                profesor, clases.get(profesor.getId()), practicas.getOrDefault(profesor.getId(), 0)));
    }

    /**
     * Alta a mano desde el modal del listado. Como la de alumnado: nace
     * confirmada y su contraseña inicial es el DNI sin la letra.
     */
    @Transactional
    public ProfesorDto crear(FichaProfesorRequest request) {
        String correo = request.correo().trim().toLowerCase();
        String dni = request.dni().trim().toUpperCase();

        if (!UsuarioService.dniValido(dni)) {
            throw UsuarioException.dniInvalido();
        }
        if (!usuarioService.dominioPermitido(correo)) {
            throw UsuarioException.correoDominioNoPermitido();
        }
        if (usuarioRepository.findByCorreo(correo).isPresent()) {
            throw UsuarioException.correoYaExiste();
        }
        if (usuarioRepository.existsByDni(dni)) {
            throw UsuarioException.dniYaRegistrado();
        }

        Usuario profesor = new Usuario(
                correo, passwordEncoder.encode(UsuarioService.contrasenaInicial(dni)), Rol.PROFESOR);
        profesor.setEsAdmin(Boolean.TRUE.equals(request.esAdmin()));
        aplicar(profesor, request, dni, correo);
        usuarioRepository.save(profesor);
        usuarioService.permitirCorreo(correo);

        return guardarTutorias(profesor, request);
    }

    @Transactional
    public ProfesorDto editar(Long id, FichaProfesorRequest request) {
        Usuario profesor = buscarProfesor(id);
        String correo = request.correo().trim().toLowerCase();
        String dni = request.dni().trim().toUpperCase();

        if (!UsuarioService.dniValido(dni)) {
            throw UsuarioException.dniInvalido();
        }
        usuarioRepository.findByCorreo(correo).ifPresent(otro -> {
            if (!otro.getId().equals(id)) {
                throw UsuarioException.correoYaExiste();
            }
        });

        // Un centro sin administrador no tiene quién dé de alta al siguiente:
        // el último no puede dejar de serlo.
        boolean esAdmin = Boolean.TRUE.equals(request.esAdmin());
        if (profesor.isEsAdmin() && !esAdmin && usuarioRepository.countByEsAdminTrue() <= 1) {
            throw UsuarioException.ultimoAdministrador();
        }
        profesor.setEsAdmin(esAdmin);

        aplicar(profesor, request, dni, correo);
        usuarioRepository.save(profesor);

        return guardarTutorias(profesor, request);
    }

    private ProfesorDto guardarTutorias(Usuario profesor, FichaProfesorRequest request) {
        Grado clase = asignarClase(profesor, request.gradoId());
        asignarPracticas(profesor, request.alumnosPractica());
        return de(profesor, clase,
                contarPracticas(List.of(profesor.getId())).getOrDefault(profesor.getId(), 0));
    }

    /**
     * La tutoría de clase, exclusiva por los dos lados: el profesor suelta la
     * que tuviera y la clase elegida deja de ser de quien la tuviese.
     */
    private Grado asignarClase(Usuario profesor, Long gradoId) {
        Grado anterior = gradoRepository.findByTutorId(profesor.getId()).orElse(null);
        if (anterior != null && anterior.getId().equals(gradoId)) {
            return anterior;
        }
        if (anterior != null) {
            anterior.setTutor(null);
            // A mano y antes de tomar la nueva: `tutor_id` es única y las dos
            // filas se guardarían en el mismo flush, en un orden que no elegimos.
            gradoRepository.saveAndFlush(anterior);
        }
        if (gradoId == null) {
            return null;
        }
        Grado clase = gradoRepository.findById(gradoId).orElseThrow(GradoException::noEncontrado);
        clase.setTutor(profesor);
        return gradoRepository.save(clase);
    }

    /**
     * La tutoría de prácticas: el tutor de la asignación abierta de cada alumno.
     *
     * ponytail: solo añade. Quitarle un alumno a un tutor es dárselo a otro
     * —{@code tutorCentro} no admite nulo—, así que la lista no borra a nadie.
     */
    private void asignarPracticas(Usuario profesor, List<Long> alumnoIds) {
        if (alumnoIds == null) {
            return;
        }
        for (Long alumnoId : alumnoIds) {
            Asignacion abierta = asignacionRepository.findFirstByAlumnoIdAndFechaFinIsNull(alumnoId)
                    .orElseThrow(AsignacionException::noEncontrada);
            abierta.setTutorCentro(profesor);
            asignacionRepository.save(abierta);
        }
    }

    /** Una sola consulta para toda la página: si no, cada tarjeta haría la suya. */
    private Map<Long, Integer> contarPracticas(List<Long> tutorIds) {
        Map<Long, Integer> porTutor = new HashMap<>();
        if (!tutorIds.isEmpty()) {
            asignacionRepository.findByTutorCentroIdInAndFechaFinIsNull(tutorIds).forEach(
                    asignacion -> porTutor.merge(asignacion.getTutorCentro().getId(), 1, Integer::sum));
        }
        return porTutor;
    }

    private void aplicar(Usuario profesor, FichaProfesorRequest request, String dni, String correo) {
        profesor.setNombre(request.nombre().trim());
        profesor.setApellido1(request.apellido1().trim());
        profesor.setApellido2(vacioANulo(request.apellido2()));
        profesor.setDni(dni);
        profesor.setCorreo(correo);
    }

    private Usuario buscarProfesor(Long id) {
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(UsuarioException::noEncontrado);
        if (usuario.getRol() != Rol.PROFESOR) {
            throw UsuarioException.noEncontrado();
        }
        return usuario;
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static ProfesorDto de(Usuario profesor, Grado clase, int alumnosPractica) {
        return new ProfesorDto(
                profesor.getId(),
                profesor.getNombre(),
                profesor.getApellido1(),
                profesor.getApellido2(),
                profesor.getDni(),
                profesor.getCorreo(),
                profesor.isEsAdmin(),
                clase == null ? null : GradoDto.de(clase),
                alumnosPractica);
    }
}
