package practikalia.asignacion;

import practikalia.empresa.Empresa;
import practikalia.empresa.EmpresaException;
import practikalia.empresa.EmpresaRepository;
import practikalia.grado.Grado;
import practikalia.grado.GradoException;
import practikalia.grado.GradoRepository;
import practikalia.usuario.Rol;
import practikalia.usuario.Usuario;
import practikalia.usuario.UsuarioException;
import practikalia.usuario.UsuarioRepository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AsignacionService {

    private final AsignacionRepository asignacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final GradoRepository gradoRepository;

    public AsignacionService(
            AsignacionRepository asignacionRepository,
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository,
            GradoRepository gradoRepository) {
        this.asignacionRepository = asignacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.gradoRepository = gradoRepository;
    }

    @Transactional
    public AsignacionDto crear(CrearAsignacionRequest request) {
        Usuario alumno = buscarAlumno(request.alumnoId());
        Usuario tutorCentro = buscarTutor(request.tutorCentroId());
        Empresa empresa = empresaRepository.findById(request.empresaId()).orElseThrow(EmpresaException::noEncontrada);
        Grado grado = gradoRepository.findById(request.gradoId()).orElseThrow(GradoException::noEncontrado);

        exigirNoRepetida(alumno.getId(), empresa.getId(), grado.getId(), request.anio());

        Asignacion asignacion = new Asignacion(alumno, empresa, tutorCentro, grado, request.anio(), request.fechaInicio());
        asignacionRepository.save(asignacion);
        return AsignacionDto.de(asignacion);
    }

    /**
     * Pone empresa a un alumno desde la pantalla de asignaciones. Si ya tenía
     * una asignación abierta se le cambia la empresa en vez de cerrarla y abrir
     * otra: equivocarse de opción en un desplegable es un error de tecleo, no un
     * cambio de empresa real, y el histórico no debería recoger la equivocación.
     * Si no tenía ninguna, nace una nueva con quien llama como tutor, la clase
     * del alumno y el curso en marcha.
     */
    @Transactional
    public AsignacionDto asignar(Long alumnoId, Long empresaId, String correoProfesor) {
        Usuario alumno = buscarAlumno(alumnoId);
        Empresa empresa = empresaRepository.findById(empresaId).orElseThrow(EmpresaException::noEncontrada);
        if (!empresa.isPublicada()) {
            throw AsignacionException.empresaNoPublicada();
        }

        Asignacion abierta = asignacionRepository.findFirstByAlumnoIdAndFechaFinIsNull(alumnoId).orElse(null);
        if (abierta != null) {
            if (!abierta.getEmpresa().getId().equals(empresaId)) {
                exigirNoRepetida(alumnoId, empresaId, abierta.getGrado().getId(), abierta.getAnio());
                abierta.setEmpresa(empresa);
                asignacionRepository.save(abierta);
            }
            return AsignacionDto.de(abierta);
        }

        // El grado es el snapshot de la asignación y no admite nulos: sin clase
        // no hay nada que fotografiar, y el mensaje dice dónde se arregla.
        if (alumno.getGrado() == null) {
            throw AsignacionException.alumnoSinClase();
        }
        int curso = Curso.actual();
        exigirNoRepetida(alumnoId, empresaId, alumno.getGrado().getId(), curso);

        Asignacion asignacion = new Asignacion(
                alumno, empresa, buscarTutor(correoProfesor), alumno.getGrado(), curso, LocalDate.now());
        asignacionRepository.save(asignacion);
        return AsignacionDto.de(asignacion);
    }

    @Transactional(readOnly = true)
    public List<AsignacionDto> listarPorAlumno(Long alumnoId, boolean esProfesor, String correoAutenticado) {
        if (!esProfesor) {
            Usuario autenticado = usuarioRepository.findByCorreo(correoAutenticado).orElseThrow();
            if (!autenticado.getId().equals(alumnoId)) {
                throw UsuarioException.accesoDenegado();
            }
        }
        return asignacionRepository.findByAlumnoId(alumnoId).stream().map(AsignacionDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<AsignacionDto> listarPorEmpresa(Long empresaId) {
        return asignacionRepository.findByEmpresaId(empresaId).stream().map(AsignacionDto::de).toList();
    }

    @Transactional(readOnly = true)
    public TasaContratacionDto tasaContratacion(Long empresaId) {
        if (!empresaRepository.existsById(empresaId)) {
            throw EmpresaException.noEncontrada();
        }
        long decididas = asignacionRepository.countByEmpresaIdAndFechaFinIsNotNullAndContratadoPosteriorIsNotNull(empresaId);
        long contrataciones = asignacionRepository.countByEmpresaIdAndFechaFinIsNotNullAndContratadoPosteriorTrue(empresaId);
        double tasa = decididas == 0 ? 0.0 : (double) contrataciones / decididas;
        return new TasaContratacionDto(empresaId, decididas, contrataciones, tasa);
    }

    @Transactional
    public AsignacionDto cerrar(Long id, ActualizarAsignacionRequest request) {
        Asignacion asignacion = asignacionRepository.findById(id).orElseThrow(AsignacionException::noEncontrada);
        asignacion.setFechaFin(request.fechaFin());
        asignacion.setContratadoPosterior(request.contratadoPosterior());
        asignacionRepository.save(asignacion);
        return AsignacionDto.de(asignacion);
    }

    private Usuario buscarAlumno(Long id) {
        Usuario alumno = usuarioRepository.findById(id).orElseThrow(AsignacionException::alumnoNoEncontrado);
        if (alumno.getRol() != Rol.ALUMNO) {
            throw AsignacionException.alumnoInvalido();
        }
        return alumno;
    }

    /** El tutor de una asignación creada desde la pantalla: siempre quien la crea. */
    private Usuario buscarTutor(String correo) {
        Usuario tutor = usuarioRepository.findByCorreo(correo).orElseThrow(AsignacionException::tutorNoEncontrado);
        if (tutor.getRol() != Rol.PROFESOR) {
            throw AsignacionException.tutorInvalido();
        }
        return tutor;
    }

    private void exigirNoRepetida(Long alumnoId, Long empresaId, Long gradoId, int anio) {
        if (asignacionRepository.existsByAlumnoIdAndEmpresaIdAndGradoIdAndAnio(alumnoId, empresaId, gradoId, anio)) {
            throw AsignacionException.yaExiste();
        }
    }

    private Usuario buscarTutor(Long id) {
        Usuario tutor = usuarioRepository.findById(id).orElseThrow(AsignacionException::tutorNoEncontrado);
        if (tutor.getRol() != Rol.PROFESOR) {
            throw AsignacionException.tutorInvalido();
        }
        return tutor;
    }
}
