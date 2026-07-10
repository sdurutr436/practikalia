package practikalia.interes;

import practikalia.empresa.Empresa;
import practikalia.empresa.EmpresaRepository;
import practikalia.etiqueta.Etiqueta;
import practikalia.etiqueta.EtiquetaRepository;
import practikalia.grado.Grado;
import practikalia.grado.GradoRepository;
import practikalia.usuario.Rol;
import practikalia.usuario.Usuario;
import practikalia.usuario.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class InteresRepositoryTest {

    @Autowired
    private InteresRepository interesRepository;
    @Autowired
    private EtiquetaRepository etiquetaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EmpresaRepository empresaRepository;
    @Autowired
    private GradoRepository gradoRepository;

    private Usuario alumno;
    private Empresa empresa;
    private Grado grado;

    @BeforeEach
    void setUp() {
        Etiqueta sector = etiquetaRepository.save(new Etiqueta("Tecnología"));
        Usuario profesor = usuarioRepository.save(new Usuario("prof@iesejemplo.es", "hash", Rol.PROFESOR));
        alumno = usuarioRepository.save(new Usuario("alumno@iesejemplo.es", "hash", Rol.ALUMNO));
        empresa = empresaRepository.save(new Empresa("Acme", null, null, sector, null, null, null, null, profesor));
        grado = gradoRepository.save(new Grado("DAW"));
    }

    @Test
    void guardaYBuscaPorAlumnoEmpresaGradoYAnio() {
        interesRepository.save(new Interes(alumno, empresa, grado, 1));

        assertThat(interesRepository.findByAlumnoIdAndEmpresaIdAndGradoIdAndAnio(
                alumno.getId(), empresa.getId(), grado.getId(), 1)).isPresent();
        assertThat(interesRepository.findByAlumnoIdAndEmpresaIdAndGradoIdAndAnio(
                alumno.getId(), empresa.getId(), grado.getId(), 2)).isEmpty();
    }

    @Test
    void listaPorEmpresaYPorAlumno() {
        interesRepository.save(new Interes(alumno, empresa, grado, 1));
        interesRepository.save(new Interes(alumno, empresa, grado, 2));

        assertThat(interesRepository.findByEmpresaId(empresa.getId())).hasSize(2);
        assertThat(interesRepository.findByAlumnoId(alumno.getId())).hasSize(2);
    }

    @Test
    void duplicadoMismoAlumnoEmpresaGradoYAnioLanzaExcepcion() {
        interesRepository.saveAndFlush(new Interes(alumno, empresa, grado, 1));

        assertThatThrownBy(() -> interesRepository.saveAndFlush(new Interes(alumno, empresa, grado, 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
