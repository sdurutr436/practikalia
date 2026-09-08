package practikalia.etiqueta;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

/**
 * Catálogo de etiquetas. El listado plano lo lee cualquier autenticado (lo
 * necesitan el formulario de empresa y los intereses del alumnado); el árbol y
 * su mantenimiento son de administrador, según {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/etiquetas")
public class EtiquetaController {

    private final EtiquetaRepository etiquetaRepository;
    private final EtiquetaService etiquetaService;

    public EtiquetaController(EtiquetaRepository etiquetaRepository, EtiquetaService etiquetaService) {
        this.etiquetaRepository = etiquetaRepository;
        this.etiquetaService = etiquetaService;
    }

    @Operation(summary = "Listar etiquetas", description = "Catálogo completo y plano, sin filtros. Cualquier autenticado.")
    @GetMapping
    public List<EtiquetaDto> listar() {
        return etiquetaRepository.findAll().stream().map(EtiquetaDto::de).toList();
    }

    @Operation(summary = "Árbol del catálogo", description = "Sectores y grupos transversales con sus actividades "
            + "y etiquetas anidadas. Los sectores primero y los grupos transversales al final. Solo admin.")
    @GetMapping("/arbol")
    public List<NodoDto> arbol() {
        return etiquetaService.arbol();
    }

    @Operation(summary = "Crear sector o etiqueta", description = "Sin `padreId` nace como sector, o como grupo "
            + "transversal si `transversal`. Con padre hereda el nivel: actividad bajo un sector, etiqueta bajo "
            + "una actividad. Solo admin.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NodoDto crear(@Valid @RequestBody CrearEtiquetaRequest peticion) {
        return etiquetaService.crear(peticion);
    }

    @Operation(summary = "Renombrar sector o etiqueta", description = "Solo el nombre; el nodo no cambia de rama. Solo admin.")
    @PutMapping("/{id}")
    public NodoDto renombrar(@PathVariable Long id, @Valid @RequestBody RenombrarEtiquetaRequest peticion) {
        return etiquetaService.renombrar(id, peticion);
    }

    @Operation(summary = "Borrar sector o etiqueta", description = "Solo si no le cuelga nada y no la usa "
            + "ninguna empresa ni ningún alumno. Solo admin.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrar(@PathVariable Long id) {
        etiquetaService.borrar(id);
    }
}
