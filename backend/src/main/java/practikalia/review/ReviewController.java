package practikalia.review;

import practikalia.common.PaginaDto;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

/**
 * Reviews de alumnos sobre empresas, colgadas de una {@code Asignacion} (de
 * ahí se derivan alumno/empresa/grado/año). Una review de alumno nace
 * `PENDIENTE` y requiere moderación de profesor; una de profesor (el tutor de
 * la propia asignación) nace ya `APROBADA`. El alumnado solo ve las
 * `APROBADA` de una empresa, salvo las suyas propias.
 */
@RestController
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "Crear una review", description = "El autor debe ser el alumno de la asignación, o el profesor "
            + "tutor de esa misma asignación. Una review de alumno nace `PENDIENTE`; una de su tutor nace `APROBADA` "
            + "directamente. Única por asignación.")
    @ApiResponse(responseCode = "403", description = "Quien crea no es el alumno de esa asignación ni su tutor")
    @ApiResponse(responseCode = "400", description = "La calificación está fuera del rango permitido (ver `GET /api/reviews/calificacion-config`)")
    @ApiResponse(responseCode = "404", description = "La asignación indicada no existe")
    @ApiResponse(responseCode = "409", description = "Esa asignación ya tiene una review")
    @PostMapping("/api/reviews")
    public ResponseEntity<ReviewDto> crear(Authentication authentication, @Valid @RequestBody CrearReviewRequest request) {
        ReviewDto response = reviewService.crear(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Editar una review propia", description = "Solo el autor original. Si el autor es `ALUMNO`, "
            + "editar reenvía la review a `PENDIENTE` (pierde la moderación previa); si es `PROFESOR`, se mantiene `APROBADA`.")
    @ApiResponse(responseCode = "403", description = "Quien edita no es el autor original de la review")
    @ApiResponse(responseCode = "400", description = "La calificación está fuera del rango permitido")
    @ApiResponse(responseCode = "404", description = "La review no existe")
    @PutMapping("/api/reviews/{id}")
    public ResponseEntity<ReviewDto> editar(
            Authentication authentication, @PathVariable Long id, @Valid @RequestBody EditarReviewRequest request) {
        return ResponseEntity.ok(reviewService.editar(id, request, authentication.getName()));
    }

    @Operation(summary = "Listar las reviews de una empresa", description = "El profesorado ve todas, en cualquier "
            + "estado. Un alumno solo ve las `APROBADA`, más las suyas propias en cualquier estado.")
    @ApiResponse(responseCode = "404", description = "La empresa no existe")
    @GetMapping("/api/empresas/{empresaId}/reviews")
    public ResponseEntity<List<ReviewDto>> listarPorEmpresa(Authentication authentication, @PathVariable Long empresaId) {
        return ResponseEntity.ok(
                reviewService.listarPorEmpresa(empresaId, authentication.getName(), esProfesor(authentication)));
    }

    @Operation(summary = "Listar reviews pendientes de moderar", description = "Solo profesor/admin (cola de moderación completa, de cualquier empresa).")
    @GetMapping("/api/reviews/pendientes")
    public ResponseEntity<List<ReviewDto>> pendientes() {
        return ResponseEntity.ok(reviewService.listarPendientes());
    }

    @Operation(summary = "Listar reviews por estado", description = "Solo profesor/admin. Paginado: una pastilla por "
            + "estado en la pantalla de moderación. Orden fijo por fecha de creación descendente.")
    @GetMapping("/api/reviews")
    public ResponseEntity<PaginaDto<ReviewDto>> listarPorEstado(
            @RequestParam EstadoReview estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "9") int tamano) {
        return ResponseEntity.ok(reviewService.listarPorEstado(estado, pagina, tamano));
    }

    @Operation(summary = "Devolver una review moderada a la cola", description = "Solo profesor/admin. Deshace una "
            + "aprobación o un rechazo: la review vuelve a `PENDIENTE` y pierde el motivo y el rastro de quién la "
            + "moderó. Para corregir un clic equivocado sin esperar a que el alumno la edite.")
    @ApiResponse(responseCode = "400", description = "La review ya estaba en `PENDIENTE`")
    @ApiResponse(responseCode = "404", description = "La review no existe")
    @PutMapping("/api/reviews/{id}/revertir")
    public ResponseEntity<ReviewDto> revertir(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.revertirModeracion(id));
    }

    @Operation(summary = "Moderar una review pendiente", description = "Solo profesor/admin. Aprobar o rechazar; "
            + "rechazar exige `motivoRechazo`. Solo se puede moderar una review que siga en `PENDIENTE`.")
    @ApiResponse(responseCode = "409", description = "La review ya fue moderada previamente (no está en `PENDIENTE`)")
    @ApiResponse(responseCode = "400", description = "El estado no es `APROBADA`/`RECHAZADA`, o falta `motivoRechazo` al rechazar")
    @ApiResponse(responseCode = "404", description = "La review no existe")
    @PutMapping("/api/reviews/{id}/moderar")
    public ResponseEntity<ReviewDto> moderar(
            Authentication authentication, @PathVariable Long id, @Valid @RequestBody ModerarReviewRequest request) {
        return ResponseEntity.ok(reviewService.moderar(id, request, authentication.getName()));
    }

    @Operation(summary = "Rango de calificación válido", description = "Cualquier rol autenticado. Configurable por instancia (`app.review.calificacion-min`/`-max`), 1-5 por defecto.")
    @GetMapping("/api/reviews/calificacion-config")
    public ResponseEntity<CalificacionConfigDto> calificacionConfig() {
        return ResponseEntity.ok(reviewService.calificacionConfig());
    }

    private boolean esProfesor(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_PROFESOR"));
    }
}
