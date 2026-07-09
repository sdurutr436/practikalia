package practikalia.empresa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    List<Empresa> findByPublicadaTrue();
}
