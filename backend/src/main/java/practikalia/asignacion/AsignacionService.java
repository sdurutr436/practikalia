package practikalia.asignacion;

import practikalia.empresa.Empresa;
import practikalia.empresa.EmpresaException;
import practikalia.empresa.EmpresaRepository;
import practikalia.usuario.Rol;
import practikalia.usuario.Usuario;
import practikalia.usuario.UsuarioException;
import practikalia.usuario.UsuarioRepository;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AsignacionService {

    private final AsignacionRepository asignacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;

    public AsignacionService(
            AsignacionRepository asignacionRepository,
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository) {
        this.asignacionRepository = asignacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
    }

    @Transactional
    public AsignacionDto crear(CrearAsignacionRequest request) {
        Usuario alumno = buscarAlumno(request.alumnoId());
        Usuario tutorCentro = buscarTutor(request.tutorCentroId());
        Empresa empresa = empresaRepository.findById(request.empresaId()).orElseThrow(EmpresaException::noEncontrada);

        if (asignacionRepository.existsByAlumnoIdAndEmpresaId(alumno.getId(), empresa.getId())) {
            throw AsignacionException.yaExiste();
        }

        Asignacion asignacion = new Asignacion(alumno, empresa, tutorCentro, request.fechaInicio());
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

    @Transactional
    public AsignacionDto cerrar(Long id, ActualizarAsignacionRequest request) {
        Asignacion asignacion = asignacionRepository.findById(id).orElseThrow(AsignacionException::noEncontrada);
        asignacion.setFechaFin(request.fechaFin());
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

    private Usuario buscarTutor(Long id) {
        Usuario tutor = usuarioRepository.findById(id).orElseThrow(AsignacionException::tutorNoEncontrado);
        if (tutor.getRol() != Rol.PROFESOR) {
            throw AsignacionException.tutorInvalido();
        }
        return tutor;
    }
}
