package practikalia.usuario.correo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CorreoPermitidoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CorreoPermitidoRepository correoPermitidoRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RequestPostProcessor comoAdmin = user("admin@iesejemplo.es")
            .authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"), new SimpleGrantedAuthority("ADMIN"));
    private final RequestPostProcessor comoProfesor =
            user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"));

    @Test
    void unProfesorNoAdminNoPuedeListarla() throws Exception {
        mockMvc.perform(get("/api/correos-permitidos").with(comoProfesor))
                .andExpect(status().isForbidden());
    }

    @Test
    void unAdminDaDeAltaUnCorreoEnMayusculasYQuedaEnMinusculas() throws Exception {
        mockMvc.perform(post("/api/correos-permitidos")
                        .with(csrf())
                        .with(comoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CrearCorreoPermitidoRequest("Alumno@IesEjemplo.es"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value("alumno@iesejemplo.es"));

        assertThat(correoPermitidoRepository.existsByCorreo("alumno@iesejemplo.es")).isTrue();
    }

    @Test
    void darDeAltaUnCorreoYaExistenteDevuelve409() throws Exception {
        correoPermitidoRepository.save(new CorreoPermitido("ya@iesejemplo.es"));

        mockMvc.perform(post("/api/correos-permitidos")
                        .with(csrf())
                        .with(comoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CrearCorreoPermitidoRequest("ya@iesejemplo.es"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CORREO_PERMITIDO_YA_EXISTE"));
    }

    @Test
    void unAdminBorraUnCorreoDeLaWhitelist() throws Exception {
        CorreoPermitido guardado = correoPermitidoRepository.save(new CorreoPermitido("borrame@iesejemplo.es"));

        mockMvc.perform(delete("/api/correos-permitidos/" + guardado.getId()).with(csrf()).with(comoAdmin))
                .andExpect(status().isNoContent());

        assertThat(correoPermitidoRepository.existsById(guardado.getId())).isFalse();
    }

    @Test
    void borrarUnIdInexistenteDevuelve404() throws Exception {
        mockMvc.perform(delete("/api/correos-permitidos/999999").with(csrf()).with(comoAdmin))
                .andExpect(status().isNotFound());
    }
}
