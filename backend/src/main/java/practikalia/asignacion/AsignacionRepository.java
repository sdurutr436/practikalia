package practikalia.asignacion;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AsignacionRepository extends JpaRepository<Asignacion, Long> {

    List<Asignacion> findByAlumnoId(Long alumnoId);

    List<Asignacion> findByEmpresaId(Long empresaId);

    boolean existsByAlumnoIdAndEmpresaIdAndGradoIdAndAnio(Long alumnoId, Long empresaId, Long gradoId, int anio);

    boolean existsByAlumnoIdAndTutorCentroIdAndFechaFinIsNull(Long alumnoId, Long tutorCentroId);

    long countByEmpresaIdAndFechaFinIsNotNullAndContratadoPosteriorIsNotNull(Long empresaId);

    long countByEmpresaIdAndFechaFinIsNotNullAndContratadoPosteriorTrue(Long empresaId);
}
