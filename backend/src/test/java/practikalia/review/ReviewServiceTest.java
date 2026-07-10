package practikalia.review;

import practikalia.asignacion.Asignacion;
import practikalia.asignacion.AsignacionException;
import practikalia.asignacion.AsignacionRepository;
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

class ReviewServiceTest {

    private ReviewRepository reviewRepository;
    private AsignacionRepository asignacionRepository;
    private UsuarioRepository usuarioRepository;
    private EmpresaRepository empresaRepository;
    private ReviewService reviewService;

    private final Usuario alumno = usuarioConId(1L, "alumno@iesejemplo.es", Rol.ALUMNO);
    private final Usuario tutor = usuarioConId(2L, "tutor@iesejemplo.es", Rol.PROFESOR);
    private final Usuario otroProfesor = usuarioConId(3L, "otro@iesejemplo.es", Rol.PROFESOR);
    private final Empresa empresa = new Empresa("Acme", null, null, new Etiqueta("Tecnología"), null, null, null, null, tutor);

    {
        empresa.setId(10L);
    }

    private final Asignacion asignacion = new Asignacion(alumno, empresa, tutor, LocalDate.of(2026, 1, 15));

    @BeforeEach
    void setUp() {
        reviewRepository = mock(ReviewRepository.class);
        asignacionRepository = mock(AsignacionRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        empresaRepository = mock(EmpresaRepository.class);
        reviewService = new ReviewService(reviewRepository, asignacionRepository, usuarioRepository, empresaRepository, 1, 5);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Usuario usuarioConId(Long id, String correo, Rol rol) {
        Usuario usuario = new Usuario(correo, "hash", rol);
        usuario.setId(id);
        return usuario;
    }

    private CrearReviewRequest request(int calificacion) {
        return new CrearReviewRequest(10L, 1L, "Buena experiencia", calificacion);
    }

    @Test
    void alumnoCreaSobreEmpresaALaQuePertenecioQuedaPendiente() {
        when(usuarioRepository.findByCorreo("alumno@iesejemplo.es")).thenReturn(Optional.of(alumno));
        when(asignacionRepository.findByAlumnoIdAndEmpresaId(1L, 10L)).thenReturn(Optional.of(asignacion));

        ReviewDto dto = reviewService.crear(request(4), "alumno@iesejemplo.es");

        assertThat(dto.estado()).isEqualTo(EstadoReview.PENDIENTE);
        assertThat(dto.moderadaPorCorreo()).isNull();
        assertThat(dto.autorCorreo()).isEqualTo("alumno@iesejemplo.es");
    }

    @Test
    void alumnoCreaSobreEmpresaALaQueNoPerteneceDevuelve404() {
        when(usuarioRepository.findByCorreo("alumno@iesejemplo.es")).thenReturn(Optional.of(alumno));
        when(asignacionRepository.findByAlumnoIdAndEmpresaId(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.crear(request(4), "alumno@iesejemplo.es"))
                .isInstanceOf(AsignacionException.class)
                .hasFieldOrPropertyWithValue("codigo", "ASIGNACION_NO_ENCONTRADA");
    }

    @Test
    void tutorCreaEnNombreDeSuAlumnoQuedaAprobada() {
        when(usuarioRepository.findByCorreo("tutor@iesejemplo.es")).thenReturn(Optional.of(tutor));
        when(asignacionRepository.findByAlumnoIdAndEmpresaId(1L, 10L)).thenReturn(Optional.of(asignacion));

        ReviewDto dto = reviewService.crear(request(4), "tutor@iesejemplo.es");

        assertThat(dto.estado()).isEqualTo(EstadoReview.APROBADA);
        assertThat(dto.autorCorreo()).isEqualTo("tutor@iesejemplo.es");
        assertThat(dto.alumnoCorreo()).isEqualTo("alumno@iesejemplo.es");
        assertThat(dto.moderadaPorCorreo()).isNull();
    }

    @Test
    void profesorQueNoEsElTutorNoPuedeCrearReview() {
        when(usuarioRepository.findByCorreo("otro@iesejemplo.es")).thenReturn(Optional.of(otroProfesor));
        when(asignacionRepository.findByAlumnoIdAndEmpresaId(1L, 10L)).thenReturn(Optional.of(asignacion));

        assertThatThrownBy(() -> reviewService.crear(request(4), "otro@iesejemplo.es"))
                .isInstanceOf(UsuarioException.class)
                .hasFieldOrPropertyWithValue("codigo", "ACCESO_DENEGADO");
    }

    @Test
    void tutorNoPuedeCrearReviewSiElAlumnoYaTieneUna() {
        when(usuarioRepository.findByCorreo("tutor@iesejemplo.es")).thenReturn(Optional.of(tutor));
        when(asignacionRepository.findByAlumnoIdAndEmpresaId(1L, 10L)).thenReturn(Optional.of(asignacion));
        when(reviewRepository.existsByAlumnoIdAndEmpresaId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.crear(request(4), "tutor@iesejemplo.es"))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("codigo", "REVIEW_YA_EXISTE");
    }

    @Test
    void calificacionFueraDeRangoDevuelve400() {
        when(usuarioRepository.findByCorreo("alumno@iesejemplo.es")).thenReturn(Optional.of(alumno));
        when(asignacionRepository.findByAlumnoIdAndEmpresaId(1L, 10L)).thenReturn(Optional.of(asignacion));

        assertThatThrownBy(() -> reviewService.crear(request(6), "alumno@iesejemplo.es"))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("codigo", "CAMPO_INVALIDO");
    }

    @Test
    void moderarAprobandoRellenaModeradorYFecha() {
        Review review = new Review(alumno, alumno, empresa, "texto", 4, EstadoReview.PENDIENTE);
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));
        when(usuarioRepository.findByCorreo("tutor@iesejemplo.es")).thenReturn(Optional.of(tutor));

        ReviewDto dto = reviewService.moderar(5L, new ModerarReviewRequest(EstadoReview.APROBADA, null), "tutor@iesejemplo.es");

        assertThat(dto.estado()).isEqualTo(EstadoReview.APROBADA);
        assertThat(dto.moderadaPorCorreo()).isEqualTo("tutor@iesejemplo.es");
        assertThat(dto.fechaModeracion()).isNotNull();
    }

    @Test
    void moderarRechazandoConMotivoLoGuarda() {
        Review review = new Review(alumno, alumno, empresa, "texto", 4, EstadoReview.PENDIENTE);
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));
        when(usuarioRepository.findByCorreo("tutor@iesejemplo.es")).thenReturn(Optional.of(tutor));

        ReviewDto dto = reviewService.moderar(
                5L, new ModerarReviewRequest(EstadoReview.RECHAZADA, "Contenido inapropiado"), "tutor@iesejemplo.es");

        assertThat(dto.estado()).isEqualTo(EstadoReview.RECHAZADA);
        assertThat(dto.motivoRechazo()).isEqualTo("Contenido inapropiado");
    }

    @Test
    void rechazarSinMotivoDevuelve400() {
        Review review = new Review(alumno, alumno, empresa, "texto", 4, EstadoReview.PENDIENTE);
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() ->
                reviewService.moderar(5L, new ModerarReviewRequest(EstadoReview.RECHAZADA, null), "tutor@iesejemplo.es"))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("codigo", "CAMPO_INVALIDO");
    }

    @Test
    void moderarUnaYaModeradaDevuelve409() {
        Review review = new Review(alumno, alumno, empresa, "texto", 4, EstadoReview.APROBADA);
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() ->
                reviewService.moderar(5L, new ModerarReviewRequest(EstadoReview.APROBADA, null), "tutor@iesejemplo.es"))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("codigo", "REVIEW_YA_MODERADA");
    }
}
