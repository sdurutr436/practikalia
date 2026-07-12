package practikalia.afinidad;

import practikalia.asignacion.AsignacionRepository;
import practikalia.empresa.Empresa;
import practikalia.empresa.EmpresaAlumnoDto;
import practikalia.empresa.EmpresaRepository;
import practikalia.etiqueta.Etiqueta;
import practikalia.etiqueta.EtiquetaDto;
import practikalia.usuario.Usuario;
import practikalia.usuario.UsuarioException;
import practikalia.usuario.UsuarioRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AfinidadService {

    // Constante provisional — la Fase 10 la sustituye por un peso real (w2·sector)
    static final double BONUS_SECTOR = 0.2;

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final AsignacionRepository asignacionRepository;

    public AfinidadService(
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository,
            AsignacionRepository asignacionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.asignacionRepository = asignacionRepository;
    }

    @Transactional(readOnly = true)
    public AfinidadListadoDto obtenerPropia(String correoAutenticado) {
        Usuario alumno = usuarioRepository.findByCorreo(correoAutenticado).orElseThrow();
        return calcular(alumno);
    }

    @Transactional(readOnly = true)
    public AfinidadListadoDto obtenerDeAlumno(Long alumnoId, boolean esAdmin, String correoAutenticado) {
        if (!esAdmin) {
            Usuario autenticado = usuarioRepository.findByCorreo(correoAutenticado).orElseThrow();
            if (!asignacionRepository.existsByAlumnoIdAndTutorCentroIdAndFechaFinIsNull(alumnoId, autenticado.getId())) {
                throw UsuarioException.accesoDenegado();
            }
        }
        Usuario alumno = usuarioRepository.findById(alumnoId).orElseThrow(UsuarioException::noEncontrado);
        return calcular(alumno);
    }

    private AfinidadListadoDto calcular(Usuario alumno) {
        // Sin etiquetas todos los scores son 0.0 y el desempate por nombre deja el listado alfabético
        List<AfinidadEmpresaDto> empresas = empresaRepository.findByPublicadaTrue().stream()
                .map(empresa -> calcularScore(alumno, empresa))
                .sorted(Comparator.comparingDouble(AfinidadEmpresaDto::score).reversed()
                        .thenComparing(dto -> dto.empresa().nombre()))
                .toList();
        return new AfinidadListadoDto(!alumno.getEtiquetas().isEmpty(), empresas);
    }

    // Coeficiente de solapamiento |A∩B| / min(|A|,|B|), no Jaccard (c_fable.md, Fase 9)
    AfinidadEmpresaDto calcularScore(Usuario alumno, Empresa empresa) {
        Set<Long> etiquetasAlumno = alumno.getEtiquetas().stream().map(Etiqueta::getId).collect(Collectors.toSet());
        List<EtiquetaDto> coincidentes = empresa.getEtiquetas().stream()
                .filter(etiqueta -> etiquetasAlumno.contains(etiqueta.getId()))
                .map(EtiquetaDto::de)
                .toList();
        int minimo = Math.min(etiquetasAlumno.size(), empresa.getEtiquetas().size());
        double solapamiento = minimo == 0 ? 0.0 : (double) coincidentes.size() / minimo;
        boolean sectorCoincide = etiquetasAlumno.contains(empresa.getSector().getId());
        return new AfinidadEmpresaDto(EmpresaAlumnoDto.de(empresa),
                solapamiento + (sectorCoincide ? BONUS_SECTOR : 0.0), coincidentes, sectorCoincide);
    }
}
