package practikalia.review;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByAsignacion_EmpresaId(Long empresaId);

    List<Review> findByEstado(EstadoReview estado);

    boolean existsByAsignacionId(Long asignacionId);
}
