package practikalia.interes;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InteresRepository extends JpaRepository<Interes, Long> {

    Optional<Interes> findByAlumnoIdAndEmpresaIdAndGradoIdAndAnio(Long alumnoId, Long empresaId, Long gradoId, Integer anio);

    List<Interes> findByEmpresaId(Long empresaId);

    List<Interes> findByAlumnoId(Long alumnoId);
}
