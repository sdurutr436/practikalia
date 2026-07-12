package practikalia.usuario;

import practikalia.etiqueta.Etiqueta;
import practikalia.etiqueta.EtiquetaRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class EtiquetasFlujoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EtiquetaRepository etiquetaRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void flujoCompletoDeEtiquetasConEdicionPropiaYDeProfesor() throws Exception {
        Usuario alumno = usuarioRepository.save(
                new Usuario("flujo@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.ALUMNO));
        usuarioRepository.save(
                new Usuario("proflujo@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.PROFESOR));
        Etiqueta java = etiquetaRepository.save(new Etiqueta("Java"));
        Etiqueta redes = etiquetaRepository.save(new Etiqueta("Redes"));
        Etiqueta diseno = etiquetaRepository.save(new Etiqueta("Diseño"));
        var comoAlumno = user("flujo@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"));
        var comoProfesor = user("proflujo@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"));

        // 1. Sin etiquetas: /me las devuelve vacías
        mockMvc.perform(get("/api/auth/me").with(comoAlumno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etiquetas.length()").value(0));

        // 2. El alumno marca 2 etiquetas sobre su propio id
        mockMvc.perform(put("/api/usuarios/" + alumno.getId() + "/etiquetas")
                        .with(csrf()).with(comoAlumno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ActualizarEtiquetasRequest(List.of(java.getId(), redes.getId())))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // 3. Ambas rutas de lectura las reflejan
        mockMvc.perform(get("/api/auth/me").with(comoAlumno))
                .andExpect(jsonPath("$.etiquetas.length()").value(2));
        mockMvc.perform(get("/api/usuarios/" + alumno.getId() + "/etiquetas").with(comoAlumno))
                .andExpect(jsonPath("$.length()").value(2));

        // 4. El profesor reemplaza (no une) con 1 etiqueta distinta
        mockMvc.perform(put("/api/usuarios/" + alumno.getId() + "/etiquetas")
                        .with(csrf()).with(comoProfesor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ActualizarEtiquetasRequest(List.of(diseno.getId())))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Diseño"));

        // 5. Solo queda la nueva, desde cualquiera de las dos rutas
        mockMvc.perform(get("/api/auth/me").with(comoAlumno))
                .andExpect(jsonPath("$.etiquetas.length()").value(1))
                .andExpect(jsonPath("$.etiquetas[0].nombre").value("Diseño"));
        mockMvc.perform(get("/api/usuarios/" + alumno.getId() + "/etiquetas").with(comoProfesor))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Diseño"));
    }
}
