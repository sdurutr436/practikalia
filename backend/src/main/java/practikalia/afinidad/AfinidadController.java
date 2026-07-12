package practikalia.afinidad;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * Afinidad alumno-empresa (MVP): score de solapamiento entre las etiquetas
 * de interés del alumno y las de cada empresa publicada, más un bonus si
 * coincide el sector. Cálculo en memoria, sin caché ni entidad propia.
 */
@RestController
public class AfinidadController {

    private final AfinidadService afinidadService;

    public AfinidadController(AfinidadService afinidadService) {
        this.afinidadService = afinidadService;
    }

    @Operation(summary = "Afinidad propia con las empresas", description = "Solo alumno. Sin etiquetas de interés "
            + "marcadas, todas las empresas puntúan `0.0` y el listado queda ordenado alfabéticamente.")
    @GetMapping("/api/empresas/afinidad")
    public ResponseEntity<AfinidadListadoDto> propia(Authentication authentication) {
        return ResponseEntity.ok(afinidadService.obtenerPropia(authentication.getName()));
    }

    @Operation(summary = "Afinidad de un alumno con las empresas", description = "Solo profesor/admin. Un profesor no "
            + "admin necesita ser tutor de centro de una asignación activa (sin `fechaFin`) de ese alumno; un admin no "
            + "tiene esa restricción.")
    @ApiResponse(responseCode = "403", description = "Quien pregunta es profesor pero no tutor de ninguna asignación activa de ese alumno")
    @ApiResponse(responseCode = "404", description = "El alumno no existe (comprobado tras la tutoría, no antes: no filtra existencia)")
    @GetMapping("/api/alumnos/{alumnoId}/afinidad")
    public ResponseEntity<AfinidadListadoDto> deAlumno(Authentication authentication, @PathVariable Long alumnoId) {
        return ResponseEntity.ok(
                afinidadService.obtenerDeAlumno(alumnoId, esAdmin(authentication), authentication.getName()));
    }

    private boolean esAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ADMIN"));
    }
}
