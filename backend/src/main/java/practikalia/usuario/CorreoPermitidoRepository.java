package practikalia.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CorreoPermitidoRepository extends JpaRepository<CorreoPermitido, Long> {

    boolean existsByCorreo(String correo);
}
