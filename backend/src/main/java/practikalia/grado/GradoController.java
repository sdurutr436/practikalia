package practikalia.grado;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

/**
 * Catálogo de grados/ciclos. Solo lectura: el alta/baja sigue gestionándose
 * directamente en base de datos por cada centro, fuera de la app. Restringido
 * a profesor/admin en {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/grados")
public class GradoController {

    private final GradoRepository gradoRepository;

    public GradoController(GradoRepository gradoRepository) {
        this.gradoRepository = gradoRepository;
    }

    @Operation(summary = "Listar grados", description = "Catálogo completo, sin filtros. Solo profesor/admin.")
    @GetMapping
    public List<GradoDto> listar() {
        return gradoRepository.findAll().stream().map(GradoDto::de).toList();
    }
}
