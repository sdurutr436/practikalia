package practikalia.review;

import practikalia.asignacion.AsignacionDto;
import practikalia.asignacion.CrearAsignacionRequest;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Flujo completo de Fase 5: mismo alumno + empresa en dos años distintos (recurrencia),
 * repetir año da 409, y editar la review original la reenvía a moderación.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecurrenciaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    @Autowired
    private EtiquetaRepository etiquetaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EmpresaRepository empresaRepository;
    @Autowired
    private GradoRepository gradoRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario profesor;
    private Usuario alumno;
    private Empresa empresa;
    private Grado grado;

    @BeforeEach
    void setUp() {
        Etiqueta sector = etiquetaRepository.save(new Etiqueta("Tecnología"));
        profesor = usuarioRepository.save(new Usuario("prof@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.PROFESOR));
        alumno = usuarioRepository.save(new Usuario("alumno@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.ALUMNO));
        empresa = empresaRepository.save(new Empresa("Acme", "d", "dir", sector, "obs", "c", "t", "e", profesor));
        grado = gradoRepository.save(new Grado("DAW"));
    }

    private CrearAsignacionRequest asignacionRequest(int anio, LocalDate fechaInicio) {
        return new CrearAsignacionRequest(alumno.getId(), empresa.getId(), profesor.getId(), grado.getId(), anio, fechaInicio);
    }

    @Test
    void flujoCompletoDeRecurrencia() throws Exception {
        MvcResult crearAnio1 = mockMvc.perform(post("/api/asignaciones")
                        .with(csrf())
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(asignacionRequest(1, LocalDate.of(2026, 1, 15)))))
                .andExpect(status().isCreated())
                .andReturn();
        Long asignacionAnio1Id = objectMapper.readValue(
                crearAnio1.getResponse().getContentAsString(), AsignacionDto.class).id();

        MvcResult crearReview = mockMvc.perform(post("/api/reviews")
                        .with(csrf())
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CrearReviewRequest(asignacionAnio1Id, "Buena experiencia", 4))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andReturn();
        Long reviewId = objectMapper.readValue(crearReview.getResponse().getContentAsString(), ReviewDto.class).id();

        mockMvc.perform(put("/api/reviews/" + reviewId + "/moderar")
                        .with(csrf())
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ModerarReviewRequest(EstadoReview.APROBADA, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"));

        mockMvc.perform(post("/api/asignaciones")
                        .with(csrf())
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(asignacionRequest(2, LocalDate.of(2027, 1, 15)))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/asignaciones")
                        .with(csrf())
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(asignacionRequest(1, LocalDate.of(2026, 1, 15)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ASIGNACION_YA_EXISTE"));

        mockMvc.perform(put("/api/reviews/" + reviewId)
                        .with(csrf())
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EditarReviewRequest("Experiencia revisada", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.contenido").value("Experiencia revisada"));

        mockMvc.perform(put("/api/reviews/" + reviewId + "/moderar")
                        .with(csrf())
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ModerarReviewRequest(EstadoReview.APROBADA, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"));
    }
}
