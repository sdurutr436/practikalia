package practikalia.interes;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * Interés de un alumno por una empresa. Solo alumno (marcar/desmarcar,
 * restringido a {@code ROLE_ALUMNO} en {@code SecurityConfig}); grado/año se
 * toman siempre del perfil actual del alumno al marcar, como snapshot —no
 * se actualizan si el alumno cambia de grado/año después.
 */
@RestController
public class InteresController {

    private final InteresService interesService;

    public InteresController(InteresService interesService) {
        this.interesService = interesService;
    }

    @Operation(summary = "Marcar interés en una empresa", description = "Solo alumno. Idempotente sobre el (grado, año) "
            + "actual del perfil del alumno: repetir la llamada no crea duplicados.")
    @ApiResponse(responseCode = "400", description = "El alumno no tiene grado/año establecidos en su perfil")
    @ApiResponse(responseCode = "404", description = "La empresa no existe, o no está publicada")
    @PutMapping("/api/empresas/{empresaId}/interes")
    public ResponseEntity<Void> marcar(Authentication authentication, @PathVariable Long empresaId) {
        interesService.marcar(empresaId, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Desmarcar interés en una empresa", description = "Solo alumno. Solo quita la marca del "
            + "(grado, año) actual del alumno; el histórico de años anteriores no se toca. Idempotente: sin perfil "
            + "de grado, o sin marca previa, no falla.")
    @DeleteMapping("/api/empresas/{empresaId}/interes")
    public ResponseEntity<Void> desmarcar(Authentication authentication, @PathVariable Long empresaId) {
        interesService.desmarcar(empresaId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar alumnos interesados en una empresa", description = "Solo profesor/admin. Incluye el histórico completo (todos los años).")
    @GetMapping("/api/empresas/{empresaId}/interesados")
    public ResponseEntity<List<InteresadoDto>> listarInteresados(@PathVariable Long empresaId) {
        return ResponseEntity.ok(interesService.listarInteresados(empresaId));
    }

    @Operation(summary = "Listar los intereses de un alumno", description = "Accesible por el propio alumno o por cualquier profesor/admin. Incluye el histórico completo (todos los años).")
    @ApiResponse(responseCode = "403", description = "Un alumno intenta consultar los intereses de otro alumno")
    @GetMapping("/api/alumnos/{alumnoId}/intereses")
    public ResponseEntity<List<InteresDto>> listarPorAlumno(Authentication authentication, @PathVariable Long alumnoId) {
        return ResponseEntity.ok(
                interesService.listarPorAlumno(alumnoId, esProfesor(authentication), authentication.getName()));
    }

    private boolean esProfesor(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_PROFESOR"));
    }
}
