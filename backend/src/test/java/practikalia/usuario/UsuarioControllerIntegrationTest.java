package practikalia.usuario;

import practikalia.grado.Grado;
import practikalia.grado.GradoRepository;
import practikalia.usuario.correo.CorreoPermitido;
import practikalia.usuario.correo.CorreoPermitidoRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "allowed.domains=iesejemplo.es")
class UsuarioControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private CorreoPermitidoRepository correoPermitidoRepository;
    @Autowired
    private GradoRepository gradoRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private void guardarProfesor(String correo, boolean esAdmin) {
        Usuario profesor = new Usuario(correo, passwordEncoder.encode("Password123!"), Rol.PROFESOR);
        profesor.setEsAdmin(esAdmin);
        usuarioRepository.save(profesor);
    }

    @Test
    void altaDeAlumnoPorProfesorEsExitosa() throws Exception {
        guardarProfesor("prof1@iesejemplo.es", false);

        mockMvc.perform(post("/api/usuarios")
                        .with(csrf())
                        .with(user("prof1@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CrearUsuarioRequest("ana@iesejemplo.es", Rol.ALUMNO))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value("ana@iesejemplo.es"))
                .andExpect(jsonPath("$.contrasenaTemporal").exists());
    }

    @Test
    void altaConCorreoNoPermitidoDevuelve403() throws Exception {
        guardarProfesor("prof2@iesejemplo.es", false);

        mockMvc.perform(post("/api/usuarios")
                        .with(csrf())
                        .with(user("prof2@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CrearUsuarioRequest("ana@gmail.com", Rol.ALUMNO))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CORREO_NO_PERMITIDO"));
    }

    @Test
    void altaConCorreoYaRegistradoDevuelve409() throws Exception {
        guardarProfesor("prof3@iesejemplo.es", false);
        guardarProfesor("existente@iesejemplo.es", false);

        mockMvc.perform(post("/api/usuarios")
                        .with(csrf())
                        .with(user("prof3@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CrearUsuarioRequest("existente@iesejemplo.es", Rol.ALUMNO))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CORREO_YA_REGISTRADO"));
    }

    @Test
    void altaViaWhitelistSinDominioPermitidoEsExitosa() throws Exception {
        guardarProfesor("prof4@iesejemplo.es", false);
        correoPermitidoRepository.save(new CorreoPermitido("suelto@gmail.com"));

        mockMvc.perform(post("/api/usuarios")
                        .with(csrf())
                        .with(user("prof4@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CrearUsuarioRequest("suelto@gmail.com", Rol.ALUMNO))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value("suelto@gmail.com"));
    }

    @Test
    void profesorCreandoOtroProfesorDevuelve403() throws Exception {
        guardarProfesor("prof5@iesejemplo.es", false);

        mockMvc.perform(post("/api/usuarios")
                        .with(csrf())
                        .with(user("prof5@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CrearUsuarioRequest("otro@iesejemplo.es", Rol.PROFESOR))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void esAdminCreandoOtroProfesorEsExitosa() throws Exception {
        guardarProfesor("admin1@iesejemplo.es", true);

        mockMvc.perform(post("/api/usuarios")
                        .with(csrf())
                        .with(user("admin1@iesejemplo.es").authorities(
                                new SimpleGrantedAuthority("ROLE_PROFESOR"), new SimpleGrantedAuthority("ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CrearUsuarioRequest("nuevo-profe@iesejemplo.es", Rol.PROFESOR))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("PROFESOR"));
    }

    @Test
    void alumnoNoPuedeCrearUsuariosDevuelve403() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .with(csrf())
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CrearUsuarioRequest("otro@iesejemplo.es", Rol.ALUMNO))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void profesorActualizaGradoDeAlumno() throws Exception {
        guardarProfesor("prof6@iesejemplo.es", false);
        Usuario alumno = usuarioRepository.save(new Usuario("alumno@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.ALUMNO));
        Grado grado = gradoRepository.save(new Grado("DAW"));

        mockMvc.perform(put("/api/usuarios/" + alumno.getId() + "/grado")
                        .with(csrf())
                        .with(user("prof6@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActualizarGradoRequest(grado.getId(), 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grado.nombre").value("DAW"))
                .andExpect(jsonPath("$.anio").value(2));
    }

    @Test
    void alumnoNoPuedeActualizarGradoDevuelve403() throws Exception {
        Usuario alumno = usuarioRepository.save(new Usuario("alumno2@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.ALUMNO));
        Grado grado = gradoRepository.save(new Grado("DAM"));

        mockMvc.perform(put("/api/usuarios/" + alumno.getId() + "/grado")
                        .with(csrf())
                        .with(user("alumno2@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActualizarGradoRequest(grado.getId(), 1))))
                .andExpect(status().isForbidden());
    }
}
