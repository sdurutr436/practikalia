package practikalia.review;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByAsignacion_EmpresaId(Long empresaId);

    List<Review> findByEstado(EstadoReview estado);

    Page<Review> findByEstado(EstadoReview estado, Pageable pageable);

    boolean existsByAsignacionId(Long asignacionId);
}
