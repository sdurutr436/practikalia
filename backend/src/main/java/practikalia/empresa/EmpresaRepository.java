package practikalia.empresa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmpresaRepository extends JpaRepository<Empresa, Long>, JpaSpecificationExecutor<Empresa> {

    List<Empresa> findByPublicadaTrue();

    long countByPublicada(boolean publicada);

    /** Para el borrado del catálogo: ¿alguna empresa tiene este nodo como sector? */
    boolean existsBySectorId(Long sectorId);

    /** Ídem, pero entre sus etiquetas. */
    boolean existsByEtiquetasId(Long etiquetaId);
}
