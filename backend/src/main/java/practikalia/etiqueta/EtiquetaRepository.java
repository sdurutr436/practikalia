package practikalia.etiqueta;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EtiquetaRepository extends JpaRepository<Etiqueta, Long> {

    Optional<Etiqueta> findByNombre(String nombre);

    /** Sin distinguir mayúsculas: el catálogo no debe tener «Java» y «java». */
    boolean existsByNombreIgnoreCase(String nombre);

    /** Un nodo con hijas no se borra: hay que vaciarlo de abajo arriba. */
    boolean existsByPadreId(Long padreId);
}
