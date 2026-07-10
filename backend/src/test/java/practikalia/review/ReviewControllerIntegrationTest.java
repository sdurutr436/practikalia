package practikalia.review;

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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReviewControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private EtiquetaRepository etiquetaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EmpresaRepository empresaRepository;
    @Autowired
    private AsignacionRepository asignacionRepository;
    @Autowired
    private GradoRepository gradoRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario profesor;
    private Usuario otroProfesor;
    private Usuario alumno;
    private Empresa empresa;
    private Grado grado;

    @BeforeEach
    void setUp() {
        Etiqueta sector = etiquetaRepository.save(new Etiqueta("Tecnología"));
        profesor = usuarioRepository.save(new Usuario("prof@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.PROFESOR));
        otroProfesor = usuarioRepository.save(new Usuario("otro@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.PROFESOR));
        alumno = usuarioRepository.save(new Usuario("alumno@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.ALUMNO));
        empresa = empresaRepository.save(new Empresa("Acme", "d", "dir", sector, "obs", "c", "t", "e", profesor));
        grado = gradoRepository.save(new Grado("DAW"));
    }

    private Asignacion crearAsignacion(Usuario alumno) {
        return asignacionRepository.save(new Asignacion(alumno, empresa, profesor, grado, 1, LocalDate.of(2026, 1, 15)));
    }

    private CrearReviewRequest request(Long asignacionId, int calificacion) {
        return new CrearReviewRequest(asignacionId, "Buena experiencia", calificacion);
    }

    @Test
    void alumnoCreaReviewSobreSiMismoQuedaPendiente() throws Exception {
        Asignacion asignacion = crearAsignacion(alumno);

        mockMvc.perform(post("/api/reviews")
                        .with(csrf())
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(asignacion.getId(), 4))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.autorCorreo").value("alumno@iesejemplo.es"));
    }

    @Test
    void tutorCreaReviewSobreSuAlumnoQuedaAprobada() throws Exception {
        Asignacion asignacion = crearAsignacion(alumno);

        mockMvc.perform(post("/api/reviews")
                        .with(csrf())
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(asignacion.getId(), 5))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("APROBADA"))
                .andExpect(jsonPath("$.autorCorreo").value("prof@iesejemplo.es"));
    }

    @Test
    void crearReviewSinAsignacionDevuelve404() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .with(csrf())
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(999L, 4))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("ASIGNACION_NO_ENCONTRADA"));
    }

    @Test
    void profesorQueNoEsElTutorNoPuedeCrearReview() throws Exception {
        Asignacion asignacion = crearAsignacion(alumno);

        mockMvc.perform(post("/api/reviews")
                        .with(csrf())
                        .with(user(otroProfesor.getCorreo()).authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(asignacion.getId(), 4))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void alumnoVeAprobadasYPropiasProfesorVeTodas() throws Exception {
        Asignacion asignacion = crearAsignacion(alumno);
        reviewRepository.save(new Review(asignacion, alumno, "propia", 4, EstadoReview.PENDIENTE));
        Usuario otroAlumno = usuarioRepository.save(new Usuario("otro-alumno@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.ALUMNO));
        Asignacion asignacionOtro = crearAsignacion(otroAlumno);
        reviewRepository.save(new Review(asignacionOtro, otroAlumno, "ajena pendiente", 3, EstadoReview.PENDIENTE));

        mockMvc.perform(get("/api/empresas/" + empresa.getId() + "/reviews")
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/empresas/" + empresa.getId() + "/reviews")
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void profesorConsultaPendientesAlumnoRecibe403() throws Exception {
        Asignacion asignacion = crearAsignacion(alumno);
        reviewRepository.save(new Review(asignacion, alumno, "propia", 4, EstadoReview.PENDIENTE));

        mockMvc.perform(get("/api/reviews/pendientes")
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/reviews/pendientes")
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void profesorModeraAprobandoYRechazando() throws Exception {
        Asignacion asignacion = crearAsignacion(alumno);
        Review paraAprobar = reviewRepository.save(new Review(asignacion, alumno, "propia", 4, EstadoReview.PENDIENTE));
        Usuario otroAlumno = usuarioRepository.save(new Usuario("otro-alumno@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.ALUMNO));
        Asignacion asignacionOtro = crearAsignacion(otroAlumno);
        Review paraRechazar = reviewRepository.save(new Review(asignacionOtro, otroAlumno, "otra", 2, EstadoReview.PENDIENTE));

        mockMvc.perform(put("/api/reviews/" + paraAprobar.getId() + "/moderar")
                        .with(csrf())
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ModerarReviewRequest(EstadoReview.APROBADA, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"))
                .andExpect(jsonPath("$.moderadaPorCorreo").value("prof@iesejemplo.es"));

        mockMvc.perform(put("/api/reviews/" + paraRechazar.getId() + "/moderar")
                        .with(csrf())
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ModerarReviewRequest(EstadoReview.RECHAZADA, "Contenido inapropiado"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.motivoRechazo").value("Contenido inapropiado"));
    }

    @Test
    void alumnoNoPuedeModerarDevuelve403() throws Exception {
        Asignacion asignacion = crearAsignacion(alumno);
        Review review = reviewRepository.save(new Review(asignacion, alumno, "propia", 4, EstadoReview.PENDIENTE));

        mockMvc.perform(put("/api/reviews/" + review.getId() + "/moderar")
                        .with(csrf())
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ModerarReviewRequest(EstadoReview.APROBADA, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void autorAlumnoEditaReviewYVuelveAPendiente() throws Exception {
        Asignacion asignacion = crearAsignacion(alumno);
        Review review = reviewRepository.save(new Review(asignacion, alumno, "propia", 4, EstadoReview.APROBADA));

        mockMvc.perform(put("/api/reviews/" + review.getId())
                        .with(csrf())
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EditarReviewRequest("editada", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.contenido").value("editada"));
    }

    @Test
    void autorProfesorEditaReviewMantieneAprobada() throws Exception {
        Asignacion asignacion = crearAsignacion(alumno);
        Review review = reviewRepository.save(new Review(asignacion, profesor, "propia", 4, EstadoReview.APROBADA));

        mockMvc.perform(put("/api/reviews/" + review.getId())
                        .with(csrf())
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EditarReviewRequest("editada", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"));
    }

    @Test
    void editarSinSerAutorDevuelve403() throws Exception {
        Asignacion asignacion = crearAsignacion(alumno);
        Review review = reviewRepository.save(new Review(asignacion, alumno, "propia", 4, EstadoReview.APROBADA));

        mockMvc.perform(put("/api/reviews/" + review.getId())
                        .with(csrf())
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EditarReviewRequest("editada", 5))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void calificacionConfigDevuelveRangoConfigurado() throws Exception {
        mockMvc.perform(get("/api/reviews/calificacion-config")
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.min").value(1))
                .andExpect(jsonPath("$.max").value(5));
    }
}
