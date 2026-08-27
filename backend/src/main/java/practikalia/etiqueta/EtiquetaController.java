package practikalia.etiqueta;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

/**
 * Catálogo de etiquetas. Solo lectura: el alta/baja sigue gestionándose
 * directamente en base de datos por cada centro, fuera de la app. Restringido
 * a profesor/admin en {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/etiquetas")
public class EtiquetaController {

    private final EtiquetaRepository etiquetaRepository;

    public EtiquetaController(EtiquetaRepository etiquetaRepository) {
        this.etiquetaRepository = etiquetaRepository;
    }

    @Operation(summary = "Listar etiquetas", description = "Catálogo completo, sin filtros. Solo profesor/admin.")
    @GetMapping
    public List<EtiquetaDto> listar() {
        return etiquetaRepository.findAll().stream().map(EtiquetaDto::de).toList();
    }
}
