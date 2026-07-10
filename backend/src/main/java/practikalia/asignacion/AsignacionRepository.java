package practikalia.asignacion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AsignacionRepository extends JpaRepository<Asignacion, Long> {

    List<Asignacion> findByAlumnoId(Long alumnoId);

    List<Asignacion> findByEmpresaId(Long empresaId);

    boolean existsByAlumnoIdAndEmpresaId(Long alumnoId, Long empresaId);

    Optional<Asignacion> findByAlumnoIdAndEmpresaId(Long alumnoId, Long empresaId);
}
