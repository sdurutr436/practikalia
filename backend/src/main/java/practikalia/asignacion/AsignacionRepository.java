package practikalia.asignacion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AsignacionRepository extends JpaRepository<Asignacion, Long> {

    List<Asignacion> findByAlumnoId(Long alumnoId);

    /** Las asignaciones en curso de varios alumnos a la vez, para no consultar tarjeta a tarjeta. */
    List<Asignacion> findByAlumnoIdInAndFechaFinIsNull(List<Long> alumnoIds);

    /** La asignación en curso de un alumno. Solo puede haber una abierta a la vez. */
    Optional<Asignacion> findFirstByAlumnoIdAndFechaFinIsNull(Long alumnoId);

    List<Asignacion> findByEmpresaId(Long empresaId);

    boolean existsByAlumnoIdAndEmpresaIdAndGradoIdAndAnio(Long alumnoId, Long empresaId, Long gradoId, int anio);

    boolean existsByAlumnoIdAndTutorCentroIdAndFechaFinIsNull(Long alumnoId, Long tutorCentroId);

    long countByEmpresaIdAndFechaFinIsNotNullAndContratadoPosteriorIsNotNull(Long empresaId);

    long countByEmpresaIdAndFechaFinIsNotNullAndContratadoPosteriorTrue(Long empresaId);
}
