package practikalia.asignacion;

import practikalia.empresa.Empresa;
import practikalia.empresa.EmpresaException;
import practikalia.empresa.EmpresaRepository;
import practikalia.etiqueta.Etiqueta;
import practikalia.grado.Grado;
import practikalia.grado.GradoRepository;
import practikalia.usuario.Rol;
import practikalia.usuario.Usuario;
import practikalia.usuario.UsuarioException;
import practikalia.usuario.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AsignacionServiceTest {

    private AsignacionRepository asignacionRepository;
    private UsuarioRepository usuarioRepository;
    private EmpresaRepository empresaRepository;
    private GradoRepository gradoRepository;
    private AsignacionService asignacionService;

    private final Usuario alumno = usuarioConId(1L, "alumno@iesejemplo.es", Rol.ALUMNO);
    private final Usuario tutor = usuarioConId(2L, "tutor@iesejemplo.es", Rol.PROFESOR);
    private final Empresa empresa = new Empresa("Acme", null, null, new Etiqueta("Tecnología"), null, null, null, null, tutor);
    private final Grado grado = new Grado("DAW");

    {
        empresa.setId(10L);
        grado.setId(20L);
    }

    @BeforeEach
    void setUp() {
        asignacionRepository = mock(AsignacionRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        empresaRepository = mock(EmpresaRepository.class);
        gradoRepository = mock(GradoRepository.class);
        asignacionService = new AsignacionService(asignacionRepository, usuarioRepository, empresaRepository, gradoRepository);
        when(asignacionRepository.save(any(Asignacion.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Usuario usuarioConId(Long id, String correo, Rol rol) {
        Usuario usuario = new Usuario(correo, "hash", rol);
        usuario.setId(id);
        return usuario;
    }

    private CrearAsignacionRequest request() {
        return new CrearAsignacionRequest(1L, 10L, 2L, 20L, 1, LocalDate.of(2026, 1, 15));
    }

    @Test
    void crearAsignacionValida() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(alumno));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(empresaRepository.findById(10L)).thenReturn(Optional.of(empresa));
        when(gradoRepository.findById(20L)).thenReturn(Optional.of(grado));

        AsignacionDto dto = asignacionService.crear(request());

        assertThat(dto.alumnoCorreo()).isEqualTo("alumno@iesejemplo.es");
        assertThat(dto.tutorCentroCorreo()).isEqualTo("tutor@iesejemplo.es");
        assertThat(dto.grado().nombre()).isEqualTo("DAW");
        assertThat(dto.anio()).isEqualTo(1);
        assertThat(dto.fechaFin()).isNull();
    }

    @Test
    void alumnoIdQueNoEsAlumnoLanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(tutor));

        assertThatThrownBy(() -> asignacionService.crear(request()))
                .isInstanceOf(AsignacionException.class)
                .hasFieldOrPropertyWithValue("codigo", "ALUMNO_INVALIDO");
    }

    @Test
    void tutorCentroIdQueNoEsProfesorLanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(alumno));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(alumno));

        assertThatThrownBy(() -> asignacionService.crear(request()))
                .isInstanceOf(AsignacionException.class)
                .hasFieldOrPropertyWithValue("codigo", "TUTOR_INVALIDO");
    }

    @Test
    void asignacionDuplicadaMismoGradoYAnioLanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(alumno));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(empresaRepository.findById(10L)).thenReturn(Optional.of(empresa));
        when(gradoRepository.findById(20L)).thenReturn(Optional.of(grado));
        when(asignacionRepository.existsByAlumnoIdAndEmpresaIdAndGradoIdAndAnio(1L, 10L, 20L, 1)).thenReturn(true);

        assertThatThrownBy(() -> asignacionService.crear(request()))
                .isInstanceOf(AsignacionException.class)
                .hasFieldOrPropertyWithValue("codigo", "ASIGNACION_YA_EXISTE");
    }

    @Test
    void mismoAlumnoEmpresaDistintoAnioNoLanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(alumno));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(empresaRepository.findById(10L)).thenReturn(Optional.of(empresa));
        when(gradoRepository.findById(20L)).thenReturn(Optional.of(grado));
        when(asignacionRepository.existsByAlumnoIdAndEmpresaIdAndGradoIdAndAnio(1L, 10L, 20L, 2)).thenReturn(false);

        AsignacionDto dto = asignacionService.crear(new CrearAsignacionRequest(1L, 10L, 2L, 20L, 2, LocalDate.of(2027, 1, 15)));

        assertThat(dto.anio()).isEqualTo(2);
    }

    @Test
    void gradoIdQueNoExisteLanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(alumno));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(empresaRepository.findById(10L)).thenReturn(Optional.of(empresa));
        when(gradoRepository.findById(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> asignacionService.crear(request()))
                .hasFieldOrPropertyWithValue("codigo", "GRADO_NO_ENCONTRADO");
    }

    @Test
    void cerrarAsignacionEstableceFechaFin() {
        Asignacion asignacion = new Asignacion(alumno, empresa, tutor, grado, 1, LocalDate.of(2026, 1, 15));
        when(asignacionRepository.findById(5L)).thenReturn(Optional.of(asignacion));

        AsignacionDto dto = asignacionService.cerrar(5L, new ActualizarAsignacionRequest(LocalDate.of(2026, 6, 30), null));

        assertThat(dto.fechaFin()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(dto.contratadoPosterior()).isNull();
    }

    @Test
    void cerrarConContratadoPosteriorLoPersiste() {
        Asignacion asignacion = new Asignacion(alumno, empresa, tutor, grado, 1, LocalDate.of(2026, 1, 15));
        when(asignacionRepository.findById(5L)).thenReturn(Optional.of(asignacion));

        AsignacionDto dto = asignacionService.cerrar(5L, new ActualizarAsignacionRequest(LocalDate.of(2026, 6, 30), true));

        assertThat(dto.contratadoPosterior()).isTrue();
    }

    @Test
    void actualizarContratadoPosteriorEnLlamadaPosterior() {
        Asignacion asignacion = new Asignacion(alumno, empresa, tutor, grado, 1, LocalDate.of(2026, 1, 15));
        when(asignacionRepository.findById(5L)).thenReturn(Optional.of(asignacion));

        asignacionService.cerrar(5L, new ActualizarAsignacionRequest(LocalDate.of(2026, 6, 30), null));
        AsignacionDto dto = asignacionService.cerrar(5L, new ActualizarAsignacionRequest(LocalDate.of(2026, 6, 30), false));

        assertThat(dto.fechaFin()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(dto.contratadoPosterior()).isFalse();
    }

    @Test
    void tasaContratacionCalculaRatio() {
        when(empresaRepository.existsById(10L)).thenReturn(true);
        when(asignacionRepository.countByEmpresaIdAndFechaFinIsNotNullAndContratadoPosteriorIsNotNull(10L)).thenReturn(4L);
        when(asignacionRepository.countByEmpresaIdAndFechaFinIsNotNullAndContratadoPosteriorTrue(10L)).thenReturn(3L);

        TasaContratacionDto dto = asignacionService.tasaContratacion(10L);

        assertThat(dto.asignacionesDecididas()).isEqualTo(4);
        assertThat(dto.contrataciones()).isEqualTo(3);
        assertThat(dto.tasa()).isEqualTo(0.75);
    }

    @Test
    void tasaContratacionSinDecididasEsCero() {
        when(empresaRepository.existsById(10L)).thenReturn(true);

        TasaContratacionDto dto = asignacionService.tasaContratacion(10L);

        assertThat(dto.asignacionesDecididas()).isZero();
        assertThat(dto.tasa()).isEqualTo(0.0);
    }

    @Test
    void tasaContratacionEmpresaInexistenteLanzaExcepcion() {
        when(empresaRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> asignacionService.tasaContratacion(99L))
                .isInstanceOf(EmpresaException.class)
                .hasFieldOrPropertyWithValue("codigo", "EMPRESA_NO_ENCONTRADA");
    }

    @Test
    void listarPorAlumnoComoOtroAlumnoLanzaAccesoDenegado() {
        when(usuarioRepository.findByCorreo("otro@iesejemplo.es")).thenReturn(Optional.of(usuarioConId(3L, "otro@iesejemplo.es", Rol.ALUMNO)));

        assertThatThrownBy(() -> asignacionService.listarPorAlumno(1L, false, "otro@iesejemplo.es"))
                .isInstanceOf(UsuarioException.class)
                .hasFieldOrPropertyWithValue("codigo", "ACCESO_DENEGADO");
    }
}
