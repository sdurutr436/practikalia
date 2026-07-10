package practikalia.interes;

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
public class InteresService {

    private final InteresRepository interesRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;

    public InteresService(
            InteresRepository interesRepository,
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository) {
        this.interesRepository = interesRepository;
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
    }

    @Transactional
    public void marcar(Long empresaId, String correoAutenticado) {
        Usuario alumno = buscarAlumnoConGrado(correoAutenticado);
        Empresa empresa = empresaRepository.findById(empresaId)
                .filter(Empresa::isPublicada)
                .orElseThrow(EmpresaException::noEncontrada);

        if (interesRepository.findByAlumnoIdAndEmpresaIdAndGradoIdAndAnio(
                alumno.getId(), empresa.getId(), alumno.getGrado().getId(), alumno.getAnio()).isEmpty()) {
            interesRepository.save(new Interes(alumno, empresa, alumno.getGrado(), alumno.getAnio()));
        }
    }

    @Transactional
    public void desmarcar(Long empresaId, String correoAutenticado) {
        Usuario alumno = usuarioRepository.findByCorreo(correoAutenticado).orElseThrow();
        if (alumno.getRol() != Rol.ALUMNO) {
            throw UsuarioException.accesoDenegado();
        }
        if (alumno.getGrado() == null || alumno.getAnio() == null) {
            return; // sin grado no hay marca del año actual que quitar — idempotente
        }
        interesRepository.findByAlumnoIdAndEmpresaIdAndGradoIdAndAnio(
                alumno.getId(), empresaId, alumno.getGrado().getId(), alumno.getAnio())
                .ifPresent(interesRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<InteresadoDto> listarInteresados(Long empresaId) {
        return interesRepository.findByEmpresaId(empresaId).stream().map(InteresadoDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<InteresDto> listarPorAlumno(Long alumnoId, boolean esProfesor, String correoAutenticado) {
        if (!esProfesor) {
            Usuario autenticado = usuarioRepository.findByCorreo(correoAutenticado).orElseThrow();
            if (!autenticado.getId().equals(alumnoId)) {
                throw UsuarioException.accesoDenegado();
            }
        }
        return interesRepository.findByAlumnoId(alumnoId).stream().map(InteresDto::de).toList();
    }

    private Usuario buscarAlumnoConGrado(String correo) {
        Usuario alumno = usuarioRepository.findByCorreo(correo).orElseThrow();
        if (alumno.getRol() != Rol.ALUMNO) {
            throw UsuarioException.accesoDenegado();
        }
        if (alumno.getGrado() == null || alumno.getAnio() == null) {
            throw InteresException.alumnoSinGrado();
        }
        return alumno;
    }
}
