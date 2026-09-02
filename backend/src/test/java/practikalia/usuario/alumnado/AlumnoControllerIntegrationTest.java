package practikalia.usuario.alumnado;

import practikalia.grado.Grado;
import practikalia.grado.GradoRepository;
import practikalia.usuario.Rol;
import practikalia.usuario.Usuario;
import practikalia.usuario.UsuarioRepository;
import practikalia.usuario.correo.CorreoPermitidoRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
class AlumnoControllerIntegrationTest {

    private static final String CABECERA = "nombre,apellido1,apellido2,dni,correo,grado,anio\n";

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private GradoRepository gradoRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CorreoPermitidoRepository correoPermitidoRepository;

    private final RequestPostProcessor comoProfesor =
            user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"));
    private final RequestPostProcessor comoAlumno =
            user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"));

    private Usuario guardarAlumno(String correo, String nombre, boolean activo) {
        Usuario alumno = new Usuario(correo, passwordEncoder.encode("Password123!"), Rol.ALUMNO);
        alumno.setNombre(nombre);
        alumno.setApellido1("Apellido");
        alumno.setActivo(activo);
        return usuarioRepository.save(alumno);
    }

    private MockMultipartFile csv(String cuerpo) {
        return new MockMultipartFile("fichero", "alumnado.csv", "text/csv", cuerpo.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void alumnoNoPuedeListarAlAlumnado() throws Exception {
        mockMvc.perform(get("/api/alumnos").with(comoAlumno))
                .andExpect(status().isForbidden());
    }

    @Test
    void laPastillaDePorConfirmarFiltraPorActivo() throws Exception {
        guardarAlumno("confirmado@iesejemplo.es", "Ana", true);
        guardarAlumno("pendiente@iesejemplo.es", "Beto", false);

        mockMvc.perform(get("/api/alumnos").param("activo", "false").with(comoProfesor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].correo").value("pendiente@iesejemplo.es"))
                .andExpect(jsonPath("$.contenido[0].activo").value(false));

        mockMvc.perform(get("/api/alumnos").with(comoProfesor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void laPlantillaEsSoloLaCabecera() throws Exception {
        mockMvc.perform(get("/api/alumnos/plantilla.csv").with(comoProfesor))
                .andExpect(status().isOk())
                .andExpect(content().string(CABECERA));
    }

    @Test
    void importarCreaLasCuentasSinConfirmarYConElDniDeContrasena() throws Exception {
        gradoRepository.save(new Grado("DAW"));

        mockMvc.perform(multipart("/api/alumnos/importar")
                        .file(csv(CABECERA + "Lucía,Ramírez,Ortega,12345678Z,lucia@iesejemplo.es,DAW,2026\n"))
                        .with(csrf())
                        .with(comoProfesor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creados").value(1));

        Usuario creado = usuarioRepository.findByCorreo("lucia@iesejemplo.es").orElseThrow();
        assertThat(creado.isActivo()).isFalse();
        assertThat(creado.isDebeCambiarContrasena()).isTrue();
        assertThat(creado.getNombre()).isEqualTo("Lucía");
        assertThat(creado.getGrado().getNombre()).isEqualTo("DAW");
        assertThat(passwordEncoder.matches("12345678", creado.getContrasenaHash())).isTrue();
        // Sin la entrada en la whitelist, `login` rechazaría la cuenta para siempre.
        assertThat(correoPermitidoRepository.existsByCorreo("lucia@iesejemplo.es")).isTrue();
    }

    @Test
    void importarConUnDominioDeFueraDelCentroFalla() throws Exception {
        gradoRepository.save(new Grado("DAW"));

        mockMvc.perform(multipart("/api/alumnos/importar")
                        .file(csv(CABECERA + "Lucía,Ramírez,,12345678Z,lucia@gmail.com,DAW,2026\n"))
                        .with(csrf())
                        .with(comoProfesor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        org.hamcrest.Matchers.containsString("dominio")));

        assertThat(usuarioRepository.findByCorreo("lucia@gmail.com")).isEmpty();
    }

    @Test
    void importarAdmiteElPuntoYComaDeExcelEnEspanol() throws Exception {
        gradoRepository.save(new Grado("DAM"));

        mockMvc.perform(multipart("/api/alumnos/importar")
                        .file(csv("nombre;apellido1;apellido2;dni;correo;grado;anio\n"
                                + "Iván;Cabrera;;12345678Z;ivan@iesejemplo.es;DAM;2026\n"))
                        .with(csrf())
                        .with(comoProfesor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creados").value(1));
    }

    @Test
    void unaFilaMalaNoImportaNingunaYDiceLaLinea() throws Exception {
        gradoRepository.save(new Grado("DAW"));

        mockMvc.perform(multipart("/api/alumnos/importar")
                        .file(csv(CABECERA
                                + "Lucía,Ramírez,Ortega,12345678Z,lucia@iesejemplo.es,DAW,2026\n"
                                + "Iván,Cabrera,,12345678A,ivan@iesejemplo.es,DAW,2026\n"))
                        .with(csrf())
                        .with(comoProfesor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CSV_INVALIDO"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("Línea 3")));

        // Todo o nada: ni siquiera la primera fila, que era correcta.
        assertThat(usuarioRepository.findByCorreo("lucia@iesejemplo.es")).isEmpty();
    }

    @Test
    void importarConUnaClaseInexistenteFalla() throws Exception {
        mockMvc.perform(multipart("/api/alumnos/importar")
                        .file(csv(CABECERA + "Lucía,Ramírez,,12345678Z,lucia@iesejemplo.es,NOEXISTE,2026\n"))
                        .with(csrf())
                        .with(comoProfesor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("NOEXISTE")));
    }

    @Test
    void editarCambiaLaFichaSinTocarLaContrasena() throws Exception {
        Grado grado = gradoRepository.save(new Grado("ASIR"));
        Usuario alumno = guardarAlumno("viejo@iesejemplo.es", "Ana", false);
        String hashAntes = alumno.getContrasenaHash();

        mockMvc.perform(put("/api/alumnos/" + alumno.getId())
                        .with(csrf())
                        .with(comoProfesor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EditarAlumnoRequest(
                                "Ana", "Ruiz", null, "12345678Z", "nuevo@iesejemplo.es", grado.getId(), 2026))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("nuevo@iesejemplo.es"))
                .andExpect(jsonPath("$.grado.nombre").value("ASIR"));

        Usuario editado = usuarioRepository.findById(alumno.getId()).orElseThrow();
        assertThat(editado.getContrasenaHash()).isEqualTo(hashAntes);
    }

    @Test
    void editarConElCorreoDeOtraCuentaDevuelve409() throws Exception {
        guardarAlumno("ocupado@iesejemplo.es", "Otro", true);
        Usuario alumno = guardarAlumno("mio@iesejemplo.es", "Ana", true);

        mockMvc.perform(put("/api/alumnos/" + alumno.getId())
                        .with(csrf())
                        .with(comoProfesor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EditarAlumnoRequest(
                                "Ana", "Ruiz", null, "12345678Z", "ocupado@iesejemplo.es", null, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CORREO_YA_EXISTE"));
    }
}
