package practikalia.usuario.alumnado;

import practikalia.asignacion.Curso;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                        .content(objectMapper.writeValueAsString(new FichaAlumnoRequest(
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
                        .content(objectMapper.writeValueAsString(new FichaAlumnoRequest(
                                "Ana", "Ruiz", null, "12345678Z", "ocupado@iesejemplo.es", null, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CORREO_YA_EXISTE"));
    }

    private FichaAlumnoRequest ficha(String correo, String dni, Long gradoId) {
        return new FichaAlumnoRequest("Rocío", "Delgado", "Cano", dni, correo, gradoId, 2026);
    }

    @Test
    void elAltaAManoNaceConfirmadaConElDniDeContrasenaYEnLaWhitelist() throws Exception {
        Grado grado = gradoRepository.save(new Grado("DAW"));

        mockMvc.perform(post("/api/alumnos")
                        .with(csrf())
                        .with(comoProfesor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                ficha("rocio@iesejemplo.es", "12345678Z", grado.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value("rocio@iesejemplo.es"))
                .andExpect(jsonPath("$.activo").value(true));

        Usuario creado = usuarioRepository.findByCorreo("rocio@iesejemplo.es").orElseThrow();
        assertThat(creado.isActivo()).isTrue();
        assertThat(creado.isDebeCambiarContrasena()).isTrue();
        assertThat(passwordEncoder.matches("12345678", creado.getContrasenaHash())).isTrue();
        assertThat(correoPermitidoRepository.existsByCorreo("rocio@iesejemplo.es")).isTrue();
    }

    @Test
    void elAltaConUnDniYaUsadoDevuelve409() throws Exception {
        Usuario existente = guardarAlumno("otro@iesejemplo.es", "Otro", true);
        existente.setDni("12345678Z");
        usuarioRepository.save(existente);

        mockMvc.perform(post("/api/alumnos")
                        .with(csrf())
                        .with(comoProfesor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                ficha("rocio@iesejemplo.es", "12345678Z", null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DNI_YA_REGISTRADO"));
    }

    @Test
    void elAltaConUnDominioDeFueraDelCentroDevuelve400() throws Exception {
        mockMvc.perform(post("/api/alumnos")
                        .with(csrf())
                        .with(comoProfesor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                ficha("rocio@gmail.com", "12345678Z", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CORREO_DOMINIO_NO_PERMITIDO"));
    }

    @Test
    void unAlumnoNoPuedeDarDeAltaAOtro() throws Exception {
        mockMvc.perform(post("/api/alumnos")
                        .with(csrf())
                        .with(comoAlumno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                ficha("rocio@iesejemplo.es", "12345678Z", null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void alumnoNoPuedeVerElListadoDelCurso() throws Exception {
        mockMvc.perform(get("/api/alumnos/curso").with(comoAlumno))
                .andExpect(status().isForbidden());
    }

    @Test
    void elListadoDelCursoDejaFueraAlAlumnadoDeCursosAnteriores() throws Exception {
        Usuario esteCurso = guardarAlumno("ana@iesejemplo.es", "Ana", true);
        esteCurso.setAnio(Curso.actual());
        usuarioRepository.save(esteCurso);

        Usuario cursoViejo = guardarAlumno("beto@iesejemplo.es", "Beto", true);
        cursoViejo.setAnio(Curso.actual() - 3);
        usuarioRepository.save(cursoViejo);

        // Sin año de matrícula manda el curso en el que se dio de alta, que es
        // este mismo: se acaba de crear.
        guardarAlumno("carla@iesejemplo.es", "Carla", true);

        // Ordenado por apellido/nombre, y los tres comparten apellido.
        mockMvc.perform(get("/api/alumnos/curso").with(comoProfesor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.contenido[0].correo").value("ana@iesejemplo.es"))
                .andExpect(jsonPath("$.contenido[1].correo").value("carla@iesejemplo.es"));
    }

    @Test
    void elListadoDelCursoFiltraPorClaseYPorTexto() throws Exception {
        Grado daw = gradoRepository.save(new Grado("DAW"));
        Usuario ana = guardarAlumno("ana@iesejemplo.es", "Ana", true);
        ana.setGrado(daw);
        usuarioRepository.save(ana);
        guardarAlumno("beto@iesejemplo.es", "Beto", true);

        // Sin filtro de clase sale también quien no tiene ninguna: el join es left.
        mockMvc.perform(get("/api/alumnos/curso").with(comoProfesor))
                .andExpect(jsonPath("$.total").value(2));

        mockMvc.perform(get("/api/alumnos/curso")
                        .param("gradoId", daw.getId().toString())
                        .with(comoProfesor))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.contenido[0].correo").value("ana@iesejemplo.es"));

        // En mayúsculas y a trozos: el LIKE va en minúsculas por los dos lados.
        mockMvc.perform(get("/api/alumnos/curso").param("texto", "BET").with(comoProfesor))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.contenido[0].correo").value("beto@iesejemplo.es"));
    }

    @Test
    void sePuedeMirarUnCursoDistintoDelQueEstaEnMarcha() throws Exception {
        Usuario viejo = guardarAlumno("beto@iesejemplo.es", "Beto", true);
        viejo.setAnio(2019);
        usuarioRepository.save(viejo);

        mockMvc.perform(get("/api/alumnos/curso").with(comoProfesor))
                .andExpect(jsonPath("$.total").value(0));

        mockMvc.perform(get("/api/alumnos/curso").param("anio", "2019").with(comoProfesor))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.contenido[0].correo").value("beto@iesejemplo.es"));
    }

    @Test
    void elSelectorDeCursosTraeElActualAunqueEsteVacio() throws Exception {
        Usuario viejo = guardarAlumno("beto@iesejemplo.es", "Beto", true);
        viejo.setAnio(2019);
        usuarioRepository.save(viejo);

        mockMvc.perform(get("/api/alumnos/cursos").with(comoProfesor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actual").value(Curso.actual()))
                .andExpect(jsonPath("$.cursos[0]").value(Curso.actual()))
                .andExpect(jsonPath("$.cursos[1]").value(2019));
    }
}
