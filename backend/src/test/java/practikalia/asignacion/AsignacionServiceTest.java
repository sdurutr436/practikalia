package practikalia.asignacion;

import practikalia.empresa.Empresa;
import practikalia.empresa.EmpresaRepository;
import practikalia.etiqueta.Etiqueta;
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
    private AsignacionService asignacionService;

    private final Usuario alumno = usuarioConId(1L, "alumno@iesejemplo.es", Rol.ALUMNO);
    private final Usuario tutor = usuarioConId(2L, "tutor@iesejemplo.es", Rol.PROFESOR);
    private final Empresa empresa = new Empresa("Acme", null, null, new Etiqueta("Tecnología"), null, null, null, null, tutor);

    {
        empresa.setId(10L);
    }

    @BeforeEach
    void setUp() {
        asignacionRepository = mock(AsignacionRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        empresaRepository = mock(EmpresaRepository.class);
        asignacionService = new AsignacionService(asignacionRepository, usuarioRepository, empresaRepository);
        when(asignacionRepository.save(any(Asignacion.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Usuario usuarioConId(Long id, String correo, Rol rol) {
        Usuario usuario = new Usuario(correo, "hash", rol);
        usuario.setId(id);
        return usuario;
    }

    private CrearAsignacionRequest request() {
        return new CrearAsignacionRequest(1L, 10L, 2L, LocalDate.of(2026, 1, 15));
    }

    @Test
    void crearAsignacionValida() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(alumno));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(empresaRepository.findById(10L)).thenReturn(Optional.of(empresa));

        AsignacionDto dto = asignacionService.crear(request());

        assertThat(dto.alumnoCorreo()).isEqualTo("alumno@iesejemplo.es");
        assertThat(dto.tutorCentroCorreo()).isEqualTo("tutor@iesejemplo.es");
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
    void asignacionDuplicadaLanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(alumno));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(empresaRepository.findById(10L)).thenReturn(Optional.of(empresa));
        when(asignacionRepository.existsByAlumnoIdAndEmpresaId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> asignacionService.crear(request()))
                .isInstanceOf(AsignacionException.class)
                .hasFieldOrPropertyWithValue("codigo", "ASIGNACION_YA_EXISTE");
    }

    @Test
    void cerrarAsignacionEstableceFechaFin() {
        Asignacion asignacion = new Asignacion(alumno, empresa, tutor, LocalDate.of(2026, 1, 15));
        when(asignacionRepository.findById(5L)).thenReturn(Optional.of(asignacion));

        AsignacionDto dto = asignacionService.cerrar(5L, new ActualizarAsignacionRequest(LocalDate.of(2026, 6, 30)));

        assertThat(dto.fechaFin()).isEqualTo(LocalDate.of(2026, 6, 30));
    }

    @Test
    void listarPorAlumnoComoOtroAlumnoLanzaAccesoDenegado() {
        when(usuarioRepository.findByCorreo("otro@iesejemplo.es")).thenReturn(Optional.of(usuarioConId(3L, "otro@iesejemplo.es", Rol.ALUMNO)));

        assertThatThrownBy(() -> asignacionService.listarPorAlumno(1L, false, "otro@iesejemplo.es"))
                .isInstanceOf(UsuarioException.class)
                .hasFieldOrPropertyWithValue("codigo", "ACCESO_DENEGADO");
    }
}
