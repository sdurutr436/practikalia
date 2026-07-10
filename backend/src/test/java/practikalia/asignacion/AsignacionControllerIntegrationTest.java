package practikalia.asignacion;

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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AsignacionControllerIntegrationTest {

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
    private AsignacionRepository asignacionRepository;
    @Autowired
    private GradoRepository gradoRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario profesor;
    private Usuario alumno;
    private Usuario otroAlumno;
    private Empresa empresa;
    private Grado grado;

    @BeforeEach
    void setUp() {
        Etiqueta sector = etiquetaRepository.save(new Etiqueta("Tecnología"));
        profesor = usuarioRepository.save(new Usuario("prof@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.PROFESOR));
        alumno = usuarioRepository.save(new Usuario("alumno@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.ALUMNO));
        otroAlumno = usuarioRepository.save(new Usuario("otro@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.ALUMNO));
        empresa = empresaRepository.save(new Empresa("Acme", "d", "dir", sector, "obs", "c", "t", "e", profesor));
        grado = gradoRepository.save(new Grado("DAW"));
    }

    private CrearAsignacionRequest request() {
        return new CrearAsignacionRequest(alumno.getId(), empresa.getId(), profesor.getId(), grado.getId(), 1, LocalDate.of(2026, 1, 15));
    }

    @Test
    void profesorCreaAsignacion() throws Exception {
        mockMvc.perform(post("/api/asignaciones")
                        .with(csrf())
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alumnoCorreo").value("alumno@iesejemplo.es"))
                .andExpect(jsonPath("$.tutorCentroCorreo").value("prof@iesejemplo.es"));
    }

    @Test
    void mismoAlumnoEmpresaDistintoAnioNoEsConflicto() throws Exception {
        asignacionRepository.save(new Asignacion(alumno, empresa, profesor, grado, 1, LocalDate.of(2026, 1, 15)));

        mockMvc.perform(post("/api/asignaciones")
                        .with(csrf())
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CrearAsignacionRequest(alumno.getId(), empresa.getId(), profesor.getId(), grado.getId(), 2, LocalDate.of(2027, 1, 15)))))
                .andExpect(status().isCreated());
    }

    @Test
    void mismoAlumnoEmpresaGradoYAnioEsConflicto() throws Exception {
        asignacionRepository.save(new Asignacion(alumno, empresa, profesor, grado, 1, LocalDate.of(2026, 1, 15)));

        mockMvc.perform(post("/api/asignaciones")
                        .with(csrf())
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ASIGNACION_YA_EXISTE"));
    }

    @Test
    void alumnoNoPuedeCrearAsignacionDevuelve403() throws Exception {
        mockMvc.perform(post("/api/asignaciones")
                        .with(csrf())
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void alumnoConsultaSusPropiasAsignaciones() throws Exception {
        asignacionRepository.save(new Asignacion(alumno, empresa, profesor, grado, 1, LocalDate.of(2026, 1, 15)));

        mockMvc.perform(get("/api/alumnos/" + alumno.getId() + "/asignaciones")
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void alumnoConsultandoAsignacionesDeOtroAlumnoDevuelve403() throws Exception {
        mockMvc.perform(get("/api/alumnos/" + alumno.getId() + "/asignaciones")
                        .with(user(otroAlumno.getCorreo()).authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void profesorConsultaAsignacionesDeCualquierAlumno() throws Exception {
        asignacionRepository.save(new Asignacion(alumno, empresa, profesor, grado, 1, LocalDate.of(2026, 1, 15)));

        mockMvc.perform(get("/api/alumnos/" + alumno.getId() + "/asignaciones")
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void profesorConsultaAsignacionesDeUnaEmpresa() throws Exception {
        asignacionRepository.save(new Asignacion(alumno, empresa, profesor, grado, 1, LocalDate.of(2026, 1, 15)));

        mockMvc.perform(get("/api/empresas/" + empresa.getId() + "/asignaciones")
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].empresaNombre").value("Acme"));
    }

    @Test
    void alumnoConsultandoAsignacionesDeEmpresaDevuelve403() throws Exception {
        mockMvc.perform(get("/api/empresas/" + empresa.getId() + "/asignaciones")
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void profesorCierraAsignacion() throws Exception {
        Asignacion asignacion = asignacionRepository.save(new Asignacion(alumno, empresa, profesor, grado, 1, LocalDate.of(2026, 1, 15)));

        mockMvc.perform(put("/api/asignaciones/" + asignacion.getId())
                        .with(csrf())
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActualizarAsignacionRequest(LocalDate.of(2026, 6, 30), null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaFin").value("2026-06-30"))
                .andExpect(jsonPath("$.contratadoPosterior").value((Object) null));
    }

    @Test
    void alumnoConsultaTasaContratacionExcluyendoNoDecididas() throws Exception {
        asignacionRepository.save(cerrada(1, true));
        asignacionRepository.save(cerrada(2, false));
        asignacionRepository.save(cerrada(3, null));
        asignacionRepository.save(new Asignacion(alumno, empresa, profesor, grado, 4, LocalDate.of(2026, 1, 15)));

        mockMvc.perform(get("/api/empresas/" + empresa.getId() + "/tasa-contratacion")
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asignacionesDecididas").value(2))
                .andExpect(jsonPath("$.contrataciones").value(1))
                .andExpect(jsonPath("$.tasa").value(0.5));
    }

    @Test
    void tasaContratacionSinDecididasEsCero() throws Exception {
        mockMvc.perform(get("/api/empresas/" + empresa.getId() + "/tasa-contratacion")
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asignacionesDecididas").value(0))
                .andExpect(jsonPath("$.tasa").value(0.0));
    }

    @Test
    void tasaContratacionEmpresaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/empresas/99999/tasa-contratacion")
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("EMPRESA_NO_ENCONTRADA"));
    }

    private Asignacion cerrada(int anio, Boolean contratadoPosterior) {
        Asignacion asignacion = new Asignacion(alumno, empresa, profesor, grado, anio, LocalDate.of(2026, 1, 15));
        asignacion.setFechaFin(LocalDate.of(2026, 6, 30));
        asignacion.setContratadoPosterior(contratadoPosterior);
        return asignacion;
    }

    @Test
    void profesorMarcaContratadoPosteriorEnLlamadaPosterior() throws Exception {
        Asignacion asignacion = asignacionRepository.save(new Asignacion(alumno, empresa, profesor, grado, 1, LocalDate.of(2026, 1, 15)));

        mockMvc.perform(put("/api/asignaciones/" + asignacion.getId())
                        .with(csrf())
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActualizarAsignacionRequest(LocalDate.of(2026, 6, 30), null))))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/asignaciones/" + asignacion.getId())
                        .with(csrf())
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActualizarAsignacionRequest(LocalDate.of(2026, 6, 30), true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaFin").value("2026-06-30"))
                .andExpect(jsonPath("$.contratadoPosterior").value(true));
    }
}
