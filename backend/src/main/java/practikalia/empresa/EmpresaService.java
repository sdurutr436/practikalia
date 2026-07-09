package practikalia.empresa;

import practikalia.etiqueta.Etiqueta;
import practikalia.etiqueta.EtiquetaRepository;
import practikalia.usuario.Usuario;
import practikalia.usuario.UsuarioRepository;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final EtiquetaRepository etiquetaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ImagenEmpresaService imagenEmpresaService;

    public EmpresaService(
            EmpresaRepository empresaRepository,
            EtiquetaRepository etiquetaRepository,
            UsuarioRepository usuarioRepository,
            ImagenEmpresaService imagenEmpresaService) {
        this.empresaRepository = empresaRepository;
        this.etiquetaRepository = etiquetaRepository;
        this.usuarioRepository = usuarioRepository;
        this.imagenEmpresaService = imagenEmpresaService;
    }

    @Transactional(readOnly = true)
    public List<EmpresaAlumnoDto> listarParaAlumno() {
        return empresaRepository.findByPublicadaTrue().stream().map(EmpresaAlumnoDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<EmpresaProfesorDto> listarParaProfesor() {
        return empresaRepository.findAll().stream().map(EmpresaProfesorDto::de).toList();
    }

    @Transactional(readOnly = true)
    public EmpresaAlumnoDto obtenerParaAlumno(Long id) {
        Empresa empresa = empresaRepository.findById(id)
                .filter(Empresa::isPublicada)
                .orElseThrow(EmpresaException::noEncontrada);
        return EmpresaAlumnoDto.de(empresa);
    }

    @Transactional(readOnly = true)
    public EmpresaProfesorDto obtenerParaProfesor(Long id) {
        return EmpresaProfesorDto.de(buscarEmpresa(id));
    }

    @Transactional
    public EmpresaProfesorDto crear(CrearEmpresaRequest request, String correoCreador) {
        Etiqueta sector = buscarEtiqueta(request.sectorId());
        List<Etiqueta> etiquetas = buscarEtiquetas(request.etiquetaIds());
        Usuario creador = usuarioRepository.findByCorreo(correoCreador).orElseThrow(EmpresaException::noEncontrada);

        Empresa empresa = new Empresa(request.nombre(), request.descripcion(), request.direccion(), sector,
                request.observaciones(), request.contactoNombre(), request.contactoTelefono(),
                request.contactoEmail(), creador);
        empresa.setEtiquetas(etiquetas);
        empresaRepository.save(empresa);
        return EmpresaProfesorDto.de(empresa);
    }

    @Transactional
    public EmpresaProfesorDto actualizar(Long id, CrearEmpresaRequest request) {
        Empresa empresa = buscarEmpresa(id);
        Etiqueta sector = buscarEtiqueta(request.sectorId());
        List<Etiqueta> etiquetas = buscarEtiquetas(request.etiquetaIds());

        empresa.setNombre(request.nombre());
        empresa.setDescripcion(request.descripcion());
        empresa.setDireccion(request.direccion());
        empresa.setSector(sector);
        empresa.setEtiquetas(etiquetas);
        empresa.setObservaciones(request.observaciones());
        empresa.setContactoNombre(request.contactoNombre());
        empresa.setContactoTelefono(request.contactoTelefono());
        empresa.setContactoEmail(request.contactoEmail());
        empresa.setPublicada(request.publicada());
        empresaRepository.save(empresa);
        return EmpresaProfesorDto.de(empresa);
    }

    @Transactional
    public EmpresaProfesorDto actualizarImagen(Long id, MultipartFile fichero) {
        Empresa empresa = buscarEmpresa(id);
        empresa.setImagen(imagenEmpresaService.guardar(fichero));
        empresaRepository.save(empresa);
        return EmpresaProfesorDto.de(empresa);
    }

    private Empresa buscarEmpresa(Long id) {
        return empresaRepository.findById(id).orElseThrow(EmpresaException::noEncontrada);
    }

    private Etiqueta buscarEtiqueta(Long id) {
        return etiquetaRepository.findById(id).orElseThrow(EmpresaException::etiquetaNoEncontrada);
    }

    private List<Etiqueta> buscarEtiquetas(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream().map(this::buscarEtiqueta).toList();
    }
}
