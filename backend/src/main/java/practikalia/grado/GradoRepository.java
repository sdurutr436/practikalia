package practikalia.grado;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GradoRepository extends JpaRepository<Grado, Long> {

    Optional<Grado> findByNombre(String nombre);

    /** La clase que tutoriza un profesor, si tiene alguna. Como mucho hay una. */
    Optional<Grado> findByTutorId(Long tutorId);

    /** Las clases de toda una página de profesorado, para no consultar tarjeta a tarjeta. */
    List<Grado> findByTutorIdIn(List<Long> tutorIds);
}
