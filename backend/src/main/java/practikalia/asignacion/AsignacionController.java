package practikalia.asignacion;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
public class AsignacionController {

    private final AsignacionService asignacionService;

    public AsignacionController(AsignacionService asignacionService) {
        this.asignacionService = asignacionService;
    }

    @PostMapping("/api/asignaciones")
    public ResponseEntity<AsignacionDto> crear(@Valid @RequestBody CrearAsignacionRequest request) {
        AsignacionDto response = asignacionService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/alumnos/{alumnoId}/asignaciones")
    public ResponseEntity<?> listarPorAlumno(Authentication authentication, @PathVariable Long alumnoId) {
        return ResponseEntity.ok(
                asignacionService.listarPorAlumno(alumnoId, esProfesor(authentication), authentication.getName()));
    }

    @GetMapping("/api/empresas/{empresaId}/asignaciones")
    public ResponseEntity<?> listarPorEmpresa(@PathVariable Long empresaId) {
        return ResponseEntity.ok(asignacionService.listarPorEmpresa(empresaId));
    }

    @PutMapping("/api/asignaciones/{id}")
    public ResponseEntity<AsignacionDto> cerrar(@PathVariable Long id, @Valid @RequestBody ActualizarAsignacionRequest request) {
        return ResponseEntity.ok(asignacionService.cerrar(id, request));
    }

    private boolean esProfesor(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_PROFESOR"));
    }
}
