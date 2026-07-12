package practikalia.afinidad;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AfinidadController {

    private final AfinidadService afinidadService;

    public AfinidadController(AfinidadService afinidadService) {
        this.afinidadService = afinidadService;
    }

    @GetMapping("/api/empresas/afinidad")
    public ResponseEntity<AfinidadListadoDto> propia(Authentication authentication) {
        return ResponseEntity.ok(afinidadService.obtenerPropia(authentication.getName()));
    }

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
