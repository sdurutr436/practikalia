package practikalia.afinidad;

import practikalia.asignacion.AsignacionRepository;
import practikalia.empresa.Empresa;
import practikalia.empresa.EmpresaRepository;
import practikalia.etiqueta.Etiqueta;
import practikalia.etiqueta.EtiquetaDto;
import practikalia.usuario.Rol;
import practikalia.usuario.Usuario;
import practikalia.usuario.UsuarioException;
import practikalia.usuario.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class AfinidadServiceTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
    private final AsignacionRepository asignacionRepository = mock(AsignacionRepository.class);
    private final AfinidadService afinidadService =
            new AfinidadService(usuarioRepository, empresaRepository, asignacionRepository);

    private final Etiqueta java = etiquetaConId(1L, "Java");
    private final Etiqueta redes = etiquetaConId(2L, "Redes");
    private final Etiqueta diseno = etiquetaConId(3L, "Diseño");
    private final Etiqueta tecnologia = etiquetaConId(4L, "Tecnología");

    private static Etiqueta etiquetaConId(Long id, String nombre) {
        Etiqueta etiqueta = new Etiqueta(nombre);
        etiqueta.setId(id);
        return etiqueta;
    }

    private static Usuario alumnoCon(Etiqueta... etiquetas) {
        Usuario alumno = new Usuario("alumno@iesejemplo.es", "hash", Rol.ALUMNO);
        alumno.setEtiquetas(List.of(etiquetas));
        return alumno;
    }

    private static Empresa empresaCon(String nombre, Etiqueta sector, Etiqueta... etiquetas) {
        Empresa empresa = new Empresa(nombre, null, null, sector, null, null, null, null, null);
        empresa.setEtiquetas(List.of(etiquetas));
        return empresa;
    }

    // --- cálculo puro por empresa ---

    @Test
    void solapamientoParcialSobreElMinimoDeEtiquetas() {
        AfinidadEmpresaDto resultado = afinidadService.calcularScore(
                alumnoCon(java, diseno), empresaCon("Acme", tecnologia, java, redes));

        assertThat(resultado.score()).isEqualTo(0.5); // 1 coincidencia / min(2, 2)
        assertThat(resultado.etiquetasCoincidentes()).extracting(EtiquetaDto::nombre).containsExactly("Java");
        assertThat(resultado.sectorCoincide()).isFalse();
    }

    @Test
    void solapamientoNoCastigaALaEmpresaConMenosEtiquetasQueElAlumno() {
        AfinidadEmpresaDto resultado = afinidadService.calcularScore(
                alumnoCon(java, redes, diseno), empresaCon("Acme", tecnologia, java, redes));

        assertThat(resultado.score()).isEqualTo(1.0); // 2 coincidencias / min(3, 2)
    }

    @Test
    void empresaSinEtiquetasPuntuaCeroSinError() {
        AfinidadEmpresaDto resultado = afinidadService.calcularScore(
                alumnoCon(java), empresaCon("Acme", tecnologia));

        assertThat(resultado.score()).isEqualTo(0.0);
        assertThat(resultado.etiquetasCoincidentes()).isEmpty();
        assertThat(resultado.sectorCoincide()).isFalse();
    }

    @Test
    void bonusDeSectorSeSumaAlSolapamiento() {
        AfinidadEmpresaDto resultado = afinidadService.calcularScore(
                alumnoCon(java, tecnologia), empresaCon("Acme", tecnologia, java));

        assertThat(resultado.score()).isEqualTo(1.0 + AfinidadService.BONUS_SECTOR); // min(2, 1) = 1
        assertThat(resultado.sectorCoincide()).isTrue();
    }

    @Test
    void empresaSinEtiquetasConservaElBonusDeSector() {
        AfinidadEmpresaDto resultado = afinidadService.calcularScore(
                alumnoCon(tecnologia), empresaCon("Acme", tecnologia));

        assertThat(resultado.score()).isEqualTo(AfinidadService.BONUS_SECTOR);
        assertThat(resultado.sectorCoincide()).isTrue();
    }

    // --- listado propio y por alumno ---

    @Test
    void obtenerPropiaOrdenaPorScoreDescendenteConDesempateAlfabetico() {
        when(usuarioRepository.findByCorreo("alumno@iesejemplo.es")).thenReturn(Optional.of(alumnoCon(java)));
        when(empresaRepository.findByPublicadaTrue()).thenReturn(List.of(
                empresaCon("Zeta", tecnologia, java),
                empresaCon("Acme", tecnologia),
                empresaCon("Beta", tecnologia, java)));

        AfinidadListadoDto listado = afinidadService.obtenerPropia("alumno@iesejemplo.es");

        assertThat(listado.alumnoConEtiquetas()).isTrue();
        assertThat(listado.empresas()).extracting(dto -> dto.empresa().nombre())
                .containsExactly("Beta", "Zeta", "Acme");
    }

    @Test
    void obtenerDeAlumnoComoAdminNoCompruebaTutoria() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(alumnoCon(java)));
        when(empresaRepository.findByPublicadaTrue()).thenReturn(List.of());

        AfinidadListadoDto listado = afinidadService.obtenerDeAlumno(1L, true, "admin@iesejemplo.es");

        assertThat(listado.alumnoConEtiquetas()).isTrue();
        verify(asignacionRepository, never()).existsByAlumnoIdAndTutorCentroIdAndFechaFinIsNull(any(), any());
    }

    @Test
    void obtenerDeAlumnoComoTutorDeLaAsignacionActivaCalcula() {
        Usuario tutor = new Usuario("prof@iesejemplo.es", "hash", Rol.PROFESOR);
        tutor.setId(2L);
        when(usuarioRepository.findByCorreo("prof@iesejemplo.es")).thenReturn(Optional.of(tutor));
        when(asignacionRepository.existsByAlumnoIdAndTutorCentroIdAndFechaFinIsNull(1L, 2L)).thenReturn(true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(alumnoCon(java)));
        when(empresaRepository.findByPublicadaTrue()).thenReturn(List.of());

        AfinidadListadoDto listado = afinidadService.obtenerDeAlumno(1L, false, "prof@iesejemplo.es");

        assertThat(listado.alumnoConEtiquetas()).isTrue();
    }

    @Test
    void obtenerDeAlumnoComoProfesorSinEsaTutoriaLanzaAccesoDenegado() {
        Usuario tutor = new Usuario("prof@iesejemplo.es", "hash", Rol.PROFESOR);
        tutor.setId(2L);
        when(usuarioRepository.findByCorreo("prof@iesejemplo.es")).thenReturn(Optional.of(tutor));
        when(asignacionRepository.existsByAlumnoIdAndTutorCentroIdAndFechaFinIsNull(1L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> afinidadService.obtenerDeAlumno(1L, false, "prof@iesejemplo.es"))
                .isInstanceOf(UsuarioException.class)
                .hasFieldOrPropertyWithValue("codigo", "ACCESO_DENEGADO");
        verify(usuarioRepository, never()).findById(any());
    }

    @Test
    void obtenerDeAlumnoSinNingunaAsignacionActivaComoProfesorLanzaAccesoDenegado() {
        Usuario tutor = new Usuario("prof@iesejemplo.es", "hash", Rol.PROFESOR);
        tutor.setId(2L);
        when(usuarioRepository.findByCorreo("prof@iesejemplo.es")).thenReturn(Optional.of(tutor));
        // alumno recién dado de alta: ninguna asignación activa de ningún tutor
        when(asignacionRepository.existsByAlumnoIdAndTutorCentroIdAndFechaFinIsNull(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> afinidadService.obtenerDeAlumno(1L, false, "prof@iesejemplo.es"))
                .isInstanceOf(UsuarioException.class)
                .hasFieldOrPropertyWithValue("codigo", "ACCESO_DENEGADO");
    }

    @Test
    void obtenerDeAlumnoInexistenteComoAdminLanzaNoEncontrado() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> afinidadService.obtenerDeAlumno(99L, true, "admin@iesejemplo.es"))
                .isInstanceOf(UsuarioException.class)
                .hasFieldOrPropertyWithValue("codigo", "USUARIO_NO_ENCONTRADO");
    }
}
