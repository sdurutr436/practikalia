package practikalia.afinidad;

import practikalia.asignacion.Asignacion;
import practikalia.asignacion.AsignacionRepository;
import practikalia.empresa.Empresa;
import practikalia.empresa.EmpresaRepository;
import practikalia.etiqueta.Etiqueta;
import practikalia.etiqueta.EtiquetaRepository;
import practikalia.grado.Grado;
import practikalia.grado.GradoRepository;
import practikalia.usuario.ActualizarEtiquetasRequest;
import practikalia.usuario.Rol;
import practikalia.usuario.Usuario;
import practikalia.usuario.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
class AfinidadFlujoIntegrationTest {

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
    private GradoRepository gradoRepository;
    @Autowired
    private AsignacionRepository asignacionRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void flujoCompletoDeAfinidadConMarcadoDeEtiquetasYVistaDelTutor() throws Exception {
        Etiqueta java = etiquetaRepository.save(new Etiqueta("Java"));
        Etiqueta redes = etiquetaRepository.save(new Etiqueta("Redes"));
        Etiqueta cocina = etiquetaRepository.save(new Etiqueta("Cocina"));
        Etiqueta tecnologia = etiquetaRepository.save(new Etiqueta("Tecnología"));
        Etiqueta hosteleria = etiquetaRepository.save(new Etiqueta("Hostelería"));

        Usuario tutor = usuarioRepository.save(
                new Usuario("tutor@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.PROFESOR));
        usuarioRepository.save(
                new Usuario("otroprof@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.PROFESOR));
        Usuario alumno = usuarioRepository.save(
                new Usuario("alumno@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.ALUMNO));
        Grado grado = gradoRepository.save(new Grado("DAW"));

        empresaPublicada("Antenas", tecnologia, redes, cocina);
        Empresa bit = empresaPublicada("Bit", tecnologia, java, redes);
        empresaPublicada("Cafetería", hosteleria, cocina);

        var comoAlumno = user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"));
        var comoTutor = user("tutor@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"));
        var comoOtroProfesor = user("otroprof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"));

        // 1. Asignación activa alumno–Bit–tutor (Fase 3)
        asignacionRepository.save(new Asignacion(alumno, bit, tutor, grado, 1, LocalDate.now()));

        // 2. Alumno sin etiquetas: listado alfabético con el flag apagado
        mockMvc.perform(get("/api/empresas/afinidad").with(comoAlumno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alumnoConEtiquetas").value(false))
                .andExpect(jsonPath("$.empresas[0].empresa.nombre").value("Antenas"))
                .andExpect(jsonPath("$.empresas[1].empresa.nombre").value("Bit"))
                .andExpect(jsonPath("$.empresas[2].empresa.nombre").value("Cafetería"));

        // 3. El alumno marca etiquetas (Fase 8) que coinciden con 2 de las 3 empresas
        mockMvc.perform(put("/api/usuarios/" + alumno.getId() + "/etiquetas")
                        .with(csrf()).with(comoAlumno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ActualizarEtiquetasRequest(List.of(java.getId(), redes.getId())))))
                .andExpect(status().isOk());

        // 4. El listado se reordena por score: Bit 1.0, Antenas 0.5, Cafetería 0.0
        String vistaAlumno = mockMvc.perform(get("/api/empresas/afinidad").with(comoAlumno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alumnoConEtiquetas").value(true))
                .andExpect(jsonPath("$.empresas[0].empresa.nombre").value("Bit"))
                .andExpect(jsonPath("$.empresas[0].score").value(1.0))
                .andExpect(jsonPath("$.empresas[0].etiquetasCoincidentes.length()").value(2))
                .andExpect(jsonPath("$.empresas[1].empresa.nombre").value("Antenas"))
                .andExpect(jsonPath("$.empresas[1].score").value(0.5))
                .andExpect(jsonPath("$.empresas[2].empresa.nombre").value("Cafetería"))
                .andExpect(jsonPath("$.empresas[2].score").value(0.0))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // 5. El tutor de la asignación activa ve el resultado idéntico
        String vistaTutor = mockMvc.perform(get("/api/alumnos/" + alumno.getId() + "/afinidad").with(comoTutor))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(vistaTutor).isEqualTo(vistaAlumno);

        // 6. Otro profesor sin esa tutoría no puede
        mockMvc.perform(get("/api/alumnos/" + alumno.getId() + "/afinidad").with(comoOtroProfesor))
                .andExpect(status().isForbidden());
    }

    private Empresa empresaPublicada(String nombre, Etiqueta sector, Etiqueta... etiquetas) {
        Usuario creador = usuarioRepository.findByCorreo("tutor@iesejemplo.es").orElseThrow();
        Empresa empresa = new Empresa(nombre, null, null, sector, null, null, null, null, creador);
        empresa.setEtiquetas(new ArrayList<>(List.of(etiquetas)));
        empresa.setPublicada(true);
        return empresaRepository.save(empresa);
    }
}
