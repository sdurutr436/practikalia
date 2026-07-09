package practikalia.empresa;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @GetMapping
    public ResponseEntity<?> listar(Authentication authentication) {
        return esProfesor(authentication)
                ? ResponseEntity.ok(empresaService.listarParaProfesor())
                : ResponseEntity.ok(empresaService.listarParaAlumno());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(Authentication authentication, @PathVariable Long id) {
        return esProfesor(authentication)
                ? ResponseEntity.ok(empresaService.obtenerParaProfesor(id))
                : ResponseEntity.ok(empresaService.obtenerParaAlumno(id));
    }

    @PostMapping
    public ResponseEntity<EmpresaProfesorDto> crear(Authentication authentication, @Valid @RequestBody CrearEmpresaRequest request) {
        EmpresaProfesorDto response = empresaService.crear(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmpresaProfesorDto> actualizar(@PathVariable Long id, @Valid @RequestBody CrearEmpresaRequest request) {
        return ResponseEntity.ok(empresaService.actualizar(id, request));
    }

    @PostMapping(value = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EmpresaProfesorDto> subirImagen(@PathVariable Long id, @RequestParam("fichero") MultipartFile fichero) {
        return ResponseEntity.ok(empresaService.actualizarImagen(id, fichero));
    }

    private boolean esProfesor(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_PROFESOR"));
    }
}
