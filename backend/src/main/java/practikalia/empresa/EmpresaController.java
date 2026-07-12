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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

/**
 * Empresas de prácticas. Lectura (`GET`) abierta a cualquier rol autenticado,
 * pero con forma distinta según quien mira: el alumnado solo ve empresas
 * `publicada=true` y un subconjunto de campos ({@link EmpresaAlumnoDto}); el
 * profesorado ve todas y el detalle completo ({@link EmpresaProfesorDto}).
 * Escritura (`POST`/`PUT`) restringida a profesor/admin en {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @Operation(summary = "Listar empresas", description = "El alumnado solo ve las publicadas; el profesorado ve todas, publicadas o no.")
    @GetMapping
    public ResponseEntity<?> listar(Authentication authentication) {
        return esProfesor(authentication)
                ? ResponseEntity.ok(empresaService.listarParaProfesor())
                : ResponseEntity.ok(empresaService.listarParaAlumno());
    }

    @Operation(summary = "Consultar una empresa", description = "Un alumno pidiendo una empresa no publicada recibe 404, "
            + "igual que si no existiera (no revela su existencia).")
    @ApiResponse(responseCode = "404", description = "La empresa no existe, o no está publicada y quien pregunta no es profesor/admin")
    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(Authentication authentication, @PathVariable Long id) {
        return esProfesor(authentication)
                ? ResponseEntity.ok(empresaService.obtenerParaProfesor(id))
                : ResponseEntity.ok(empresaService.obtenerParaAlumno(id));
    }

    @Operation(summary = "Crear una empresa", description = "Solo profesor/admin. Nace siempre con `publicada=false`, "
            + "ignorando ese campo del request; para publicarla hace falta un `PUT` posterior.")
    @ApiResponse(responseCode = "400", description = "El sector o alguna de las etiquetas indicadas no existe")
    @PostMapping
    public ResponseEntity<EmpresaProfesorDto> crear(Authentication authentication, @Valid @RequestBody CrearEmpresaRequest request) {
        EmpresaProfesorDto response = empresaService.crear(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Actualizar una empresa", description = "Solo profesor/admin. Reemplazo completo de los campos "
            + "editables, incluido `publicada` (a diferencia de la creación, aquí sí se honra).")
    @ApiResponse(responseCode = "404", description = "La empresa no existe")
    @ApiResponse(responseCode = "400", description = "El sector o alguna de las etiquetas indicadas no existe")
    @PutMapping("/{id}")
    public ResponseEntity<EmpresaProfesorDto> actualizar(@PathVariable Long id, @Valid @RequestBody CrearEmpresaRequest request) {
        return ResponseEntity.ok(empresaService.actualizar(id, request));
    }

    @Operation(summary = "Subir la imagen de una empresa", description = "Solo profesor/admin. Valida el formato por la "
            + "firma real de bytes del fichero (JPEG/PNG/WebP), no por extensión ni Content-Type declarado; máximo 5 MB.")
    @ApiResponse(responseCode = "404", description = "La empresa no existe")
    @ApiResponse(responseCode = "400", description = "El fichero no es una imagen JPEG/PNG/WebP válida, o supera los 5 MB")
    @PostMapping(value = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EmpresaProfesorDto> subirImagen(@PathVariable Long id, @RequestParam("fichero") MultipartFile fichero) {
        return ResponseEntity.ok(empresaService.actualizarImagen(id, fichero));
    }

    private boolean esProfesor(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_PROFESOR"));
    }
}
