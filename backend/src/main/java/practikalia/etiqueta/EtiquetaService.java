package practikalia.etiqueta;

import practikalia.empresa.EmpresaRepository;
import practikalia.usuario.UsuarioRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mantenimiento del catálogo de sectores, actividades y etiquetas. Solo lo
 * usa la pantalla de administración: el resto de la aplicación sigue leyendo
 * el catálogo plano de {@link EtiquetaController#listar()}.
 */
@Service
public class EtiquetaService {

    /** Sector (0) → actividad (1) → etiqueta (2). Colgar de una etiqueta daría un cuarto nivel. */
    private static final int NIVEL_HOJA = 2;

    /** Las raíces salen con los sectores primero y los grupos transversales al final. */
    private static final Comparator<Etiqueta> ORDEN =
            Comparator.comparing(Etiqueta::isTransversal).thenComparing(Etiqueta::getNombre);

    private final EtiquetaRepository etiquetaRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;

    public EtiquetaService(EtiquetaRepository etiquetaRepository, EmpresaRepository empresaRepository,
            UsuarioRepository usuarioRepository) {
        this.etiquetaRepository = etiquetaRepository;
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * El catálogo entero anidado. Se arma en memoria a partir de un único
     * {@code findAll}: son decenas de filas, no miles, y así la pantalla no
     * pide una consulta por columna.
     */
    @Transactional(readOnly = true)
    public List<NodoDto> arbol() {
        List<Etiqueta> todas = etiquetaRepository.findAll();
        Map<Long, List<Etiqueta>> porPadre = todas.stream()
                .filter(etiqueta -> etiqueta.getPadre() != null)
                .collect(Collectors.groupingBy(etiqueta -> etiqueta.getPadre().getId()));
        return todas.stream()
                .filter(etiqueta -> etiqueta.getPadre() == null)
                .sorted(ORDEN)
                .map(raiz -> nodo(raiz, porPadre))
                .toList();
    }

    @Transactional
    public NodoDto crear(CrearEtiquetaRequest peticion) {
        String nombre = peticion.nombre().trim();
        exigirNombreLibre(nombre);
        Etiqueta padre = peticion.padreId() == null ? null : buscar(peticion.padreId());
        if (padre != null && padre.nivel() >= NIVEL_HOJA) {
            throw EtiquetaException.nivelMaximo();
        }
        // Colgando de un grupo transversal la marca sobra: la rama entera ya lo es.
        boolean transversal = padre == null && Boolean.TRUE.equals(peticion.transversal());
        return nodo(etiquetaRepository.save(new Etiqueta(nombre, padre, transversal)), Map.of());
    }

    @Transactional
    public NodoDto renombrar(Long id, RenombrarEtiquetaRequest peticion) {
        Etiqueta etiqueta = buscar(id);
        String nombre = peticion.nombre().trim();
        if (!etiqueta.getNombre().equalsIgnoreCase(nombre)) {
            exigirNombreLibre(nombre);
        }
        etiqueta.setNombre(nombre);
        return nodo(etiquetaRepository.save(etiqueta), Map.of());
    }

    /**
     * Solo si está libre: ni le cuelga nada, ni la usa una empresa como sector
     * o como etiqueta, ni la tiene ningún alumno entre sus intereses. Borrar en
     * cascada aquí vaciaría de golpe la clasificación de media aplicación.
     */
    @Transactional
    public void borrar(Long id) {
        Etiqueta etiqueta = buscar(id);
        if (etiquetaRepository.existsByPadreId(id)) {
            throw EtiquetaException.conHijas();
        }
        if (empresaRepository.existsBySectorId(id)
                || empresaRepository.existsByEtiquetasId(id)
                || usuarioRepository.existsByEtiquetasId(id)) {
            throw EtiquetaException.enUso();
        }
        etiquetaRepository.delete(etiqueta);
    }

    private Etiqueta buscar(Long id) {
        return etiquetaRepository.findById(id).orElseThrow(EtiquetaException::noEncontrada);
    }

    private void exigirNombreLibre(String nombre) {
        if (etiquetaRepository.existsByNombreIgnoreCase(nombre)) {
            throw EtiquetaException.nombreRepetido();
        }
    }

    private NodoDto nodo(Etiqueta etiqueta, Map<Long, List<Etiqueta>> porPadre) {
        List<NodoDto> hijas = porPadre.getOrDefault(etiqueta.getId(), List.of()).stream()
                .sorted(ORDEN)
                .map(hija -> nodo(hija, porPadre))
                .toList();
        return new NodoDto(etiqueta.getId(), etiqueta.getNombre(), etiqueta.isTransversal(), hijas);
    }
}
