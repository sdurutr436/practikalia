package practikalia.etiqueta;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EtiquetaRepositoryTest {

    @Autowired
    private EtiquetaRepository etiquetaRepository;

    @Test
    void guardaYBuscaPorNombre() {
        etiquetaRepository.save(new Etiqueta("Java"));

        assertThat(etiquetaRepository.findByNombre("Java")).isPresent();
    }
}
