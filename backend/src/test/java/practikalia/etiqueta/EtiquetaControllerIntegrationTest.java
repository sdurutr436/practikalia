package practikalia.etiqueta;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EtiquetaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EtiquetaRepository etiquetaRepository;

    private final RequestPostProcessor comoProfesor =
            user("profesor@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"));
    private final RequestPostProcessor comoAlumno =
            user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"));

    @Test
    void profesorListaElCatalogoCompleto() throws Exception {
        etiquetaRepository.save(new Etiqueta("Java"));
        etiquetaRepository.save(new Etiqueta("Python"));

        mockMvc.perform(get("/api/etiquetas").with(comoProfesor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Java"))
                .andExpect(jsonPath("$[1].nombre").value("Python"));
    }

    @Test
    void alumnoNoAccedeAlCatalogo() throws Exception {
        mockMvc.perform(get("/api/etiquetas").with(comoAlumno))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinAutenticarNoAccedeAlCatalogo() throws Exception {
        mockMvc.perform(get("/api/etiquetas"))
                .andExpect(status().isUnauthorized());
    }
}
