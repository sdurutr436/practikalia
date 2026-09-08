package practikalia.usuario.profesorado;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "allowed.domains=iesejemplo.es")
class ProfesorControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private GradoRepository gradoRepository;
    @Autowired
    private EtiquetaRepository etiquetaRepository;
    @Autowired
    private EmpresaRepository empresaRepository;
    @Autowired
    private AsignacionRepository asignacionRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final RequestPostProcessor comoAdmin = user("admin@iesejemplo.es")
            .authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"), new SimpleGrantedAuthority("ADMIN"));
    private final RequestPostProcessor comoProfesor =
            user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"));

    private Grado daw;

    @BeforeEach
    void setUp() {
        daw = gradoRepository.save(new Grado("DAW"));
    }

    private Usuario guardarProfesor(String correo, String nombre, boolean esAdmin) {
        Usuario profesor = new Usuario(correo, passwordEncoder.encode("Password123!"), Rol.PROFESOR);
        profesor.setNombre(nombre);
        profesor.setApellido1("Apellido");
        profesor.setEsAdmin(esAdmin);
        return usuarioRepository.save(profesor);
    }

    private FichaProfesorRequest ficha(String correo, String dni, Long gradoId, Boolean esAdmin, List<Long> practicas) {
        return new FichaProfesorRequest("Marta", "Núñez", "Gil", dni, correo, gradoId, esAdmin, practicas);
    }

    @Test
    void unProfesorNoAdminNoDaDeAltaAOtroProfesor() throws Exception {
        mockMvc.perform(post("/api/profesores")
                        .with(csrf())
                        .with(comoProfesor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                ficha("nueva@iesejemplo.es", "12345678Z", null, false, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void elProfesoradoSeLeeEntreSi() throws Exception {
        guardarProfesor("otra@iesejemplo.es", "Ana", false);

        mockMvc.perform(get("/api/profesores").with(comoProfesor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1));
    }

    @Test
    void elAltaNaceConElDniDeContrasenaYLaClaseQueSeLeDa() throws Exception {
        mockMvc.perform(post("/api/profesores")
                        .with(csrf())
                        .with(comoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                ficha("marta@iesejemplo.es", "12345678Z", daw.getId(), true, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.esAdmin").value(true))
                .andExpect(jsonPath("$.clase.nombre").value("DAW"))
                .andExpect(jsonPath("$.alumnosPractica").value(0));

        Usuario creada = usuarioRepository.findByCorreo("marta@iesejemplo.es").orElseThrow();
        assertThat(creada.getRol()).isEqualTo(Rol.PROFESOR);
        assertThat(creada.isActivo()).isTrue();
        assertThat(creada.isDebeCambiarContrasena()).isTrue();
        assertThat(passwordEncoder.matches("12345678", creada.getContrasenaHash())).isTrue();
        assertThat(gradoRepository.findByTutorId(creada.getId())).isPresent();
    }

    @Test
    void laClaseEsExclusiva() throws Exception {
        Usuario anterior = guardarProfesor("anterior@iesejemplo.es", "Ana", false);
        daw.setTutor(anterior);
        gradoRepository.save(daw);
        Usuario nuevo = guardarProfesor("nuevo@iesejemplo.es", "Beto", false);

        mockMvc.perform(put("/api/profesores/" + nuevo.getId())
                        .with(csrf())
                        .with(comoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                ficha("nuevo@iesejemplo.es", "12345678Z", daw.getId(), false, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clase.nombre").value("DAW"));

        assertThat(gradoRepository.findByTutorId(anterior.getId())).isEmpty();
        assertThat(gradoRepository.findByTutorId(nuevo.getId())).isPresent();
    }

    @Test
    void elUltimoAdministradorNoPuedeDejarDeSerlo() throws Exception {
        Usuario unico = guardarProfesor("unico@iesejemplo.es", "Ana", true);

        mockMvc.perform(put("/api/profesores/" + unico.getId())
                        .with(csrf())
                        .with(comoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                ficha("unico@iesejemplo.es", "12345678Z", null, false, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ULTIMO_ADMINISTRADOR"));

        assertThat(usuarioRepository.findById(unico.getId()).orElseThrow().isEsAdmin()).isTrue();
    }

    @Test
    void conDosAdministradoresSiSePuedeQuitarUno() throws Exception {
        guardarProfesor("otra-admin@iesejemplo.es", "Ana", true);
        Usuario segunda = guardarProfesor("segunda@iesejemplo.es", "Beto", true);

        mockMvc.perform(put("/api/profesores/" + segunda.getId())
                        .with(csrf())
                        .with(comoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                ficha("segunda@iesejemplo.es", "12345678Z", null, false, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.esAdmin").value(false));
    }

    @Test
    void laPastillaFiltraPorTutoriaDeClase() throws Exception {
        Usuario conClase = guardarProfesor("con@iesejemplo.es", "Ana", false);
        guardarProfesor("sin@iesejemplo.es", "Beto", false);
        daw.setTutor(conClase);
        gradoRepository.save(daw);

        mockMvc.perform(get("/api/profesores").param("conClase", "true").with(comoProfesor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].correo").value("con@iesejemplo.es"));

        mockMvc.perform(get("/api/profesores").param("conClase", "false").with(comoProfesor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].correo").value("sin@iesejemplo.es"));
    }

    @Test
    void asignarAlumnosDePracticaLosPasaAlNuevoTutor() throws Exception {
        Usuario tutorAnterior = guardarProfesor("anterior@iesejemplo.es", "Ana", false);
        Usuario nuevo = guardarProfesor("nuevo@iesejemplo.es", "Beto", false);
        Usuario alumno = usuarioRepository.save(
                new Usuario("alumno@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.ALUMNO));
        Etiqueta sector = etiquetaRepository.save(new Etiqueta("Tecnología"));
        Empresa empresa = empresaRepository.save(
                new Empresa("Acme", "d", "dir", sector, "obs", "c", "t", "e", tutorAnterior));
        Asignacion abierta = asignacionRepository.save(
                new Asignacion(alumno, empresa, tutorAnterior, daw, 2026, LocalDate.of(2026, 1, 15)));

        mockMvc.perform(put("/api/profesores/" + nuevo.getId())
                        .with(csrf())
                        .with(comoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                ficha("nuevo@iesejemplo.es", "12345678Z", null, false, List.of(alumno.getId())))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alumnosPractica").value(1));

        assertThat(asignacionRepository.findById(abierta.getId()).orElseThrow()
                .getTutorCentro().getId()).isEqualTo(nuevo.getId());
    }
}
