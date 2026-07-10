package practikalia.review;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByEmpresaId(Long empresaId);

    List<Review> findByEstado(EstadoReview estado);

    boolean existsByAlumnoIdAndEmpresaId(Long alumnoId, Long empresaId);
}
