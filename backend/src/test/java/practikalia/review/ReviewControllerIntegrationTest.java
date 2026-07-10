package practikalia.review;

import practikalia.asignacion.Asignacion;
import practikalia.asignacion.AsignacionRepository;
import practikalia.empresa.Empresa;
import practikalia.empresa.EmpresaRepository;
import practikalia.etiqueta.Etiqueta;
import practikalia.etiqueta.EtiquetaRepository;
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
    private ReviewRepository reviewRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario profesor;
    private Usuario otroProfesor;
    private Usuario alumno;
    private Empresa empresa;

    @BeforeEach
    void setUp() {
        Etiqueta sector = etiquetaRepository.save(new Etiqueta("Tecnología"));
        profesor = usuarioRepository.save(new Usuario("prof@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.PROFESOR));
        otroProfesor = usuarioRepository.save(new Usuario("otro@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.PROFESOR));
        alumno = usuarioRepository.save(new Usuario("alumno@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.ALUMNO));
        empresa = empresaRepository.save(new Empresa("Acme", "d", "dir", sector, "obs", "c", "t", "e", profesor));
    }

    private void crearAsignacion() {
        asignacionRepository.save(new Asignacion(alumno, empresa, profesor, LocalDate.of(2026, 1, 15)));
    }

    private CrearReviewRequest request(int calificacion) {
        return new CrearReviewRequest(empresa.getId(), alumno.getId(), "Buena experiencia", calificacion);
    }

    @Test
    void alumnoCreaReviewSobreSiMismoQuedaPendiente() throws Exception {
        crearAsignacion();

        mockMvc.perform(post("/api/reviews")
                        .with(csrf())
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(4))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.autorCorreo").value("alumno@iesejemplo.es"));
    }

    @Test
    void tutorCreaReviewSobreSuAlumnoQuedaAprobada() throws Exception {
        crearAsignacion();

        mockMvc.perform(post("/api/reviews")
                        .with(csrf())
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(5))))
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
                        .content(objectMapper.writeValueAsString(request(4))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("ASIGNACION_NO_ENCONTRADA"));
    }

    @Test
    void profesorQueNoEsElTutorNoPuedeCrearReview() throws Exception {
        crearAsignacion();

        mockMvc.perform(post("/api/reviews")
                        .with(csrf())
                        .with(user(otroProfesor.getCorreo()).authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(4))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void alumnoVeAprobadasYPropiasProfesorVeTodas() throws Exception {
        crearAsignacion();
        reviewRepository.save(new Review(alumno, alumno, empresa, "propia", 4, EstadoReview.PENDIENTE));
        Usuario otroAlumno = usuarioRepository.save(new Usuario("otro-alumno@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.ALUMNO));
        asignacionRepository.save(new Asignacion(otroAlumno, empresa, profesor, LocalDate.of(2026, 1, 15)));
        reviewRepository.save(new Review(otroAlumno, otroAlumno, empresa, "ajena pendiente", 3, EstadoReview.PENDIENTE));

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
        reviewRepository.save(new Review(alumno, alumno, empresa, "propia", 4, EstadoReview.PENDIENTE));

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
        Review paraAprobar = reviewRepository.save(new Review(alumno, alumno, empresa, "propia", 4, EstadoReview.PENDIENTE));
        Usuario otroAlumno = usuarioRepository.save(new Usuario("otro-alumno@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.ALUMNO));
        Review paraRechazar = reviewRepository.save(new Review(otroAlumno, otroAlumno, empresa, "otra", 2, EstadoReview.PENDIENTE));

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
        Review review = reviewRepository.save(new Review(alumno, alumno, empresa, "propia", 4, EstadoReview.PENDIENTE));

        mockMvc.perform(put("/api/reviews/" + review.getId() + "/moderar")
                        .with(csrf())
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ModerarReviewRequest(EstadoReview.APROBADA, null))))
                .andExpect(status().isForbidden());
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
