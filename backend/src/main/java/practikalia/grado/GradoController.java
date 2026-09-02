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

    @Operation(summary = "Listar grados sin sesión", description = "Mismo catálogo que `GET /api/grados`, pero público: "
            + "lo necesita el formulario de auto-registro, que todavía no tiene sesión. Ningún dato sensible, "
            + "el mismo `{id, nombre}` que ya ve cualquier usuario autenticado.")
    @GetMapping("/publico")
    public List<GradoDto> listarPublico() {
        return listar();
    }
}
