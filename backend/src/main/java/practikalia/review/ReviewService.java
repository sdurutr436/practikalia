package practikalia.review;

import practikalia.asignacion.Asignacion;
import practikalia.asignacion.AsignacionException;
import practikalia.asignacion.AsignacionRepository;
import practikalia.empresa.EmpresaException;
import practikalia.empresa.EmpresaRepository;
import practikalia.usuario.Rol;
import practikalia.usuario.Usuario;
import practikalia.usuario.UsuarioException;
import practikalia.usuario.UsuarioRepository;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final AsignacionRepository asignacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final int calificacionMin;
    private final int calificacionMax;

    public ReviewService(
            ReviewRepository reviewRepository,
            AsignacionRepository asignacionRepository,
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository,
            @Value("${app.review.calificacion-min:1}") int calificacionMin,
            @Value("${app.review.calificacion-max:5}") int calificacionMax) {
        this.reviewRepository = reviewRepository;
        this.asignacionRepository = asignacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.calificacionMin = calificacionMin;
        this.calificacionMax = calificacionMax;
    }

    @Transactional
    public ReviewDto crear(CrearReviewRequest request, String correoAutenticado) {
        Usuario autor = usuarioRepository.findByCorreo(correoAutenticado).orElseThrow();
        Asignacion asignacion = asignacionRepository.findById(request.asignacionId())
                .orElseThrow(AsignacionException::noEncontrada);

        if (autor.getRol() == Rol.ALUMNO && !autor.getId().equals(asignacion.getAlumno().getId())) {
            throw UsuarioException.accesoDenegado();
        }

        if (request.calificacion() < calificacionMin || request.calificacion() > calificacionMax) {
            throw ReviewException.campoInvalido(
                    "La calificación debe estar entre " + calificacionMin + " y " + calificacionMax);
        }

        if (autor.getRol() == Rol.PROFESOR && !asignacion.getTutorCentro().getId().equals(autor.getId())) {
            throw UsuarioException.accesoDenegado();
        }

        if (reviewRepository.existsByAsignacionId(asignacion.getId())) {
            throw ReviewException.yaExiste();
        }

        EstadoReview estado = autor.getRol() == Rol.PROFESOR ? EstadoReview.APROBADA : EstadoReview.PENDIENTE;
        Review review = new Review(asignacion, autor, request.contenido(), request.calificacion(), estado);
        reviewRepository.save(review);
        return ReviewDto.de(review);
    }

    @Transactional
    public ReviewDto editar(Long id, EditarReviewRequest request, String correoAutenticado) {
        Review review = reviewRepository.findById(id).orElseThrow(ReviewException::noEncontrada);
        Usuario autenticado = usuarioRepository.findByCorreo(correoAutenticado).orElseThrow();

        if (!review.getAutor().getId().equals(autenticado.getId())) {
            throw UsuarioException.accesoDenegado();
        }
        if (request.calificacion() < calificacionMin || request.calificacion() > calificacionMax) {
            throw ReviewException.campoInvalido(
                    "La calificación debe estar entre " + calificacionMin + " y " + calificacionMax);
        }

        review.setContenido(request.contenido());
        review.setCalificacion(request.calificacion());
        if (review.getAutor().getRol() == Rol.ALUMNO) {
            review.setEstado(EstadoReview.PENDIENTE);
            review.setModeradaPor(null);
            review.setMotivoRechazo(null);
            review.setFechaModeracion(null);
        }
        reviewRepository.save(review);
        return ReviewDto.de(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> listarPorEmpresa(Long empresaId, String correoAutenticado, boolean esProfesor) {
        if (!empresaRepository.existsById(empresaId)) {
            throw EmpresaException.noEncontrada();
        }
        List<Review> reviews = reviewRepository.findByAsignacion_EmpresaId(empresaId);
        if (esProfesor) {
            return reviews.stream().map(ReviewDto::de).toList();
        }

        Usuario autenticado = usuarioRepository.findByCorreo(correoAutenticado).orElseThrow();
        return reviews.stream()
                .filter(review -> review.getEstado() == EstadoReview.APROBADA
                        || review.getAsignacion().getAlumno().getId().equals(autenticado.getId())
                        || review.getAutor().getRol() == Rol.PROFESOR)
                .map(ReviewDto::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> listarPendientes() {
        return reviewRepository.findByEstado(EstadoReview.PENDIENTE).stream().map(ReviewDto::de).toList();
    }

    @Transactional
    public ReviewDto moderar(Long id, ModerarReviewRequest request, String correoAutenticado) {
        Review review = reviewRepository.findById(id).orElseThrow(ReviewException::noEncontrada);

        if (review.getEstado() != EstadoReview.PENDIENTE) {
            throw ReviewException.yaModerada();
        }
        if (request.estado() != EstadoReview.APROBADA && request.estado() != EstadoReview.RECHAZADA) {
            throw ReviewException.campoInvalido("El estado debe ser APROBADA o RECHAZADA");
        }
        if (request.estado() == EstadoReview.RECHAZADA
                && (request.motivoRechazo() == null || request.motivoRechazo().isBlank())) {
            throw ReviewException.campoInvalido("El motivo de rechazo es obligatorio al rechazar una review");
        }

        Usuario moderador = usuarioRepository.findByCorreo(correoAutenticado).orElseThrow();
        review.setEstado(request.estado());
        review.setModeradaPor(moderador);
        review.setFechaModeracion(Instant.now());
        review.setMotivoRechazo(request.estado() == EstadoReview.RECHAZADA ? request.motivoRechazo() : null);
        reviewRepository.save(review);
        return ReviewDto.de(review);
    }

    public CalificacionConfigDto calificacionConfig() {
        return new CalificacionConfigDto(calificacionMin, calificacionMax);
    }
}
