package practikalia.grado;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class GradoRepositoryTest {

    @Autowired
    private GradoRepository gradoRepository;

    @Test
    void guardaYBuscaPorNombre() {
        gradoRepository.save(new Grado("DAW"));

        assertThat(gradoRepository.findByNombre("DAW")).isPresent();
    }

    @Test
    void nombreDuplicadoLanzaExcepcion() {
        gradoRepository.saveAndFlush(new Grado("DAM"));

        assertThatThrownBy(() -> gradoRepository.saveAndFlush(new Grado("DAM")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
