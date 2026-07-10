package practikalia.review;

import java.util.List;

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
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/api/reviews")
    public ResponseEntity<ReviewDto> crear(Authentication authentication, @Valid @RequestBody CrearReviewRequest request) {
        ReviewDto response = reviewService.crear(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/empresas/{empresaId}/reviews")
    public ResponseEntity<List<ReviewDto>> listarPorEmpresa(Authentication authentication, @PathVariable Long empresaId) {
        return ResponseEntity.ok(
                reviewService.listarPorEmpresa(empresaId, authentication.getName(), esProfesor(authentication)));
    }

    @GetMapping("/api/reviews/pendientes")
    public ResponseEntity<List<ReviewDto>> pendientes() {
        return ResponseEntity.ok(reviewService.listarPendientes());
    }

    @PutMapping("/api/reviews/{id}/moderar")
    public ResponseEntity<ReviewDto> moderar(
            Authentication authentication, @PathVariable Long id, @Valid @RequestBody ModerarReviewRequest request) {
        return ResponseEntity.ok(reviewService.moderar(id, request, authentication.getName()));
    }

    @GetMapping("/api/reviews/calificacion-config")
    public ResponseEntity<CalificacionConfigDto> calificacionConfig() {
        return ResponseEntity.ok(reviewService.calificacionConfig());
    }

    private boolean esProfesor(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_PROFESOR"));
    }
}
