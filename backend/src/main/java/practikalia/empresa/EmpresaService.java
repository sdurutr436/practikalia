package practikalia.empresa;

import practikalia.common.PaginaDto;
import practikalia.etiqueta.Etiqueta;
import practikalia.etiqueta.EtiquetaRepository;
import practikalia.usuario.Usuario;
import practikalia.usuario.UsuarioRepository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

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
    public PaginaDto<EmpresaAlumnoDto> listarParaAlumno(EmpresaFiltroDto filtro) {
        // El alumnado solo ve publicadas, mande lo que mande en el filtro.
        return PaginaDto.de(buscar(filtro, true), EmpresaAlumnoDto::de);
    }

    @Transactional(readOnly = true)
    public PaginaDto<EmpresaProfesorDto> listarParaProfesor(EmpresaFiltroDto filtro) {
        return PaginaDto.de(buscar(filtro, false), EmpresaProfesorDto::de);
    }

    private Page<Empresa> buscar(EmpresaFiltroDto filtro, boolean soloPublicadas) {
        // Orden fijo por nombre: sin él, la página 2 puede repetir u omitir filas.
        Sort porNombre = Sort.by("nombre");
        Pageable pagina = filtro.tamano() == null
                ? Pageable.unpaged(porNombre)
                : PageRequest.of(filtro.pagina(), filtro.tamano(), porNombre);
        return empresaRepository.findAll(criterios(filtro, soloPublicadas), pagina);
    }

    /**
     * Traduce el filtro a predicados. Todo lo que venga a null se cae solo, así
     * que el mismo método sirve para el listado sin filtrar.
     */
    private static Specification<Empresa> criterios(EmpresaFiltroDto filtro, boolean soloPublicadas) {
        return (raiz, consulta, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            Boolean publicada = soloPublicadas ? Boolean.TRUE : filtro.publicada();
            if (publicada != null) {
                predicados.add(cb.equal(raiz.get("publicada"), publicada));
            }
            if (filtro.sectorId() != null) {
                predicados.add(cb.equal(raiz.get("sector").get("id"), filtro.sectorId()));
            }
            boolean porEtiquetas = filtro.etiquetaIds() != null && !filtro.etiquetaIds().isEmpty();
            boolean porTexto = filtro.texto() != null && !filtro.texto().isBlank();
            if (porEtiquetas || porTexto) {
                // Una sola unión a etiquetas para las dos condiciones, y distinct
                // porque una empresa con varias etiquetas saldría repetida.
                var etiquetas = raiz.join("etiquetas", JoinType.LEFT);
                consulta.distinct(true);
                if (porEtiquetas) {
                    predicados.add(etiquetas.get("id").in(filtro.etiquetaIds()));
                }
                if (porTexto) {
                    // ponytail: LIKE en minúsculas, sin quitar acentos. Si hace falta
                    // que "diseno" encuentre "Diseño", aquí entra un translate/unaccent.
                    String patron = "%" + filtro.texto().toLowerCase() + "%";
                    predicados.add(cb.or(
                            cb.like(cb.lower(raiz.get("nombre")), patron),
                            cb.like(cb.lower(cb.coalesce(raiz.get("descripcion"), "")), patron),
                            cb.like(cb.lower(raiz.get("sector").get("nombre")), patron),
                            cb.like(cb.lower(cb.coalesce(etiquetas.get("nombre"), "")), patron)));
                }
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
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
        Usuario creador = usuarioRepository.findByCorreo(correoCreador).orElseThrow();

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
        List<Etiqueta> etiquetas = new ArrayList<>();
        if (ids != null) {
            for (Long id : ids) {
                etiquetas.add(buscarEtiqueta(id));
            }
        }
        return etiquetas;
    }
}
