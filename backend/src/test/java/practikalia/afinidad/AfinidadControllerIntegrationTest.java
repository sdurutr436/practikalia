package practikalia.afinidad;

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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AfinidadControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EtiquetaRepository etiquetaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EmpresaRepository empresaRepository;
    @Autowired
    private GradoRepository gradoRepository;
    @Autowired
    private AsignacionRepository asignacionRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario alumno;
    private Usuario otroAlumno;
    private Usuario tutor;
    private Grado grado;
    private Empresa devs;

    private final RequestPostProcessor comoAlumno =
            user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"));
    private final RequestPostProcessor comoOtroAlumno =
            user("otro@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"));
    private final RequestPostProcessor comoTutor =
            user("tutor@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"));
    private final RequestPostProcessor comoOtroProfesor =
            user("otroprof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"));
    private final RequestPostProcessor comoAdmin =
            user("admin@iesejemplo.es").authorities(
                    new SimpleGrantedAuthority("ROLE_PROFESOR"), new SimpleGrantedAuthority("ADMIN"));

    @BeforeEach
    void setUp() {
        Etiqueta java = etiquetaRepository.save(new Etiqueta("Java"));
        Etiqueta redes = etiquetaRepository.save(new Etiqueta("Redes"));
        Etiqueta tecnologia = etiquetaRepository.save(new Etiqueta("Tecnología"));
        Etiqueta hosteleria = etiquetaRepository.save(new Etiqueta("Hostelería"));

        tutor = usuarioRepository.save(
                new Usuario("tutor@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.PROFESOR));
        usuarioRepository.save(
                new Usuario("otroprof@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.PROFESOR));
        alumno = new Usuario("alumno@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.ALUMNO);
        alumno.setEtiquetas(new ArrayList<>(List.of(java, redes)));
        alumno = usuarioRepository.save(alumno);
        otroAlumno = usuarioRepository.save(
                new Usuario("otro@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.ALUMNO));
        grado = gradoRepository.save(new Grado("DAW"));

        // Scores para el alumno con {Java, Redes}: Antenas 1.2 (1.0 + bonus sector), Devs 1.0, Cafetería 0.0
        empresaPublicada("Antenas", redes, redes);
        devs = empresaPublicada("Devs", tecnologia, java, redes);
        empresaPublicada("Cafetería", hosteleria);
        Empresa oculta = empresaPublicada("Oculta", tecnologia, java, redes);
        oculta.setPublicada(false);
        empresaRepository.save(oculta);

        asignacionRepository.save(new Asignacion(alumno, devs, tutor, grado, 1, LocalDate.now()));
    }

    private Empresa empresaPublicada(String nombre, Etiqueta sector, Etiqueta... etiquetas) {
        Empresa empresa = new Empresa(nombre, null, null, sector, null, null, null, null, tutor);
        empresa.setEtiquetas(new ArrayList<>(List.of(etiquetas)));
        empresa.setPublicada(true);
        return empresaRepository.save(empresa);
    }

    @Test
    void alumnoVeSuPropiaAfinidadOrdenadaYExplicada() throws Exception {
        mockMvc.perform(get("/api/empresas/afinidad").with(comoAlumno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alumnoConEtiquetas").value(true))
                .andExpect(jsonPath("$.empresas.length()").value(3))
                .andExpect(jsonPath("$.empresas[0].empresa.nombre").value("Antenas"))
                .andExpect(jsonPath("$.empresas[0].score").value(1.2))
                .andExpect(jsonPath("$.empresas[0].sectorCoincide").value(true))
                .andExpect(jsonPath("$.empresas[1].empresa.nombre").value("Devs"))
                .andExpect(jsonPath("$.empresas[1].etiquetasCoincidentes.length()").value(2))
                .andExpect(jsonPath("$.empresas[1].sectorCoincide").value(false))
                .andExpect(jsonPath("$.empresas[2].empresa.nombre").value("Cafetería"))
                .andExpect(jsonPath("$.empresas[2].score").value(0.0));
    }

    @Test
    void profesorYAdminEnElEndpointDeAlumnoDevuelven403() throws Exception {
        mockMvc.perform(get("/api/empresas/afinidad").with(comoTutor))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/empresas/afinidad").with(comoAdmin))
                .andExpect(status().isForbidden());
    }

    @Test
    void alumnoConsultandoAfinidadPorIdInclusoLaSuyaDevuelve403() throws Exception {
        mockMvc.perform(get("/api/alumnos/" + alumno.getId() + "/afinidad").with(comoAlumno))
                .andExpect(status().isForbidden());
    }

    @Test
    void tutorDeLaAsignacionActivaVeLaAfinidadDelAlumno() throws Exception {
        mockMvc.perform(get("/api/alumnos/" + alumno.getId() + "/afinidad").with(comoTutor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alumnoConEtiquetas").value(true))
                .andExpect(jsonPath("$.empresas[0].empresa.nombre").value("Antenas"));
    }

    @Test
    void profesorSinEsaTutoriaDevuelve403() throws Exception {
        mockMvc.perform(get("/api/alumnos/" + alumno.getId() + "/afinidad").with(comoOtroProfesor))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void tutorDeUnaAsignacionYaCerradaDevuelve403() throws Exception {
        Asignacion cerrada = new Asignacion(otroAlumno, devs, tutor, grado, 1, LocalDate.now().minusYears(1));
        cerrada.setFechaFin(LocalDate.now().minusMonths(6));
        asignacionRepository.save(cerrada);

        mockMvc.perform(get("/api/alumnos/" + otroAlumno.getId() + "/afinidad").with(comoTutor))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void adminVeCualquierAlumnoInclusoSinAsignacionActiva() throws Exception {
        mockMvc.perform(get("/api/alumnos/" + otroAlumno.getId() + "/afinidad").with(comoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alumnoConEtiquetas").value(false));
    }

    @Test
    void alumnoSinEtiquetasRecibeListadoAlfabeticoConFlagApagado() throws Exception {
        mockMvc.perform(get("/api/empresas/afinidad").with(comoOtroAlumno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alumnoConEtiquetas").value(false))
                .andExpect(jsonPath("$.empresas.length()").value(3))
                .andExpect(jsonPath("$.empresas[0].empresa.nombre").value("Antenas"))
                .andExpect(jsonPath("$.empresas[1].empresa.nombre").value("Cafetería"))
                .andExpect(jsonPath("$.empresas[2].empresa.nombre").value("Devs"));
    }
}
