package practikalia.interes;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InteresController {

    private final InteresService interesService;

    public InteresController(InteresService interesService) {
        this.interesService = interesService;
    }

    @PutMapping("/api/empresas/{empresaId}/interes")
    public ResponseEntity<Void> marcar(Authentication authentication, @PathVariable Long empresaId) {
        interesService.marcar(empresaId, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/empresas/{empresaId}/interes")
    public ResponseEntity<Void> desmarcar(Authentication authentication, @PathVariable Long empresaId) {
        interesService.desmarcar(empresaId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/empresas/{empresaId}/interesados")
    public ResponseEntity<List<InteresadoDto>> listarInteresados(@PathVariable Long empresaId) {
        return ResponseEntity.ok(interesService.listarInteresados(empresaId));
    }

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
