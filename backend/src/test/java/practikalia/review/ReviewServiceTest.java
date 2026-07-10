package practikalia.review;

import practikalia.asignacion.Asignacion;
import practikalia.asignacion.AsignacionException;
import practikalia.asignacion.AsignacionRepository;
import practikalia.empresa.Empresa;
import practikalia.empresa.EmpresaRepository;
import practikalia.etiqueta.Etiqueta;
import practikalia.grado.Grado;
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
    private final Grado grado = new Grado("DAW");

    {
        empresa.setId(10L);
        grado.setId(20L);
    }

    private final Asignacion asignacion = new Asignacion(alumno, empresa, tutor, grado, 1, LocalDate.of(2026, 1, 15));

    {
        asignacion.setId(50L);
    }

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
        return new CrearReviewRequest(50L, "Buena experiencia", calificacion);
    }

    @Test
    void alumnoCreaSobreAsignacionPropiaQuedaPendiente() {
        when(usuarioRepository.findByCorreo("alumno@iesejemplo.es")).thenReturn(Optional.of(alumno));
        when(asignacionRepository.findById(50L)).thenReturn(Optional.of(asignacion));

        ReviewDto dto = reviewService.crear(request(4), "alumno@iesejemplo.es");

        assertThat(dto.estado()).isEqualTo(EstadoReview.PENDIENTE);
        assertThat(dto.moderadaPorCorreo()).isNull();
        assertThat(dto.autorCorreo()).isEqualTo("alumno@iesejemplo.es");
    }

    @Test
    void crearReviewConAsignacionInexistenteDevuelve404() {
        when(usuarioRepository.findByCorreo("alumno@iesejemplo.es")).thenReturn(Optional.of(alumno));
        when(asignacionRepository.findById(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.crear(request(4), "alumno@iesejemplo.es"))
                .isInstanceOf(AsignacionException.class)
                .hasFieldOrPropertyWithValue("codigo", "ASIGNACION_NO_ENCONTRADA");
    }

    @Test
    void alumnoCreandoSobreAsignacionAjenaDevuelve403() {
        Usuario otroAlumno = usuarioConId(4L, "otro-alumno@iesejemplo.es", Rol.ALUMNO);
        when(usuarioRepository.findByCorreo("otro-alumno@iesejemplo.es")).thenReturn(Optional.of(otroAlumno));
        when(asignacionRepository.findById(50L)).thenReturn(Optional.of(asignacion));

        assertThatThrownBy(() -> reviewService.crear(request(4), "otro-alumno@iesejemplo.es"))
                .isInstanceOf(UsuarioException.class)
                .hasFieldOrPropertyWithValue("codigo", "ACCESO_DENEGADO");
    }

    @Test
    void tutorCreaEnNombreDeSuAlumnoQuedaAprobada() {
        when(usuarioRepository.findByCorreo("tutor@iesejemplo.es")).thenReturn(Optional.of(tutor));
        when(asignacionRepository.findById(50L)).thenReturn(Optional.of(asignacion));

        ReviewDto dto = reviewService.crear(request(4), "tutor@iesejemplo.es");

        assertThat(dto.estado()).isEqualTo(EstadoReview.APROBADA);
        assertThat(dto.autorCorreo()).isEqualTo("tutor@iesejemplo.es");
        assertThat(dto.alumnoCorreo()).isEqualTo("alumno@iesejemplo.es");
        assertThat(dto.moderadaPorCorreo()).isNull();
    }

    @Test
    void profesorQueNoEsElTutorNoPuedeCrearReview() {
        when(usuarioRepository.findByCorreo("otro@iesejemplo.es")).thenReturn(Optional.of(otroProfesor));
        when(asignacionRepository.findById(50L)).thenReturn(Optional.of(asignacion));

        assertThatThrownBy(() -> reviewService.crear(request(4), "otro@iesejemplo.es"))
                .isInstanceOf(UsuarioException.class)
                .hasFieldOrPropertyWithValue("codigo", "ACCESO_DENEGADO");
    }

    @Test
    void tutorNoPuedeCrearReviewSiLaAsignacionYaTieneUna() {
        when(usuarioRepository.findByCorreo("tutor@iesejemplo.es")).thenReturn(Optional.of(tutor));
        when(asignacionRepository.findById(50L)).thenReturn(Optional.of(asignacion));
        when(reviewRepository.existsByAsignacionId(50L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.crear(request(4), "tutor@iesejemplo.es"))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("codigo", "REVIEW_YA_EXISTE");
    }

    @Test
    void calificacionFueraDeRangoDevuelve400() {
        when(usuarioRepository.findByCorreo("alumno@iesejemplo.es")).thenReturn(Optional.of(alumno));
        when(asignacionRepository.findById(50L)).thenReturn(Optional.of(asignacion));

        assertThatThrownBy(() -> reviewService.crear(request(6), "alumno@iesejemplo.es"))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("codigo", "CAMPO_INVALIDO");
    }

    @Test
    void editarComoAutorAlumnoVuelveAPendiente() {
        Review review = new Review(asignacion, alumno, "texto", 4, EstadoReview.APROBADA);
        review.setModeradaPor(tutor);
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));
        when(usuarioRepository.findByCorreo("alumno@iesejemplo.es")).thenReturn(Optional.of(alumno));

        ReviewDto dto = reviewService.editar(5L, new EditarReviewRequest("texto editado", 5), "alumno@iesejemplo.es");

        assertThat(dto.estado()).isEqualTo(EstadoReview.PENDIENTE);
        assertThat(dto.moderadaPorCorreo()).isNull();
        assertThat(dto.contenido()).isEqualTo("texto editado");
        assertThat(dto.calificacion()).isEqualTo(5);
    }

    @Test
    void editarComoAutorProfesorMantieneAprobada() {
        Review review = new Review(asignacion, tutor, "texto", 4, EstadoReview.APROBADA);
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));
        when(usuarioRepository.findByCorreo("tutor@iesejemplo.es")).thenReturn(Optional.of(tutor));

        ReviewDto dto = reviewService.editar(5L, new EditarReviewRequest("texto editado", 5), "tutor@iesejemplo.es");

        assertThat(dto.estado()).isEqualTo(EstadoReview.APROBADA);
    }

    @Test
    void editarSinSerAutorDevuelve403() {
        Review review = new Review(asignacion, alumno, "texto", 4, EstadoReview.PENDIENTE);
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));
        when(usuarioRepository.findByCorreo("tutor@iesejemplo.es")).thenReturn(Optional.of(tutor));

        assertThatThrownBy(() -> reviewService.editar(5L, new EditarReviewRequest("texto editado", 5), "tutor@iesejemplo.es"))
                .isInstanceOf(UsuarioException.class)
                .hasFieldOrPropertyWithValue("codigo", "ACCESO_DENEGADO");
    }

    @Test
    void moderarAprobandoRellenaModeradorYFecha() {
        Review review = new Review(asignacion, alumno, "texto", 4, EstadoReview.PENDIENTE);
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));
        when(usuarioRepository.findByCorreo("tutor@iesejemplo.es")).thenReturn(Optional.of(tutor));

        ReviewDto dto = reviewService.moderar(5L, new ModerarReviewRequest(EstadoReview.APROBADA, null), "tutor@iesejemplo.es");

        assertThat(dto.estado()).isEqualTo(EstadoReview.APROBADA);
        assertThat(dto.moderadaPorCorreo()).isEqualTo("tutor@iesejemplo.es");
        assertThat(dto.fechaModeracion()).isNotNull();
    }

    @Test
    void moderarRechazandoConMotivoLoGuarda() {
        Review review = new Review(asignacion, alumno, "texto", 4, EstadoReview.PENDIENTE);
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));
        when(usuarioRepository.findByCorreo("tutor@iesejemplo.es")).thenReturn(Optional.of(tutor));

        ReviewDto dto = reviewService.moderar(
                5L, new ModerarReviewRequest(EstadoReview.RECHAZADA, "Contenido inapropiado"), "tutor@iesejemplo.es");

        assertThat(dto.estado()).isEqualTo(EstadoReview.RECHAZADA);
        assertThat(dto.motivoRechazo()).isEqualTo("Contenido inapropiado");
    }

    @Test
    void rechazarSinMotivoDevuelve400() {
        Review review = new Review(asignacion, alumno, "texto", 4, EstadoReview.PENDIENTE);
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() ->
                reviewService.moderar(5L, new ModerarReviewRequest(EstadoReview.RECHAZADA, null), "tutor@iesejemplo.es"))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("codigo", "CAMPO_INVALIDO");
    }

    @Test
    void moderarUnaYaModeradaDevuelve409() {
        Review review = new Review(asignacion, alumno, "texto", 4, EstadoReview.APROBADA);
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() ->
                reviewService.moderar(5L, new ModerarReviewRequest(EstadoReview.APROBADA, null), "tutor@iesejemplo.es"))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("codigo", "REVIEW_YA_MODERADA");
    }
}
