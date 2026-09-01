package practikalia.panel;

import practikalia.asignacion.Asignacion;
import practikalia.asignacion.AsignacionRepository;
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

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PanelServiceTest {

    @Autowired
    private PanelService panelService;
    @Autowired
    private EtiquetaRepository etiquetaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EmpresaRepository empresaRepository;
    @Autowired
    private GradoRepository gradoRepository;
    @Autowired
    private AsignacionRepository asignacionRepository;

    private Usuario profesor;
    private Empresa publicada;
    private Grado grado;

    @BeforeEach
    void setUp() {
        Etiqueta sector = etiquetaRepository.save(new Etiqueta("Tecnología"));
        profesor = usuarioRepository.save(new Usuario("prof@iesejemplo.es", "hash", Rol.PROFESOR));
        grado = gradoRepository.save(new Grado("DAW"));

        publicada = new Empresa("Acme", null, null, sector, null, null, null, null, profesor);
        publicada.setPublicada(true);
        publicada = empresaRepository.save(publicada);
        empresaRepository.save(new Empresa("Borrador", null, null, sector, null, null, null, null, profesor));
    }

    @Test
    void cuentaEmpresasPorEstadoDePublicacion() {
        ResumenCentroDto resumen = panelService.resumenCentro();

        assertThat(resumen.empresasPublicadas()).isEqualTo(1);
        assertThat(resumen.empresasSinPublicar()).isEqualTo(1);
    }

    @Test
    void sinAsignarSoloMiraLasAsignacionesAbiertas() {
        Usuario conAsignacionAbierta = alumno("abierta@iesejemplo.es");
        Usuario conAsignacionCerrada = alumno("cerrada@iesejemplo.es");
        alumno("libre@iesejemplo.es");

        asignar(conAsignacionAbierta, null);
        asignar(conAsignacionCerrada, LocalDate.of(2026, 6, 30));

        ResumenCentroDto resumen = panelService.resumenCentro();

        assertThat(resumen.alumnadoActivo()).isEqualTo(3);
        // El de la asignación cerrada vuelve a estar libre, así que cuenta.
        assertThat(resumen.alumnadoSinAsignar()).isEqualTo(2);
    }

    @Test
    void elAlumnoInactivoNoCuentaEnNingunoDeLosDos() {
        Usuario inactivo = alumno("inactivo@iesejemplo.es");
        inactivo.setActivo(false);
        usuarioRepository.save(inactivo);

        ResumenCentroDto resumen = panelService.resumenCentro();

        assertThat(resumen.alumnadoActivo()).isZero();
        assertThat(resumen.alumnadoSinAsignar()).isZero();
    }

    private Usuario alumno(String correo) {
        return usuarioRepository.save(new Usuario(correo, "hash", Rol.ALUMNO));
    }

    private void asignar(Usuario alumno, LocalDate fechaFin) {
        Asignacion asignacion =
                new Asignacion(alumno, publicada, profesor, grado, 1, LocalDate.of(2026, 1, 15));
        asignacion.setFechaFin(fechaFin);
        asignacionRepository.save(asignacion);
    }
}
