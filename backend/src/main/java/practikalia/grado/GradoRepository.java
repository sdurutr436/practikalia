package practikalia.grado;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GradoRepository extends JpaRepository<Grado, Long> {

    Optional<Grado> findByNombre(String nombre);
}
